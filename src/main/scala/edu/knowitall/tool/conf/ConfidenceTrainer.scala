package edu.knowitall.tool.conf

/** A trainer for a confidence function.
  *
  * @tparam  E  the extraction the confidence function will rank
  * @param  featureSet  the features to use
  */
abstract class ConfidenceTrainer[E](features: FeatureSet[E, Double]) extends Trainer[E, Double](features) {
  override val apply = train _
  override def train(examples: Iterable[Labelled[E]]): ConfidenceFunction[E]
}