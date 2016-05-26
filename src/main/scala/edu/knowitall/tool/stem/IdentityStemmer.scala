package edu.knowitall
package tool
package stem

/** A trivial stemmer that doesn't apply a stemming algorithm. */
object IdentityStemmer extends Stemmer {
  override def stem(word: String) = word

  implicit def instance: Stemmer = IdentityStemmer
}
