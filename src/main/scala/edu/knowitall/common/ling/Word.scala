package edu.knowitall
package common
package ling

/** Functions that operate on words.
  *
  * @author  Michael Schmitz
  */
object Word {
  def capitalize(s: String) =
    s(0).toUpper + s.substring(1, s.length)
}
