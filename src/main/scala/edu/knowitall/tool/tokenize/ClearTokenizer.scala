package edu.knowitall
package tool
package tokenize

import scala.collection.JavaConversions._
import edu.knowitall.common.Resource.using
import com.clearnlp.tokenization.AbstractTokenizer
import com.clearnlp.nlp.NLPGetter
import java.util.zip.ZipInputStream
import java.net.URL

class ClearTokenizer
    extends Tokenizer {
  val tokenizer = NLPGetter.getTokenizer("en")

  def tokenize(sentence: String): Seq[Token] = {
    val strings = tokenizer.getTokens(sentence)
    Tokenizer.computeOffsets(strings, sentence)
  }
}

object ClearTokenizerMain extends TokenizerMain {
  val tokenizer = new ClearTokenizer()
}
