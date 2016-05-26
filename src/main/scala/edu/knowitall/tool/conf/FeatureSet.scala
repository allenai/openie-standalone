package edu.knowitall.tool.conf
import scala.collection.immutable.SortedMap

/** FeatureSet represents a set of features on T that can be
  * represented as a double.
  *
  * @param  featureMap  a lookup for the features
  */
class FeatureSet[T, V](val featureMap: SortedMap[String, Feature[T, V]]) {
  def this() = this(SortedMap.empty[String, Feature[T, V]])

  def apply(name: String) = featureMap(name)

  def featureNames(): Seq[String] =
    featureMap.keys.toSeq

  def numFeatures(): Int =
    featureNames.size

  def vectorize(example: T): Seq[V] =
    featureNames.map({ name =>
      val featureFunction = featureMap(name)
      featureFunction(example)
    })(scala.collection.breakOut)
}

object FeatureSet {
  val binaryClass = true

  def apply[T, V](features: Iterable[Feature[T, V]]): FeatureSet[T, V] = {
    new FeatureSet[T, V](SortedMap.empty[String, Feature[T, V]] ++
      features.map(feature => (feature.name, feature)))
  }
}
