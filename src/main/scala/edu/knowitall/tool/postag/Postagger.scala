package edu.knowitall
package tool
package postag

import edu.knowitall.tool.tokenize._
import scala.collection.immutable

/** A POS tagger takes tokenized input and associates a part of speech
  * tag with each token.
  */
abstract class Postagger {
  def tokenizer: tokenize.Tokenizer

  def apply(sentence: String) = postag(sentence)

  /* POS tag pre-tokenized text */
  def postagTokenized(tokens: Seq[tokenize.Token]): Seq[PostaggedToken]

  /* Tokenize and then POS tag text*/
  def postag(sentence: String): Seq[PostaggedToken] = {
    val tokens = tokenizer.tokenize(sentence)
    postagTokenized(tokens)
  }
}

object Postagger {
  val subjectPronouns = immutable.Set("i", "you", "he", "she", "it", "they", "we")
  val objectPronouns = immutable.Set("me", "you", "him", "her", "it", "us", "them")
  val reflexivePronouns = immutable.Set("myself", "yourself", "himself", "itself", "themself", "ourselves", "yourselves", "themselves")
  val possessivePronouns = immutable.Set("mine", "yours", "his", "hers", "its", "ours", "theirs", "whose")
  val pronouns = subjectPronouns ++ objectPronouns ++ reflexivePronouns ++ possessivePronouns

  val articles = immutable.Set("a", "an", "the")
  val possessives = immutable.Set("my", "your", "his", "her", "its", "one's", "our", "your", "their")

  val whWords = immutable.Set("what", "which", "who", "whose", "that", "where", "when")

  val simplePrepositions = immutable.Set(
    "a", "abaft", "aboard", "about", "above", "absent", "across", "afore",
    "after", "against", "along", "alongside", "amid", "amidst", "among",
    "amongst", "an", "apropos", "around", "as", "aside", "astride", "at",
    "athwart", "atop", "barring", "before", "behind", "below", "beneath",
    "beside", "besides", "between", "betwixt", "beyond", "but", "by", "circa",
    "concerning", "despite", "down", "during", "except", "excluding",
    "failing", "following", "for", "from", "given", "in", "including",
    "inside", "into", "lest", "like", "mid", "midst", "minus", "modulo",
    "near", "next", "notwithstanding", "of", "off", "on", "onto", "opposite",
    "out", "outside", "over", "pace", "past", "per", "plus", "pro", "qua",
    "regarding", "round", "sans", "save", "since", "than", "through",
    "thru", "throughout", "thruout", "till", "times", "to", "toward",
    "towards", "under", "underneath", "unlike", "until", "up", "upon",
    "versus", "vs.", "v.", "via", "vice", "with", "within", "without",
    "worth"
  )

  val complexPrepositions = immutable.Set(
    "according to", "ahead of", "apart from", "as for", "as of", "as per",
    "as regards", "aside from", "back to", "because of", "close to",
    "due to", "except for", "far from", "in to", "inside of", "instead of",
    "left of", "near to", "next to", "on to", "out from", "out of",
    "outside of", "owing to", "prior to", "pursuant to", "regardless of",
    "right of", "subsequent to", "thanks to", "that of", "up to",
    "where as", "as far as", "as well as"
  )

  val prepositions = simplePrepositions ++ complexPrepositions

  def tokensFrom(postags: Seq[String], tokens: Seq[Token]): Seq[PostaggedToken] = {
    (postags zip tokens).map { case (postag, token) => PostaggedToken(token, postag) }
  }

  class stringFormat(val delim: String) extends Format[Seq[PostaggedToken], String] {
    def write(tokens: Seq[PostaggedToken]): String = {
      val serializedTokens = for (tok <- tokens) yield {
        PostaggedToken.stringFormat.write(tok)
      }
      serializedTokens.mkString(delim)
    }

    def read(string: String): Seq[PostaggedToken] = {
      for (str <- string.split(delim)) yield PostaggedToken.stringFormat.read(str)
    }
  }
  object stringFormat extends stringFormat("\t")
  object multilineStringFormat extends stringFormat("\n")
}
