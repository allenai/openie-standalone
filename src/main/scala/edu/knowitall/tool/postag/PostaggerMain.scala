package edu.knowitall.tool
package postag

abstract class PostaggerMain extends LineProcessor("postagger") {
  def tagger: Postagger
  override def process(line: String) =
    Postagger.multilineStringFormat.write(tagger(line))
}