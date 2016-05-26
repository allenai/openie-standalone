package edu.knowitall.common

/** This object provides a function to generate a hash code
  * out of multiple hashable parts.
  *
  * @author  Michael Schmitz
  */
object HashCodeHelper {
  def apply(parts: Any*): Int = this.apply(41)(parts: _*)
  def apply(prime: Int)(parts: Any*): Int = {
    var code = 0;
    for (part <- parts) {
      code = prime * code + part.hashCode
    }

    code
  }
}
