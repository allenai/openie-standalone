package edu.knowitall
package tool
package chunk

import java.net.URL
import opennlp.tools.chunker._
import edu.knowitall.common.Resource

class OpenNlpChunker(
  val model: ChunkerModel,
  override val postagger: postag.Postagger
)
    extends Chunker {
  def this(postagger: postag.Postagger) =
    this(OpenNlpChunker.loadDefaultModel(), postagger)

  def this() = this(new postag.OpenNlpPostagger())

  val chunker = new ChunkerME(model)

  def chunkPostagged(tokens: Seq[postag.PostaggedToken]): Seq[ChunkedToken] = {
    val chunks = chunker.chunk(tokens.map(_.string).toArray, tokens.map(_.postag).toArray)
    (tokens zip chunks) map { case (token, chunk) => ChunkedToken(token, chunk) }
  }
}

object OpenNlpChunker {
  private def defaultModelName = "en-chunker.bin"
  val defaultModelUrl: URL = {
    val url = this.getClass.getClassLoader.getResource(defaultModelName)
    require(url != null, "Could not load default chunker model: " + defaultModelName)
    url
  }

  def loadDefaultModel(): ChunkerModel = loadModel(defaultModelUrl)

  private def loadModel(url: URL): ChunkerModel = {
    Resource.using(url.openStream()) { stream =>
      new ChunkerModel(stream)
    }
  }
}

object OpenNlpChunkerMain extends ChunkerMain {
  lazy val chunker = new OpenNlpChunker()
}
