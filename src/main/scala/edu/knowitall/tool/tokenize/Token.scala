package edu.knowitall
package tool
package tokenize

import edu.knowitall.common.HashCodeHelper
import edu.knowitall.collection.immutable.Interval

/** The most simple representation of a token.  A token has a string
  * and a character offset in the original text.
  *
  * @param  string  the string of the token
  * @param  offset  the character offset of the token in the source sentence
  */
class Token(val string: String, val offset: Int) {
  override def toString = string + "@" + offset

  override def hashCode = HashCodeHelper(this.string, this.offset)
  def canEqual(that: Token) = that.isInstanceOf[Token]
  override def equals(that: Any) = that match {
    case that: Token => (that canEqual this) &&
      this.string == that.string &&
      this.offset == that.offset
    case _ => false
  }

  @deprecated("Use offsets instead.", "2.4.0")
  def interval = offsets

  def offsets = Interval.open(offset, offset + string.length)
}

object Token {
  def apply(string: String, offset: Int) = new Token(string, offset)
  def unapply(token: Token): Option[(String, Int)] = Some((token.string, token.offset))

  object stringFormat extends Format[Token, String] {
    val tokenRegex = "(.*?) +([^ ]*)".r
    def write(token: Token): String = token.string + " " + token.offset
    def read(pickled: String): Token = {
      val (string, offset) = pickled match {
        case tokenRegex(string, offset) => (string, offset.toInt)
        case _ => throw new MatchError("Error parsing token: " + pickled)
      }
      Token(string, offset)
    }
  }
}
