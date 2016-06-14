package edu.knowitall.collection.immutable

import scala.collection.immutable
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Builder
import scala.collection.generic.CanBuildFrom
import scala.collection.IterableLike

/** An immutable data structure that is similar to a set but can store
  * multiple instances of any one value.  Also known as a multiset.
  *
  * Bags are created using the companion object.
  *
  * @author  Michael Schmitz
  */
class Bag[T] private (private val bagmap: immutable.Map[T, Int], override val size: Int)
    extends Iterable[T] with IterableLike[T, Bag[T]] {
  private def this() = this(immutable.Map[T, Int]().withDefaultValue(0), 0)

  def apply(k: T): Int = bagmap(k)

  // override Object
  override def equals(that: Any) = that match {
    case that: Bag[_] => that.bagmap == this.bagmap
    case _ => false
  }
  override def hashCode = bagmap.hashCode

  // override Traversable
  override def iterator =
    if (bagmap.isEmpty) {
      Iterator.empty
    }
    else {
      bagmap map { case (k, v) => Iterator.continually(k).take(v) } reduce (_ ++ _)
    }

  override def newBuilder = Bag.newBuilder[T]

  /** Efficiently convert to a immutable.Map */
  def asMap = this.bagmap

  /** Add an item and a count to this bag.
    *
    * @param  k  an item to add
    * @param  sumand  the count to add
    * @return  a bag with `count` more of key `k`
    */
  def add(k: T, sumand: Int): Bag[T] = {
    val v = bagmap(k) + sumand
    require(v >= 0, "values must be >= 0")
    new Bag[T](bagmap + (k -> v), size + sumand)
  }

  /** Efficiently merge another bag with this bag.
    *
    * @param  the bag to merge with
    * @return  the merged bag
    */
  def merge(that: Bag[T]) = {
    that.bagmap.foldLeft(this) { case (bag, (v, c)) => bag add (v, c) }
  }

  /** Add multiple items to this bag. */
  def ++(vs: Traversable[T]) = vs.foldLeft(this)((bag, v) => bag + v)

  /** Add an item and a count as a tuple to this bag. */
  def +(kv: (T, Int)): Bag[T] = kv match {
    case (k, v) =>
      this add (k, v)
  }

  /** Remove an item and a count as a tuple from this bag. */
  def -(kv: (T, Int)): Bag[T] = kv match {
    case (k, v) =>
      this add (k, -v)
  }

  /** Add a single item to this bag. */
  def +(k: T): Bag[T] =
    this add (k, 1)

  /** Remove a single item from this bag. */
  def -(k: T): Bag[T] =
    this add (k, -1)

  /** Update the count of a particular key in this bag.
    *
    * @param  kv  a tuple of the key and the new count
    * @return  a bag with key updated to the count
    */
  def update(kv: (T, Int)): Bag[T] = kv match {
    case (k, v) =>
      require(v >= 0, "values must be >= 0")
      new Bag[T](bagmap + kv, size - bagmap(k) + v)
  }

  /** Remove a particular key from this bag. */
  def removeKey(k: T): Bag[T] =
    new Bag[T](bagmap - k, size - bagmap(k))

  /** Get the count of a key from this bag
    *
    * @param  k  the key to lookup
    * @return  `Some` of the count if the bag contains the key, or `None`
    */
  def get(k: T): Option[Int] = {
    bagmap.get(k)
  }

  /** An iterable of the unique keys in this bag. */
  def uniques: Iterable[T] = this.bagmap.keys
}

object Bag {
  /** Create a bag from a traversable of tuples where
    * the first part of the tuple is the key and the
    * second part of the tuple is the count for that key.
    */
  def fromCounts[T](ts: TraversableOnce[(T, Int)]): Bag[T] = {
    ts.foldLeft(Bag.empty[T]) {
      case (bag, (k, v)) =>
        bag.add(k, v)
    }
  }

  /** Create a bag from a traversable of the bag's elements. */
  def from[T](ts: TraversableOnce[T]): Bag[T] = {
    ts.foldLeft(Bag.empty[T]) { (bag, k) =>
      bag + k
    }
  }

  /** Create a new empty bag. */
  def apply[T]() = new Bag[T]()

  /** Create a new bag initialized with the specified elements. */
  def apply[T](varargs: T*) = from(varargs)

  /** An empty bag. */
  def empty[T] = new Bag[T]()

  class BagBuilder[T](empty: Bag[T]) extends Builder[T, Bag[T]] {
    var bag: Bag[T] = empty

    def clear {
      bag = Bag.empty[T]
    }

    def +=(elem: T): this.type = {
      bag += elem
      this
    }

    def result = { bag }
  }

  def newBuilder[T]: Builder[T, Bag[T]] = new BagBuilder[T](Bag.empty[T])

  implicit def canBuildFrom[T] =
    new CanBuildFrom[Bag[T], T, Bag[T]] {
      def apply() = newBuilder
      def apply(from: Bag[T]) = newBuilder
    }
}
