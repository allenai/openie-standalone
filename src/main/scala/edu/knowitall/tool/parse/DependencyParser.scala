package edu.knowitall
package tool
package parse

import graph._

import postag.PostaggedToken
import postag.Postagger
import tokenize.Token

import scala.concurrent.ExecutionContext.Implicits.global

/** A trait for a tool that produces dependencies, such as the
  * Stanford dependency parser.
  */
trait DependencyParser {

  def postagger: Postagger

  /** Create a graph of the dependencies from POS-tagged tokens.
    */
  def dependencyGraphPostagged(tokens: Seq[PostaggedToken]): DependencyGraph

  def apply(string: String) = dependencyGraph(string)

  /** Create a graph of the dependencies.  This has more information than
    * creating a DependencyGraph from an `Iterable[Dependency]` because it
    * will have the source text.
    */
  def dependencyGraph(string: String): DependencyGraph = {
    val postaggedTokens = postagger.postag(string)
    dependencyGraphPostagged(postaggedTokens)
  }

  /** Create a graph of the dependencies from Tokens.
    */
  def dependencyGraphTokenized(tokens: Seq[Token]) = {
    val postaggedTokens = postagger.postagTokenized(tokens)
    dependencyGraphPostagged(postaggedTokens)
  }

  @deprecated("Use dependencyGraph(string).dependencies", "2.4.3")
  def dependencies(string: String): Iterable[Dependency] = {
    this.dependencyGraph(string).dependencies
  }

}
