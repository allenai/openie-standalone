package edu.knowitall.common

import scala.util
import scala.collection.mutable.ArrayBuffer

/** Functions that help with tasks involving randomness.
  *
  * @author  Michael Schmitz
  */
object Random {
  /* choose a single element out of an interable of unknown size */
  def choose[A](iterable: Iterable[A], rand: util.Random): A = {
    assert(!iterable.isEmpty, "iterable must not be empty")
    val iterator = iterable.iterator
    def rec(n: Int, choice: A): A = {
      if (iterator.hasNext) {
        val next = iterator.next
        if (rand.nextDouble() * n < 1) {
          rec(n + 1, next)
        }
        else {
          rec(n + 1, choice)
        }
      } else {
        choice
      }
    }

    rec(2, iterator.next)
  }

  /* choose a single element out of an interable of known size */
  def choose[A](iterable: Traversable[A], size: Int, rand: util.Random): A = {
    assert(!iterable.isEmpty, "iterable must not be empty")
    val index = rand.nextInt(size)
    iterable.drop(index).head
  }

  /* choose n elements out of an iterable of known size */
  def select[A](iterable: Iterable[A], size: Int, n: Int, rand: util.Random): Iterable[A] = {
    assert(size >= n, "set is smaller than n: " + size + " < " + n)
    var k = n
    for {
      (item, i) <- iterable.zipWithIndex
      if (rand.nextDouble * (size - i) < k)
    } yield {
      k -= 1; item
    }
  }

  /* choose n elements out of an iterable of unknown size */
  def select[A: Manifest](iterable: Iterable[A], k: Int, rand: util.Random): Iterable[A] = {
    assert(!iterable.isEmpty, "iterable must not be empty")
    var result = new Array[A](k)
    for ((item, i) <- iterable.zipWithIndex) {
      if (i < k) {
        result(i) = item
      } else {
        val s = rand.nextInt(i + 1)
        if (s < k) result.update(s, item)
      }
    }

    result
  }
}
