package edu.knowitall.tool

import scala.util.Try

trait Writer[F, T] {
  def write(from: F): T
}

trait Reader[F, T] {
  def read(from: F): T
  def readTry(from: F): Try[T] = Try(this.read(from))
}

trait Format[F, T]
    extends Writer[F, T] with Reader[T, F] {
  def roundtrip(f: F) = read(write(f))
  def reverseRoundtrip(t: T) = write(read(t))
}
