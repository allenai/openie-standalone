package edu.knowitall
package tool
package tokenize

import java.net.URL
import scala.collection.JavaConverters._
import opennlp.tools.tokenize.{ TokenizerME, TokenizerModel }
import opennlp.tools.util.Span
import edu.knowitall.common.Resource

class OpenNlpTokenizer(val model: TokenizerModel) extends Tokenizer {
  def this() = this(OpenNlpTokenizer.loadDefaultModel())

  val tokenizer = new TokenizerME(model)

  def tokenize(sentence: String): Seq[Token] = {
    val positions = tokenizer.tokenizePos(sentence)
    val strings = positions.map {
      pos => sentence.substring(pos.getStart, pos.getEnd)
    }

    assume(positions.length == strings.length)

    for ((pos, string) <- (positions.iterator zip strings.iterator).toList)
      yield new Token(string, pos.getStart)
  }
}

object OpenNlpTokenizer {
  private def defaultModelName = "en-token.bin"

  val defaultModelUrl: URL = {
    val url = this.getClass.getClassLoader.getResource(defaultModelName)
    require(url != null, "Could not load default tokenizer model: " + defaultModelName)
    url
  }

  def loadDefaultModel(): TokenizerModel = {
    Resource.using(defaultModelUrl.openStream()) { stream =>
      new TokenizerModel(stream)
    }
  }
}

object OpenNlpTokenizerMain extends TokenizerMain {
  val tokenizer = new OpenNlpTokenizer()
}
