package edu.knowitall.tool
package srl

import edu.knowitall.tool.LineProcessor
import edu.knowitall.tool.parse.DependencyParser
import edu.knowitall.tool.parse.graph.DependencyGraph
import scala.concurrent.Await
import scala.concurrent.duration._

abstract class Srl {
  def apply(graph: DependencyGraph): Seq[Frame]
}
