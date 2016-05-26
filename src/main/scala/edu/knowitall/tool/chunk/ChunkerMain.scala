package edu.knowitall.tool
package chunk

abstract class ChunkerMain
    extends LineProcessor("chunker") {
  def chunker: Chunker
  override def process(line: String) =
    Chunker.multilineStringFormat.write(chunker.chunk(line))

  override def init(config: Config) {
    // for timing purposes
    chunker.chunk("I want to initialize the chunker.")
  }
}