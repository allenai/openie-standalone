package edu.knowitall.repr.sentence

import edu.knowitall.tool.tokenize.Token
import edu.knowitall.tool.postag.PostaggedToken
import edu.knowitall.tool.parse._
import edu.knowitall.tool.parse.graph._

trait Parsed {
  this: Sentence =>

  def dgraph: DependencyGraph
}

trait Parser extends Parsed {
  this: Sentence with Postagged =>

  def parser: DependencyParser

  override lazy val dgraph =
    parser.dependencyGraphPostagged(this.tokens)
}
