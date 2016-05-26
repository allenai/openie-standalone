package edu.knowitall
package tool
package parse
package graph

import scala.io.Source
import scopt.OptionParser
import tool.stem.IdentityStemmer

/** An executable that applies a pattern to a sentence. */
object ApplyPattern {
  def main(args: Array[String]) {
    case class CliConfig(patternFilePath: String = null, sentenceFilePath: String = null)
    val parser = new OptionParser[CliConfig]("applypat") {
      opt[String]('p', "patterns") action { (v: String, config) => config.copy(patternFilePath = v) }
      opt[String]('s', "sentences") action { (v: String, config) => config.copy(sentenceFilePath = v) }
    }

    val patternFormat = new DependencyPattern.StringFormat()(IdentityStemmer.instance)

    parser.parse(args, CliConfig()).foreach { parser =>
      val patternSource = Source.fromFile(parser.patternFilePath)
      val patterns = patternSource.getLines.map(patternFormat.read(_)).toList
      patternSource.close

      val sentenceSource = Source.fromFile(parser.sentenceFilePath)
      try {
        for (p <- patterns) {
          println("pattern: " + p)
          for (line <- sentenceSource.getLines) {
            val Array(text, deps) = line.split("\t")
            val graph = DependencyGraph.stringFormat.read(deps)
            for (m <- p(graph.graph)) {
              println(m)
            }
          }

          println()
          println()
        }
      } finally {
        sentenceSource.close
      }
    }
  }
}
