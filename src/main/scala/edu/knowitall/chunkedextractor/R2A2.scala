package edu.knowitall.chunkedextractor

import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.collection.immutable.Interval

import edu.washington.cs.knowitall.extractor.ReVerbExtractor
import edu.washington.cs.knowitall.commonlib.Range
import edu.washington.cs.knowitall.nlp.ChunkedSentence
import edu.washington.cs.knowitall.nlp.extraction.ChunkedExtraction
import edu.washington.cs.knowitall.extractor.conf.ConfidenceFunction
import edu.washington.cs.knowitall.util.DefaultObjects
import edu.washington.cs.knowitall.extractor.conf.ReVerbOpenNlpConfFunction
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction
import edu.washington.cs.knowitall.extractor
import edu.washington.cs.knowitall.argumentidentifier.ConfidenceMetric

class R2A2(val r2a2: extractor.R2A2, val conf: Option[ConfidenceMetric] = None) extends Extractor[Seq[ChunkedToken], BinaryExtractionInstance[ChunkedToken]] with JavaChunkedExtractor {
  def this() = this(new extractor.R2A2, Some(new ConfidenceMetric))

  private def confidence(extr: ChunkedBinaryExtraction): Double =
    (conf map (_ getConf extr)).getOrElse {
      throw new IllegalArgumentException("No confidence function defined.")
    }

  private def reverbExtract(tokens: Seq[ChunkedToken]) = {
    import collection.JavaConverters._

    val chunkedSentence = new ChunkedSentence(
      tokens.map(token => Range.fromInterval(token.offset, token.offset + token.string.length)).toArray,
      tokens.map(_.string).toArray,
      tokens.map(_.postag).toArray,
      tokens.map(_.chunk).toArray
    )

    val extrs = r2a2.extract(chunkedSentence)
    extrs.asScala
  }

  private def convertExtraction(tokens: Seq[ChunkedToken])(extr: ChunkedBinaryExtraction) = {
    def convertPart(ce: ChunkedExtraction) = {
      val interval = Interval.open(ce.getRange.getStart, ce.getRange.getEnd)
      new ExtractionPart(ce.getText, tokens.view(interval.start, interval.end), interval)
    }

    new BinaryExtraction(convertPart(extr.getArgument1), convertPart(extr.getRelation), convertPart(extr.getArgument2))
  }

  def apply(tokens: Seq[ChunkedToken]): Seq[BinaryExtractionInstance[ChunkedToken]] = {
    (reverbExtract(tokens) map convertExtraction(tokens) map (extr => new BinaryExtractionInstance(extr, tokens)))(
      scala.collection.breakOut
    )
  }

  @deprecated("Use extractWithConfidence", "2.4.1")
  def extractWithConf(tokens: Seq[ChunkedToken]): Seq[(Option[Double], BinaryExtractionInstance[ChunkedToken])] = {
    val extrs = reverbExtract(tokens)
    val confs = extrs map { extr =>
      conf.map(_.getConf(extr))
    }

    val converted = extrs map (extr => new BinaryExtractionInstance(convertExtraction(tokens)(extr), tokens))
    (confs.iterator zip converted.iterator).toList
  }

  def extractWithConfidence(tokens: Seq[ChunkedToken]): Seq[(Double, BinaryExtractionInstance[ChunkedToken])] = {
    val extrs = reverbExtract(tokens)
    val confs = extrs map this.confidence

    val converted = extrs map (extr => new BinaryExtractionInstance(convertExtraction(tokens)(extr), tokens))
    (confs.iterator zip converted.iterator).toList
  }
}
