package edu.knowitall.chunkedextractor.confidence

import org.slf4j.LoggerFactory
import edu.knowitall.tool.conf.impl.LogisticRegression
import edu.knowitall.tool.conf.FeatureSet
import java.net.URL
import edu.knowitall.chunkedextractor.BinaryExtractionInstance
import edu.knowitall.tool.chunk.ChunkedToken

object ChunkedExtractorConfidenceFunction {
  val logger = LoggerFactory.getLogger(this.getClass)

  def fromUrl(featureSet: FeatureSet[BinaryExtractionInstance[ChunkedToken], Double], url: URL) = {
    LogisticRegression.fromUrl(featureSet, url)
  }
}

object RelnounConfidenceFunction {
  val defaultModelUrl = Option(this.getClass.getResource("relnoun-confidence.txt")).getOrElse {
    throw new IllegalArgumentException("Could not load confidence function resource.")
  }

  def loadDefaultClassifier(): LogisticRegression[BinaryExtractionInstance[ChunkedToken]] = {
    ChunkedExtractorConfidenceFunction.fromUrl(ChunkedExtractorFeatureSet, defaultModelUrl)
  }
}