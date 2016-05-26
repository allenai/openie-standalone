package edu.knowitall.tool
package parse

import edu.knowitall.tool.parse.graph._

abstract class DependencyParserMain extends LineProcessor("parser") {
  def dependencyParser: DependencyParser

  override def init(config: Config) {
    // for timing purposes
    dependencyParser.dependencyGraph("I want to initialize the parser.")
  }

  override def process(line: String) = {
    val parse = dependencyParser.dependencyGraph(line)
    DependencyGraph.multilineStringFormat.write(parse)
  }
}
