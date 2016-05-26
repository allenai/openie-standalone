package edu.knowitall.tool
package parse

import edu.knowitall.tool.parse.graph._

import edu.knowitall.tool.tokenize._
import edu.knowitall.tool.postag._
import edu.knowitall.tool.chunk._

class RemoteDependencyParser(val urlString: String) extends DependencyParser with Remote {
  override def postagger = throw new UnsupportedOperationException()

  override def dependencyGraph(sentence: String) = {
    val response = post(sentence)
    DependencyGraph.multilineStringFormat.read(response)
  }

  /** Throws UnsupportedOperationException
    */
  override def dependencyGraphPostagged(tokens: Seq[PostaggedToken]): DependencyGraph = {
    throw new UnsupportedOperationException()
  }

  /** Throws UnsupportedOperationException
    */
  override def dependencyGraphTokenized(tokens: Seq[Token]): DependencyGraph = {
    throw new UnsupportedOperationException()
  }
}
