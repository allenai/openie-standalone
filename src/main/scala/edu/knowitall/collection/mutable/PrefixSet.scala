package edu.knowitall.collection.mutable

import scala.collection.mutable

class PrefixSet
    extends mutable.Set[String]
    with mutable.SetLike[String, PrefixSet] {

  val map = PrefixMap[AnyRef]()

  def contains(key: String) = {
    map.contains(key)
  }

  def +=(elem: String) = { map += (elem -> null); this }
  def -=(elem: String) = { map -= elem; this }
  def iterator = map.iterator.map(_._1)
  override def empty = new PrefixSet

}

object PrefixSet {
  def apply[String](xs: (String, String)*) = {
    new PrefixSet ++ xs
  }
}
