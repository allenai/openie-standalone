package edu.knowitall.collection.mutable

import scala.collection.mutable
import scala.collection.immutable
import scala.collection.mutable.Builder
import scala.collection.mutable.MapBuilder
import scala.collection.generic.CanBuildFrom
import scala.collection.generic.CanBuildFrom

/** Patricia Trie implementation taken from "Programming in Scala" by Martin Odersky.
  *
  *   www.artima.com/scalazine/articles/scala_collections_architecture3.html
  *
  * I (Michael) was going to write my own implementation but when I looked for an
  * example how how to subclass Map properly I found this implementation.
  */
class PrefixMap[T]
    extends mutable.Map[String, T]
    with mutable.MapLike[String, T, PrefixMap[T]] {

  var suffixes: immutable.Map[Char, PrefixMap[T]] = Map.empty
  var value: Option[T] = None

  def get(s: String): Option[T] =
    if (s.isEmpty) value
    else suffixes get (s(0)) flatMap (_.get(s substring 1))

  def withPrefix(s: String): PrefixMap[T] =
    if (s.isEmpty) this
    else {
      val leading = s(0)
      suffixes get leading match {
        case None =>
          suffixes = suffixes + (leading -> empty)
        case _ =>
      }
      suffixes(leading) withPrefix (s substring 1)
    }

  override def update(s: String, elem: T) =
    withPrefix(s).value = Some(elem)

  override def remove(s: String): Option[T] =
    if (s.isEmpty) { val prev = value; value = None; prev }
    else suffixes get (s(0)) flatMap (_.remove(s substring 1))

  def iterator: Iterator[(String, T)] =
    (for (v <- value.iterator) yield ("", v)) ++
      (for (
        (chr, m) <- suffixes.iterator;
        (s, v) <- m.iterator
      ) yield (chr +: s, v))

  def +=(kv: (String, T)): this.type = { update(kv._1, kv._2); this }

  def -=(s: String): this.type = { remove(s); this }

  override def empty = new PrefixMap[T]
}

object PrefixMap {
  def empty[T] = new PrefixMap[T]

  def apply[T](kvs: (String, T)*): PrefixMap[T] = {
    val m: PrefixMap[T] = empty
    for (kv <- kvs) m += kv
    m
  }

  def newBuilder[T]: Builder[(String, T), PrefixMap[T]] =
    new MapBuilder[String, T, PrefixMap[T]](empty)

  implicit def canBuildFrom[T]: CanBuildFrom[PrefixMap[_], (String, T), PrefixMap[T]] =
    new CanBuildFrom[PrefixMap[_], (String, T), PrefixMap[T]] {
      def apply(from: PrefixMap[_]) = newBuilder[T]
      def apply() = newBuilder[T]
    }
}
