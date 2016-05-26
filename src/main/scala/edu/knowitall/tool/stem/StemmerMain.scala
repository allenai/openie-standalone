package edu.knowitall
package tool
package stem

abstract class StemmerMain
    extends LineProcessor("stemmer") {
  def stemmer: Stemmer
  override def process(line: String) = line.split("\\s+").map(stemmer.stem(_)).mkString(" ")
}