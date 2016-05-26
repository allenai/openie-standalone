package edu.knowitall.tool
package srl

import edu.knowitall.tool.parse.graph._

abstract class SrlMain extends LineProcessor("srl") {
  def srl: Srl

  override def process(line: String) = {
    val dgraph = DependencyGraph.stringFormat.read(line)
    (srl(dgraph) map (_.serialize)).mkString("\n")
  }
}