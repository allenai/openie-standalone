package edu.knowitall
package tool
package stem

import java.io.StringReader

import edu.washington.cs.knowitall.morpha.{ MorphaStemmer => MorphaStem }
import uk.ac.susx.informatics.Morpha

/** This stemmer handles many cases, but the JFlex is 5 MB. */
class MorphaStemmer extends Stemmer with PostaggedStemmer {
  def stem(word: String) = MorphaStem.stemToken(word)

  override def stem(word: String, postag: String) = MorphaStem.stemToken(word, postag)
}

object MorphaStemmer extends MorphaStemmer

object MorphaStemmerMain
    extends StemmerMain {
  lazy val stemmer = new MorphaStemmer
}
