package edu.knowitall.collection.mutable

import scala.collection.Iterable

/** A wrapper iterable that iterates over multiple iterables in parallel.
  *
  * @author  Michael Schmitz
  */
class MultiIterable[E](val iterables: Iterable[E]*) extends Iterable[List[E]] {
  def iterator = new MultiIterator(iterables.map(_.iterator): _*)
}

/** A wrapper iterator that iterates over multiple iterators in parallel.
  *
  * @author  Michael Schmitz
  */
class MultiIterator[E](private val iterators: Iterator[E]*) extends Iterator[List[E]] {
  /** true if all iterators have a next element */
  def hasNext = iterators.forall(_.hasNext)

  /** A list of the next elements of each iterator.  Should only be called if
    * `hasNext` is true.
    *
    * @throws  NoSuchElementException  any iterator has no next element
    */
  def next = iterators.map(_.next).toList
}
