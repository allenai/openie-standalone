package edu.knowitall.chunkedextractor

abstract class Extractor[A, B] extends Function[A, Iterable[B]] {
  def extract(a: A) = this.apply(a)
}
