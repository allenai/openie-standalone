package edu.knowitall.repr.document

import edu.knowitall.tool.segment._
import edu.knowitall.repr.sentence._

case class DocumentSentence[S <: Sentence](sentence: S, offset: Int)

trait Sentenced[S <: Sentence] {
  this: Document =>

  def sentences: Stream[DocumentSentence[S]]
}

trait Sentencer[S <: Sentence] extends Sentenced[S] {
  this: Document =>

  def constructor(text: String): S
  def sentencer: Segmenter

  override lazy val sentences: Stream[DocumentSentence[S]] =
    sentencer(text).toStream.map { segment =>
      DocumentSentence(constructor(segment.text), segment.offset)
    }
}
