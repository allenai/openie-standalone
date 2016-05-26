package edu.knowitall.tool
package segment

class RemoteSegmenter(val urlString: String) extends Segmenter with Remote {
  def segment(sentence: String) = {
    val response = this.post(sentence)
    response.split("\\n").map(Segment.deserialize)(scala.collection.breakOut)
  }
}