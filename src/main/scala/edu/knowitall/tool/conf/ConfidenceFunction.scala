package edu.knowitall.tool.conf

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

import edu.knowitall.common.Resource.using

/** A confidence function for ranking how likely an extraction is correct.
  *
  * @tparam  E  the extraction to rank
  * @param  featureSet  the features to use
  */
abstract class ConfidenceFunction[E](val featureSet: FeatureSet[E, Double]) extends Function[E, Double] {
  def apply(that: E): Double

  def save(output: OutputStream): Unit
  def saveFile(file: File) {
    using(new BufferedOutputStream(new FileOutputStream(file))) { stream =>
      this.save(stream)
    }
  }
}