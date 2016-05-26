package edu.knowitall.tool.conf

/** An abstract representation for a feature used by the
  * confidence function.
  *
  * @param  name  a human-readable name for this feature
  */
abstract class Feature[E, V](val name: String) extends Function[E, V] {
  def apply(that: E): V
}

object Feature {
  /** A convenience factory method for creating a Feature from
    * an anonymous function.
    */
  def from[E, V](name: String, f: E => V) = new Feature[E, V](name) {
    override def apply(that: E): V = f(that)
  }

  implicit def booleanToDouble[E](feature: Feature[E, Boolean]) =
    new Feature[E, Double](feature.name) {
      override def apply(item: E) = {
        if (feature(item)) 1.0
        else 0.0
      }
    }
}