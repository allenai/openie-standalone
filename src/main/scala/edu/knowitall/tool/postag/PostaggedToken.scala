package edu.knowitall.tool
package postag

import edu.knowitall.common.HashCodeHelper
import edu.knowitall.tool.tokenize.Token

/** A representation for a part-of-speech tagged token.  POS tokens
  * use PENN-treebank style tags.
  *
  * @param  string  the string of the token
  * @param  offset  the character offset of the token in the source sentence
  * @param  postag  the PENN-style part-of-speech tag of the token
  * @param  chunk   the chunk tag of the token in BIO format
  */
class PostaggedToken(val postagSymbol: Symbol, override val string: String, override val offset: Int)
    extends Token(string, offset) {
  def postag = postagSymbol.name
  require(postag.forall(!_.isWhitespace), "postag contains whitespace: " + postag)

  override def toString = string + "/" + postag + "@" + offset

  override def hashCode = super.hashCode * 31 + HashCodeHelper(this.postag)
  def canEqual(that: PostaggedToken) = that.isInstanceOf[PostaggedToken]
  override def equals(that: Any) = that match {
    case that: PostaggedToken => (that canEqual this) &&
      super.equals(that) &&
      this.postag == that.postag
    case _ => false
  }

  def isProperNoun = postagSymbol == Symbol("NNP") || postagSymbol == Symbol("NNPS")
  def isCommonNoun = postagSymbol == Symbol("NN") || postagSymbol == Symbol("NNS")
  def isNoun = isProperNoun || isCommonNoun
  def isPluralNoun = postagSymbol == Symbol("NNS") || postagSymbol == Symbol("NNPS")

  def isVerbBase = postagSymbol == Symbol("VB")
  def isVerbPast = postagSymbol == Symbol("VBD")
  def isVerbGerund = postagSymbol == Symbol("VBG")
  def isVerbPastParticiple = postagSymbol == Symbol("VBN")
  def isVerbNon3pPresent = postagSymbol == Symbol("VBP")
  def isVerb3pPresent = postagSymbol == Symbol("VBZ")
  def isVerbPresent = isVerbNon3pPresent || isVerb3pPresent
  def isVerb = postag.startsWith("VB")

  def isPlainAdjective = postagSymbol == Symbol("JJ")
  def isComparativeAdjective = postagSymbol == Symbol("JJR")
  def isSuperlativeAdjective = postagSymbol == Symbol("JJS")
  def isAdjective = isPlainAdjective || isComparativeAdjective || isSuperlativeAdjective

  def isPersonalPronoun = postagSymbol == Symbol("PRP")
  def isPossessivePronoun = postagSymbol == Symbol("PRP$")
  def isPronoun = isPersonalPronoun || isPossessivePronoun

  def isPossessive = isPossessivePronoun || postagSymbol == Symbol("POS")

  def isDeterminer = postagSymbol == Symbol("DT")
  def isCardinalNumber = postagSymbol == Symbol("CD")
  def isSuperlativeAdverb = postagSymbol == Symbol("RBS")
  def isPunctuation = punctuation.contains(postag)
  def isSubordinatingConjunction = postagSymbol == Symbol("IN")
  def isCoordinatingConjunction = postagSymbol == Symbol("CC")
  def isConjunction = isSubordinatingConjunction || isCoordinatingConjunction
  def isPreposition = postagSymbol == Symbol("IN")

  def isWhDeterminer = postagSymbol == Symbol("WDT")
  def isWhPronoun = postagSymbol == Symbol("WP")
  def isWhPossessivePronoun = postagSymbol == Symbol("WP$")
  def isWhAdverb = postagSymbol == Symbol("WRB")
  def isWhWord = isWhDeterminer || isWhPronoun || isWhPossessivePronoun || isWhAdverb

  val punctuation = Set("#", "$", "''", "(", ")", ",", ".", ":", "``")
}

object PostaggedToken {
  def apply(postag: String, string: String, offset: Int): PostaggedToken =
    new PostaggedToken(Symbol(postag), string, offset)

  def apply(token: Token, postag: String): PostaggedToken = PostaggedToken(postag, token.string, token.offset)

  def unapply(token: PostaggedToken): Option[(String, String, Int)] = Some((token.postag, token.string, token.offset))

  object bratFormat extends Format[PostaggedToken, String] {
    def write(token: PostaggedToken): String = {
      Iterator(token.postag + " " + token.offset + " " + token.offsets.end, token.string).mkString("\t")
    }

    def read(string: String): PostaggedToken = {
      string.split("\t") match {
        case Array(meat, token) =>
          meat.split("\\s+") match {
            case Array(postag, token, offset) => PostaggedToken(postag, token, offset.toInt)
            case _ => throw new MatchError("Could not match BRAT PostaggedToken: " + string)
          }
        case _ => throw new MatchError("Could not match BRAT PostaggedToken: " + string)
      }
    }
  }

  object stringFormat extends Format[PostaggedToken, String] {
    def write(postaggedToken: PostaggedToken): String = {
      Iterator(Token.stringFormat.write(postaggedToken), postaggedToken.postag).mkString(" ")
    }
    def read(str: String): PostaggedToken = {
      val postaggedTokenRegex = """(.*?) +([^ ]*)""".r
      try {
        val postaggedTokenRegex(pickledToken, postag) = str
        val token = Token.stringFormat.read(pickledToken)
        PostaggedToken(token, postag)
      } catch {
        case e: Exception => {
          throw new MatchError("Error deserializing PostaggedToken: " + str)
        }
      }
    }
  }
}
