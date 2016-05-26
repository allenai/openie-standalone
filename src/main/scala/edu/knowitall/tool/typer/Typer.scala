package edu.knowitall.tool.typer

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.tokenize.Token

abstract class Typer[E <: Token] {
  def name: String
  def source: String

  def apply(seq: Seq[E]): Seq[Type]
}

abstract class Type {
  def name: String
  def source: String
  def tokenInterval: Interval
  def text: String

  def matchText[E <: Token](seq: Seq[E]): String = seq.iterator.slice(tokenInterval.start, tokenInterval.end).map(_.string).mkString(" ")
  def tokens[E <: Token](seq: Seq[E]): Seq[E] = seq.slice(tokenInterval.start, tokenInterval.end)
}

object Type {
  def apply(name: String, source: String, tokenInterval: Interval, text: String): Type = {
    this.create(name, source, tokenInterval, text)
  }

  def create(name: String, source: String, tokenInterval: Interval, text: String): Type = {
    TypeImpl(name, source, tokenInterval, text)
  }

  private case class TypeImpl(
    val name: String,
    val source: String,
    val tokenInterval: Interval,
    val text: String
  ) extends Type
}
