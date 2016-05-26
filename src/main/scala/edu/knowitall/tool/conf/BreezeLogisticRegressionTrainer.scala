/* michaels: commented out due to dependency changes.
 * Moving to Scala 2.11 forced us to upgrade breeze.
 * The new APIs are quite different than the old ones.

package edu.knowitall
package tool
package conf

import breeze.classify.LogisticClassifier
import breeze.data.Example
import breeze.linalg.DenseVector
import breeze.optimize.FirstOrderMinimizer.OptParams
import edu.knowitall.tool.conf.impl.LogisticRegression

class BreezeLogisticRegressionTrainer[E](features: FeatureSet[E, Double]) extends ConfidenceTrainer[E](features) {
  def trainBreezeClassifier(instances: Iterable[Labelled[E]], optParams: OptParams) = {
    val examples = instances.zipWithIndex map {
      case (Labelled(label, item: Any), i) =>
        val vector = DenseVector((1.0 +: features.vectorize(item.asInstanceOf[E])).toArray)
        Example[Boolean, DenseVector[Double]](label, vector, id = i.toString)
    }

    new LogisticClassifier.Trainer[Boolean, DenseVector[Double]](optParams).train(examples)
  }

  def train(labelled: Iterable[Labelled[E]], optParams: OptParams): LogisticRegression[E] = {
    val classifier = trainBreezeClassifier(labelled, optParams)

    val weights = (("Intercept" +: features.featureNames).iterator zip classifier.featureWeights.indexed(true).iterator.map(_._2)).toMap
    new LogisticRegression(features, weights, 0.0)
  }

  override def train(labelled: Iterable[Labelled[E]]): LogisticRegression[E] = {
    train(labelled, OptParams(useL1 = true))
  }
}

*/ 