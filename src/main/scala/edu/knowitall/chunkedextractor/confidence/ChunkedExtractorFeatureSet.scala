package edu.knowitall.chunkedextractor.confidence

import edu.knowitall.tool.conf.FeatureSet
import edu.knowitall.tool.conf.Feature
import scala.collection.immutable.SortedMap
import edu.knowitall.tool.srl.FrameHierarchy
import java.util.regex.Pattern
import java.util.regex.Pattern
import edu.knowitall.chunkedextractor.BinaryExtractionInstance
import edu.knowitall.tool.chunk.ChunkedToken

object ChunkedExtractorFeatureSet extends FeatureSet[BinaryExtractionInstance[ChunkedToken], Double](ChunkedExtractorFeatures.featureMap)

/** Features defined for OllieExtractionInstances */
object ChunkedExtractorFeatures {
  type ChunkedExtractorFeature = Feature[BinaryExtractionInstance[ChunkedToken], Double]

  implicit def boolToDouble(bool: Boolean) = if (bool) 1.0 else 0.0

  object startExtr extends ChunkedExtractorFeature("sent starts w/ extr") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.extr.arg1.tokenInterval.start == 0 ||
        inst.extr.arg2.tokenInterval.start == 0
    }
  }

  object endArg2 extends ChunkedExtractorFeature("sent ends w/ extr") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.extr.arg1.tokenInterval.end == inst.sent.size ||
        inst.extr.arg2.tokenInterval.end == inst.sent.size
    }
  }

  object pronounBeforeRel extends ChunkedExtractorFeature("which|who|that before rel") {
    val targets = Set("which", "who", "that")
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      val res = inst.sent.take(inst.extr.rel.tokenInterval.start).lastOption.map { prev =>
        targets contains prev.string
      }.getOrElse(false)

      res
    }
  }

  object arg1Proper extends ChunkedExtractorFeature("arg1 is proper") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.extr.arg1.tokens.exists(_.isProperNoun)
    }
  }

  object arg2Proper extends ChunkedExtractorFeature("arg2 is proper") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.extr.arg2.tokens.exists(_.isProperNoun)
    }
  }

  object extrCoversSentence extends ChunkedExtractorFeature("extr covers sent") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.extr.tokenInterval.start == 0 && inst.extr.tokenInterval.end == inst.sent.size
    }
  }

  object npBeforeExtr extends ChunkedExtractorFeature("np before extr") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.sent.take(inst.extr.tokenInterval.start).exists(_.chunk == "B-NP")
    }
  }

  object npAfterExtr extends ChunkedExtractorFeature("np after extr") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      val next = inst.sent.drop(inst.extr.rel.tokenInterval.end).headOption
      val res = next.map(next => next.chunk == "B-NP" || next.chunk == "I-NP").getOrElse(false)
      res
    }
  }

  object conjBeforeRel extends ChunkedExtractorFeature("conj before rel") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      val res = inst.sent.take(inst.extr.rel.tokenInterval.start).lastOption.map(_.chunk == "CC").getOrElse(false)
      res
    }
  }

  object prepBeforeExtr extends ChunkedExtractorFeature("prep before extr") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      val res = inst.sent.take(inst.extr.rel.tokenInterval.start).lastOption.map(_.chunk == "IN").getOrElse(false)
      res
    }
  }

  object verbAfterExtr extends ChunkedExtractorFeature("verb after extr") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      val res = inst.sent.drop(inst.extr.rel.tokenInterval.end).headOption.map(_.isVerb).getOrElse(false)
      res
    }
  }

  object prepAfterExtr extends ChunkedExtractorFeature("prep after extr") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      val res = inst.sent.drop(inst.extr.rel.tokenInterval.end).headOption.map(_.isVerb).getOrElse(false)
      res
    }
  }

  object arg1ContainsPronoun extends ChunkedExtractorFeature("arg1 contains pronoun") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.extr.arg1.tokens.exists(_.isPronoun)
    }
  }

  object arg2ContainsPronoun extends ChunkedExtractorFeature("arg2 contains pronoun") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.extr.arg1.tokens.exists(_.isPronoun)
    }
  }

  object arg1ContainsPosPronoun extends ChunkedExtractorFeature("arg1 contains PRP$") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.extr.arg1.tokens.exists(_.isPossessivePronoun)
    }
  }

  object arg2ContainsPosPronoun extends ChunkedExtractorFeature("arg2 contains PRP$") {
    override def apply(inst: BinaryExtractionInstance[ChunkedToken]): Double = {
      inst.extr.arg1.tokens.exists(_.isPossessivePronoun)
    }
  }

  def features: Seq[ChunkedExtractorFeature] = Seq(
    startExtr,
    endArg2,
    pronounBeforeRel,
    arg1Proper,
    arg2Proper,
    extrCoversSentence,
    npBeforeExtr,
    npAfterExtr,
    conjBeforeRel,
    prepBeforeExtr,
    verbAfterExtr,
    prepAfterExtr,
    arg1ContainsPronoun,
    arg2ContainsPronoun,
    arg1ContainsPosPronoun,
    arg2ContainsPosPronoun
  )

  def featureMap: SortedMap[String, ChunkedExtractorFeature] = {
    (for (f <- features) yield (f.name -> Feature.from(f.name, f.apply _)))(scala.collection.breakOut)
  }
}
