package edu.knowitall
package tool
package chunk

import scala.util.matching.Regex

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.postag._
import edu.knowitall.tool.tokenize._

/** A Chunker takes postagged text and adds a chunk tag, specifying
  * whether a noun or verb phrase is starting or continuing.
  */
abstract class Chunker {
  def postagger: postag.Postagger

  def apply(sentence: String) = chunk(sentence)

  /** chunk postagged text */
  def chunkPostagged(tokens: Seq[postag.PostaggedToken]): Seq[ChunkedToken]

  /** chunk tokenized text */
  def chunkTokenized(tokens: Seq[tokenize.Token]): Seq[ChunkedToken] = {
    val postags = postagger.postagTokenized(tokens)
    chunkPostagged(postags)
  }

  /** chunk raw text */
  def chunk(sentence: String): Seq[ChunkedToken] = {
    val postags = postagger.postag(sentence)
    chunkPostagged(postags)
  }
}

object Chunker {
  def join(stringRegex: Regex, postag: String)(chunks: Seq[ChunkedToken]): Seq[ChunkedToken] = {
    var mutableChunks = chunks

    for (index <- Range(0, chunks.size)) {
      val chunk = chunks(index)
      if (stringRegex.pattern.matcher(chunk.string).matches() && chunk.postag == postag &&
        (index > 0 && (chunks(index - 1).chunk endsWith "NP")) &&
        (index < chunks.length && (chunks(index + 1).chunk endsWith "-NP"))) {
        val nextChunk = chunks(index + 1)
        mutableChunks = mutableChunks.updated(index, ChunkedToken("I-NP", chunk.postag, chunk.string, chunk.offset))
        mutableChunks = mutableChunks.updated(index + 1, ChunkedToken("I-NP", nextChunk.postag, nextChunk.string, nextChunk.offset))
      }
    }

    mutableChunks
  }

  def joinOf(chunks: Seq[ChunkedToken]) = join("of".r, "IN")(chunks)
  def joinPos(chunks: Seq[ChunkedToken]) = join("'|'s".r, "POS")(chunks)

  /** Return the intervals represented by these ChunkedTokens.
    * The first part of a pair is the chunk type, the second part is the interval.
    */
  def intervals(chunks: Seq[ChunkedToken]): Seq[(String, Interval)] = {
    def helper(chunks: Iterator[String]) = {
      var intervals = Vector.empty[(String, Interval)]
      var iterator: Iterator[(String, Int)] = chunks.zipWithIndex
      while (iterator.hasNext) {
        val interval = {
          val (nextToken, nextIndex) = iterator.next()

          val (chunkTokens, rest) = iterator.span(token => token._1 startsWith "I")
          iterator = rest

          val chunkType = {
            val hyphen = nextToken.indexOf('-')
            if (hyphen == -1) {
              nextToken
            }
            else {
              nextToken.drop(hyphen + 1)
            }
          }
          (chunkType, Interval.open(nextIndex, chunkTokens.toSeq.lastOption.map(_._2 + 1).getOrElse(nextIndex + 1)))
        }

        intervals = intervals :+ interval
      }

      intervals
    }

    helper(chunks.iterator.map(_.chunk))
  }

  def tokensFrom(chunks: Seq[String], postags: Seq[String], tokens: Seq[Token]) = {
    val postaggedTokens = Postagger.tokensFrom(postags, tokens)
    (chunks zip postaggedTokens).map { case (chunk, postaggedToken) => ChunkedToken(postaggedToken, chunk) }
  }

  class stringFormat(val delim: String) extends Format[Seq[ChunkedToken], String] {
    def write(chunkedTokens: Seq[ChunkedToken]): String = {
      val serializedChunkedTokens = for (chunkedTok <- chunkedTokens) yield {
        ChunkedToken.stringFormat.write(chunkedTok)
      }
      serializedChunkedTokens.mkString(delim)
    }
    def read(str: String): Seq[ChunkedToken] = {
      for (s <- str.split(delim)) yield ChunkedToken.stringFormat.read(s)
    }
  }
  object stringFormat extends stringFormat("\t")
  object multilineStringFormat extends stringFormat("\n")
}