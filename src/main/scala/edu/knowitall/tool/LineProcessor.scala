package edu.knowitall.tool

import edu.knowitall.common.Timing

import java.io.File
import java.util.Scanner
import java.io.PrintWriter

import scala.io.Source
import scala.io.Codec

abstract class LineProcessor(name: String) {
  import scopt.OptionParser._

  case class Config(val server: Boolean = false, val port: Int = 8080, val outputFile: Option[File] = None, val inputFile: Option[File] = None, parallel: Boolean = false)

  val parser = new scopt.OptionParser[Config](name) {
    opt[Unit]("server") action { (_, c: Config) => c.copy(server = true) } text ("run as a server")
    opt[Int]("port") action { (port: Int, c: Config) =>
      require(c.server, "--server must be set with --port"); c.copy(port = port)
    } text ("which port to run the server on")
    opt[String]("input") action { (path: String, c: Config) => c.copy(inputFile = Some(new File(path))) } text ("file to input from")
    opt[String]("output") action { (path: String, c: Config) => c.copy(outputFile = Some(new File(path))) } text ("file to output to")
    opt[Unit]("parallel") action { (_, c: Config) => c.copy(parallel = true) } text ("parallel execution")
  }

  def main(args: Array[String]) = {
    parser.parse(args, new Config) match {
      case Some(config) => run(config)
      case None =>
    }
  }

  def init(config: Config): Unit = {}

  def run(config: Config) {
    init(config)
    if (config.server) {
      (new LineProcessorServer(this.getClass.getSimpleName(), config.port, process)).run()
    }
    else {
      runCli(config)
    }
  }

  def handle(writer: PrintWriter, line: String): Unit = {
    writer.println(process(line))
    writer.flush()
  }

  def process(line: String): String

  def runCli(config: Config) {
    val source = config.inputFile match {
      case Some(file) => Source.fromFile(file)(Codec.UTF8)
      case None => Source.fromInputStream(System.in)(Codec.UTF8)
    }

    val writer = config.outputFile match {
      case Some(file) => new PrintWriter(file, "UTF-8")
      case None => new PrintWriter(System.out)
    }

    val ns = Timing.time {
      val lines = {
        if (config.parallel) {
          source.getLines.toIndexedSeq.par
        }
        else {
          source.getLines
        }
      }
      for (line <- lines) {
        handle(writer, line)
      }
    }

    System.err.println(Timing.Seconds.format(ns))

    source.close()
    writer.close()
  }
}

// This is a separate class so that optional dependencies are not loaded
// unless a server instance is being create.
class LineProcessorServer(name: String, port: Int, process: String => String) {
  import unfiltered.request._
  import unfiltered.response._
  import unfiltered.filter.Planify

  def run() {
    val plan = Planify {
      case Path(Seg(p :: Nil)) => ResponseString(p)
      case req @ POST(_) => process.synchronized {
        ResponseString(process(Body.string(req)))
      }
      case req @ GET(_) => ResponseString("Post a line to process for: " + name)
    }

    unfiltered.jetty.Http(port).filter(plan).run()
    System.out.println("Server started on port: " + port);
  }
}
