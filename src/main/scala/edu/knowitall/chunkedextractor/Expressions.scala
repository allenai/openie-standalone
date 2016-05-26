package edu.knowitall
package chunkedextractor

import java.util.regex.Pattern
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.tool.chunk.ChunkedToken

object Expressions {
  type Token = Lemmatized[ChunkedToken]

  /** A regular expression that is evaluated against the string portion of a
    * token.
    *
    * This comparison is case-sensitive.
    *
    * @author schmmd
    */
  class CaseSensitiveStringExpression(val pattern: Pattern) extends Function[Token, Boolean] {
    def this(string: String, flags: Int) {
      this(Pattern.compile(string, flags))
    }

    def this(string: String) {
      this(string, 0)
    }

    override def apply(token: Token): Boolean =
      return pattern.matcher(token.token.string).matches()
  }

  /** A regular expression that is evaluated against the string portion of a
    * token.
    *
    * This comparison is case-insensitive.
    *
    * @author schmmd
    */
  class StringExpression(val pattern: Pattern) extends Function[Token, Boolean] {
    def this(string: String, flags: Int) {
      this(Pattern.compile(string, flags))
    }

    def this(string: String) {
      this(string, Pattern.CASE_INSENSITIVE)
    }

    override def apply(token: Token): Boolean =
      pattern.matcher(token.token.string).matches()
  }

  /** A regular expression that is evaluated against the lemma portion of a
    * token.
    * @author schmmd
    */
  class LemmaExpression(val pattern: Pattern) extends Function[Token, Boolean] {
    def this(string: String, flags: Int) {
      this(Pattern.compile(string, flags))
    }

    def this(string: String) {
      this(string, Pattern.CASE_INSENSITIVE)
    }

    override def apply(token: Token): Boolean =
      pattern.matcher(token.lemma).matches()
  }

  /** A regular expression that is evaluated against the POS tag portion of a
    * token.
    * @author schmmd
    */
  class PostagExpression(val pattern: Pattern) extends Function[Token, Boolean] {
    def this(string: String, flags: Int) {
      this(Pattern.compile(string, flags))
    }

    def this(string: String) {
      this(string, Pattern.CASE_INSENSITIVE)
    }

    override def apply(token: Token): Boolean =
      pattern.matcher(token.token.postag).matches()
  }

  /** A regular expression that is evaluated against the chunk tag portion of a
    * token.
    * @author schmmd
    */
  class ChunkExpression(val pattern: Pattern) extends Function[Token, Boolean] {
    def this(string: String, flags: Int) {
      this(Pattern.compile(string, flags))
    }

    def this(string: String) {
      this(string, Pattern.CASE_INSENSITIVE)
    }

    override def apply(token: Token): Boolean =
      pattern.matcher(token.token.chunk).matches()
  }
}
