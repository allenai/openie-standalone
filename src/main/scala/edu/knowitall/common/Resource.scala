package edu.knowitall.common

import scala.language.reflectiveCalls

/** Functions for managing resources. */
object Resource {
  /** A using clause that uses structural typing so it can be used on
    * any object with a close method.
    *
    * The supplied block will run with the allocated resource.
    * The resource will be cleaned up when the block is complete.
    */
  def using[T <: { def close(): Unit }, S](obj: T)(operation: T => S) = {
    val result = try {
      operation(obj)
    } finally {
      obj.close()
    }

    result
  }
}
