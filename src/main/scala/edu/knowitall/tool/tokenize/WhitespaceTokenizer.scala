package edu.knowitall
package tool
package tokenize

/* The PennTokenizer was used to tokenize the Penn Treebank.
 * The following is a translation from a sed file.  This algorithm
 * is entirely deterministic.  It is composed of regular expression
 * replacements.
 *
 * @author  Michael Schmitz
 */
object WhitespaceTokenizer extends Tokenizer {
  override def tokenize(string: String) =
    Tokenizer.computeOffsets(string.split("\\s+").toSeq, string)
}

object WhitespaceTokenizerMain extends LineProcessor("whitespace-tokenizer") {
  val tokenizer = WhitespaceTokenizer
  override def process(sentence: String) =
    tokenizer.tokenize(sentence).mkString(" ")
}
