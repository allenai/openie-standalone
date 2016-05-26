package edu.knowitall
package tool
package sentence

import java.net.URL
import edu.knowitall.tool.segment.Segmenter
import edu.knowitall.tool.segment.SegmenterMain
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.segment.Segment
import edu.knowitall.common.Resource

/** A max-ent sentencer. */
class OpenNlpSentencer(val model: SentenceModel) extends Segmenter {
  val sentencer = new SentenceDetectorME(model)

  def this() = this(OpenNlpSentencer.loadDefaultModel())

  override def segmentTexts(document: String) = {
    sentencer.sentDetect(document)
  }

  def segment(document: String) = {
    val spans = sentencer.sentPosDetect(document)
    spans.map { span =>
      val text = document.substring(span.getStart, span.getEnd)
      val offset = span.getStart()
      Segment(text, offset)
    }
  }
}

object OpenNlpSentencer {
  private def defaultModelName = "en-sent.bin"

  val defaultModelUrl: URL = {
    val url = this.getClass.getClassLoader.getResource(defaultModelName)
    require(url != null, "Could not load default sentencer model: " + defaultModelName)
    url
  }

  def loadDefaultModel(): SentenceModel = {
    Resource.using(defaultModelUrl.openStream()) { stream =>
      new SentenceModel(stream)
    }
  }
}

object OpenNlpSegmenterMain
    extends SegmenterMain {
  lazy val sentencer = new OpenNlpSentencer
}
