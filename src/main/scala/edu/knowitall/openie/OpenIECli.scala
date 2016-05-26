package edu.knowitall.openie

import java.io.File
import java.io.PrintStream
import java.nio.charset.MalformedInputException
import java.io.PrintWriter
import java.net.URL

import scala.io.Source

import edu.knowitall.common.{ Resource, Timing }
import edu.knowitall.tool.parse.DependencyParser
import edu.knowitall.tool.parse.ClearParser
import edu.knowitall.tool.parse.RemoteDependencyParser
import edu.knowitall.tool.srl.Srl
import edu.knowitall.tool.srl.RemoteSrl
import edu.knowitall.tool.srl.ClearSrl
import edu.knowitall.tool.sentence.OpenNlpSentencer
import edu.knowitall.openie.util.SentenceIterator

/** A command line application for exploring Open IE.
  *
  * Input is a sentence (or text to be broken into sentences
  * if --split is specified) and output is one or more extractions.
  */
object OpenIECli extends App {
  object OutputFormat {
    def parse(format: String): OutputFormat = format match {
      case "simple" => SimpleFormat
      case "column" => ColumnFormat
      case _ => throw new MatchError("Unknown format: " + format)
    }
  }

  /** An abstract class definite how extractions are outputted.
    */
  sealed abstract class OutputFormat {
    def print(writer: PrintWriter, sentence: String, insts: Seq[Instance])
  }

  /** Sentences are printed followed by extractions, one per line.
    */
  case object SimpleFormat extends OutputFormat {
    def print(writer: PrintWriter, sentence: String, insts: Seq[Instance]) {
      writer.println(sentence)
      insts foreach writer.println
      writer.println()
    }
  }

  /** All relevant data is printed in columns seperated by tab.
    */
  case object ColumnFormat extends OutputFormat {
    def print(writer: PrintWriter, sentence: String, insts: Seq[Instance]) {
      insts.foreach { inst =>
        writer.println(
          Iterator(
            inst.confidence,
            inst.extr.context.getOrElse(""),
            inst.extr.arg1,
            inst.extr.rel,
            inst.extr.arg2s.mkString("; "),
            sentence
          ).mkString("\t")
        )
      }
    }
  }

  /** A class that represents the command line configuration
    * of the application.
    *
    * @param  inputFile  The file to use as input
    * @param  outputFile  The file to use as output
    * @param  srlServer  A URL to an SRL server
    * @param  parserServer  A URL to a parser server
    * @param  encoding  The input and output character encoding
    * @param  formatter  The OutputFormat subclass to be used for output
    * @param  split  If true, input text is split into sentences
    */
  case class Config(
      inputFile: Option[File] = None,
      outputFile: Option[File] = None,
      parserServer: Option[URL] = None,
      srlServer: Option[URL] = None,
      encoding: String = "UTF-8",
      formatter: OutputFormat = SimpleFormat,
      ignoreErrors: Boolean = false,
      showUsage: Boolean = false,
      binary: Boolean = false,
      split: Boolean = false,
      includeUnknownArg2: Boolean = false
  ) {

    /** Create the input source from a file or stdin.
      */
    def source() = {
      inputFile match {
        case Some(file) => Source.fromFile(file, encoding)
        case None => Source.fromInputStream(System.in, encoding)
      }
    }

    /** Create a writer to a file or stdout.
      */
    def writer() = {
      outputFile match {
        case Some(file) => new PrintWriter(file, encoding)
        case None => new PrintWriter(new PrintStream(System.out, true, encoding))
      }
    }

    def createParser(): DependencyParser = parserServer match {
      case Some(url) => new RemoteDependencyParser(url.toString)
      case None => new ClearParser()
    }

    def createSrl(): Srl = srlServer match {
      case Some(url) => new RemoteSrl(url.toString)
      case None => new ClearSrl()
    }
  }

  // definition for command-line argument parser
  val argumentParser = new scopt.OptionParser[Config]("openie") {
    opt[String]("input-file") action { (string, config) =>
      val file = new File(string)
      require(file.exists, "input file does not exist: " + file)
      config.copy(inputFile = Some(file))
    } text ("input file")
    opt[String]("ouput-file") action { (string, config) =>
      val file = new File(string)
      config.copy(outputFile = Some(file))
    } text ("output file")
    opt[String]("parser-server") action { (string, config) =>
      config.copy(parserServer = Some(new URL(string)))
    } text ("Parser server")
    opt[String]("srl-server") action { (string, config) =>
      config.copy(srlServer = Some(new URL(string)))
    } text ("SRL server")
    opt[String]("encoding") action { (string, config) =>
      config.copy(encoding = string)
    } text ("Character encoding")
    opt[String]("format") action { (string, config) =>
      config.copy(formatter = OutputFormat.parse(string))
    } text ("Output format")
    opt[Unit]('u', "usage") action { (_, config) =>
      config.copy(showUsage = true)
    } text ("show cli usage")
    opt[Unit]("ignore-errors") action { (_, config) =>
      config.copy(ignoreErrors = true)
    } text ("ignore errors")
    opt[Unit]("include-unknown-arg2") action { (_, config) =>
      config.copy(includeUnknownArg2 = true)
    } text ("includes arg2 [UNKNOWN] extractions from relnoun")
    opt[Unit]('b', "binary") action { (_, config) =>
      config.copy(binary = true)
    } text ("binary output")
    opt[Unit]('s', "split") action { (_, config) =>
      config.copy(split = true)
    } text ("Split paragraphs into sentences")
  }

  argumentParser.parse(args, Config()) match {
    case Some(config) if config.showUsage => println(argumentParser.usage)
    case Some(config) =>
      try {
        run(config)
      } catch {
        case e: MalformedInputException =>
          System.err.println(
            "\nError: a MalformedInputException was thrown.\n" +
              "This usually means there is a mismatch between what is expected and the input file.\n" +
              "Try changing the input file's character encoding to UTF-8 or specifying the correct character encoding for the input file with '--encoding'.\n"
          )
          e.printStackTrace()
      }
    case None => // usage will be shown
  }

  /** Main method with structured arguments.
    */
  def run(config: Config) {
    // the extractor system
    val openie = new OpenIE(parser = config.createParser(), srl = config.createSrl(), config.binary, config.includeUnknownArg2)

    println("* * * * * * * * * * * * *")
    println("* OpenIE 4.1.x is ready *")
    println("* * * * * * * * * * * * *")

    // a sentencer used if --split is specified
    lazy val sentencer = new OpenNlpSentencer

    config.inputFile.foreach { file =>
      System.err.println("Processing file: " + file)
    }

    Timing.timeThen {
      // iterate over input
      Resource.using(config.source()) { source =>
        Resource.using(config.writer()) { writer =>
          val sentences =
            if (config.split) new SentenceIterator(sentencer, source.getLines.buffered)
            else source.getLines

          // iterate over sentences
          for {
            sentence <- sentences
            if !sentence.trim.isEmpty
          } {
            try {
              // run the extractor
              val insts = openie.extract(sentence)
              config.formatter.print(writer, sentence, insts)
              writer.flush()
            } catch {
              case e if config.ignoreErrors =>
                System.err.println("Error on sentence: " + sentence)
                e.printStackTrace()
              case e: Exception =>
                System.err.println("Error on sentence: " + sentence)
                throw e
            }
          }
        }
      }
    } { ns =>
      System.err.println(s"Processed in ${Timing.Seconds.format(ns)}.")
    }

    config.outputFile.foreach { file =>
      System.err.println("Output written to file: " + file)
    }
  }
}
