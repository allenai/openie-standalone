package edu.knowitall
package tool.chunk

import edu.knowitall.common.HashCodeHelper
import edu.knowitall.tool.postag.PostaggedToken
import edu.knowitall.tool.Format

/** A representation of a chunked token.  A chunked token has all the
  * aspects of a postagged token along with a chunk tag.
  *
  * @constructor
  * @param  string  the string of the token
  * @param  offset  the character offset of the token in the source sentence
  * @param  postag  the PENN-style part-of-speech tag of the token
  * @param  chunk   the chunk tag of the token in BIO format
  */
class ChunkedToken(val chunkSymbol: Symbol, override val postagSymbol: Symbol, override val string: String, override val offset: Int)
    extends PostaggedToken(postagSymbol, string, offset) {
  def chunk = chunkSymbol.name
  require(chunk.forall(!_.isWhitespace), "chunk contains whitespace: " + chunk)

  override def toString = string + "/" + postag + "/" + chunk + "@" + offset

  override def hashCode = super.hashCode * 31 + HashCodeHelper(this.postag, this.chunk)
  def canEqual(that: ChunkedToken) = that.isInstanceOf[ChunkedToken]
  override def equals(that: Any) = that match {
    case that: ChunkedToken => (that canEqual this) &&
      super.equals(that) &&
      this.chunk == that.chunk
    case _ => false
  }
}

object ChunkedToken {
  def apply(chunk: String, postag: String, string: String, offset: Int): ChunkedToken =
    new ChunkedToken(Symbol(chunk), Symbol(postag), string, offset)

  def apply(token: PostaggedToken, chunk: String): ChunkedToken =
    new ChunkedToken(Symbol(chunk), token.postagSymbol, token.string, token.offset)

  def unapply(token: ChunkedToken): Option[(String, String, String, Int)] = Some((token.chunk, token.postag, token.string, token.offset))

  object stringFormat extends Format[ChunkedToken, String] {
    def write(chunkedToken: ChunkedToken): String = {
      Iterator(PostaggedToken.stringFormat.write(chunkedToken), chunkedToken.chunk).mkString(" ")
    }
    def read(str: String): ChunkedToken = {
      val chunkedTokenRegex = """(.*?) +([^ ]*)""".r
      try {
        val chunkedTokenRegex(pickledPostaggedToken, chunk) = str
        val postaggedToken = PostaggedToken.stringFormat.read(pickledPostaggedToken)
        ChunkedToken(postaggedToken, chunk)
      } catch {
        case e: Exception =>
          throw new MatchError("Error deserializing ChunkedToken: " + str)
      }
    }
  }
}
