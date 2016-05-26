package edu.knowitall.chunkedextractor

import edu.knowitall.tool.chunk.ChunkedToken

trait JavaChunkedExtractor {
  def apply(tokens: Seq[ChunkedToken]): Seq[BinaryExtractionInstance[ChunkedToken]]
  def extractWithConfidence(tokens: Seq[ChunkedToken]): Seq[(Double, BinaryExtractionInstance[ChunkedToken])]
}
