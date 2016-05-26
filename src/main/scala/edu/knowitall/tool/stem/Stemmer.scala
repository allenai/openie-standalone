package edu.knowitall
package tool
package stem

import tokenize.Token
import postag.PostaggedToken

/** A stemmer takes a string token and produces a normalized form. */
abstract class Stemmer {
  def apply(word: String) = lemmatize(word)

  /** Apply the stemming algorithm. */
  def stem(word: String): String

  /** Stem a token without a postag. */
  def stemToken[T <: Token](token: T) = Lemmatized(token, this.stem(token.string))

  /** Apply the normalizing algorithm and then the stemming algorithm. */
  def lemmatize(word: String) = this.stem(Stemmer.normalize(word))

  /** Lemmatize a token without a postag. */
  def lemmatizeToken[T <: Token](token: T) = Lemmatized(token, this.lemmatize(token.string))
}

trait PostaggedStemmer {
  /** Some stemmers can take advantage of postags. */
  def stem(word: String, postag: String): String

  /** Apply the normalizing algorithm and then the stemming algorithm with postag. */
  def lemmatize(word: String, postag: String) = this.stem(Stemmer.normalize(word), postag)

  /** Stem a token with a postag. */
  def stemPostaggedToken[T <: PostaggedToken](token: T): Lemmatized[T] = Lemmatized(token, this.stem(token.string, token.postag))

  /** Lemmatize a token with a postag. */
  def lemmatizePostaggedToken[T <: PostaggedToken](token: T): Lemmatized[T] = Lemmatized(token, this.lemmatize(token.string, token.postag))
}

object Stemmer {
  /** Special characters to remove. */
  val remove = """[()\[\].,;:"']""".r;

  /** Remove special characters and lowercase the string. */
  def normalize(word: String) = Stemmer.remove.replaceAllIn(
    word.trim.toLowerCase, ""
  )
}
