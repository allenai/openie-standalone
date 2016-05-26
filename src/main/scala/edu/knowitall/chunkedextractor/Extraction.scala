package edu.knowitall
package chunkedextractor

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.tokenize.Token

case class ExtractionPart[+T <% Token](text: String, tokens: Seq[T], tokenInterval: Interval) {
  override def toString = text

  def offsetInterval = Interval.open(tokens.head.offsets.start, tokens.last.offsets.end)

  @deprecated("1.0.3", "Use tokenInterval instead.")
  def interval = tokenInterval
}

object ExtractionPart {
  def fromSentenceTokens[T <% Token](sentenceTokens: Seq[T], tokenInterval: Interval, text: String) =
    new ExtractionPart[T](text, sentenceTokens.view(tokenInterval.start, tokenInterval.end), tokenInterval)

  def fromSentenceTokens[T <% Token](sentenceTokens: Seq[T], tokenInterval: Interval) =
    new ExtractionPart(sentenceTokens.view(tokenInterval.start, tokenInterval.end).iterator.map(_.string).mkString(" "), sentenceTokens.view(tokenInterval.start, tokenInterval.end), tokenInterval)
}

case class BinaryExtraction[+T <% Token](arg1: ExtractionPart[T], rel: ExtractionPart[T], arg2: ExtractionPart[T]) {
  override def toString = Iterable(arg1, rel, arg2).mkString("(", "; ", ")")

  def text = Iterable(arg1.text, rel.text, arg2.text).mkString(" ")
  def tokenInterval = Interval.span(Iterable(arg1.tokenInterval, rel.tokenInterval, arg2.tokenInterval))
  def offsetInterval = Interval.span(Iterable(arg1.offsetInterval, rel.offsetInterval, arg2.offsetInterval))
  def tokens = arg1.tokens ++ rel.tokens ++ arg2.tokens

  @deprecated("1.0.3", "Use tokenInterval instead.")
  def interval = tokenInterval
}

class BinaryExtractionInstance[+T <% Token](val extr: BinaryExtraction[T], val sent: Seq[T]) {
  override def toString = extr.toString + " <- \"" + sent.map(_.string).mkString(" ") + "\""
}
