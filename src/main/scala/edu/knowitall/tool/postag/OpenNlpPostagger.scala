package edu.knowitall
package tool
package postag

import java.net.URL
import opennlp.tools.postag._
import tool.tokenize.Token
import edu.knowitall.common.Resource

class OpenNlpPostagger(
  val model: POSModel,
  override val tokenizer: tokenize.Tokenizer
)
    extends Postagger {

  def this(tokenizer: tokenize.Tokenizer) =
    this(OpenNlpPostagger.loadDefaultModel(), tokenizer)

  def this() = this(new tokenize.OpenNlpTokenizer())

  val tagger = new POSTaggerME(model)

  override def postagTokenized(tokens: Seq[Token]): Seq[PostaggedToken] = {
    val postags = tagger.tag(tokens.iterator.map(_.string).toArray)
    (tokens zip postags).map {
      case (token, postag) =>
        PostaggedToken(token, postag)
    }
  }
}

object OpenNlpPostagger {
  private def defaultModelName = "en-pos-maxent.bin"

  val defaultModelUrl: URL = {
    val url = this.getClass.getClassLoader.getResource(defaultModelName)
    require(url != null, "Could not load default postagger model: " + defaultModelName)
    url
  }

  def loadDefaultModel(): POSModel = {
    Resource.using(defaultModelUrl.openStream()) { stream =>
      new POSModel(stream)
    }
  }
}

object OpenNlpPostaggerMain extends PostaggerMain {
  override val tagger = new OpenNlpPostagger()
}
