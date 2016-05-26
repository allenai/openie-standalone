package edu.knowitall.common

/** Functions to aid with timing.
  *
  * @author  Michael Schmitz
  */
object Timing {
  /** Intervals are used to format time in different units. */
  sealed abstract class Interval {
    def divisor: Long
    def symbol: String
    def format(duration: Long) = "%.2f".format(duration.toDouble / divisor.toDouble) + " " + symbol
  }
  case object Days extends Interval {
    override val divisor = 24L * Hours.divisor
    override val symbol = "days"
  }
  case object Hours extends Interval {
    override val divisor = 60L * Minutes.divisor
    override val symbol = "hrs"
  }
  case object Minutes extends Interval {
    override val divisor = 60L * Seconds.divisor
    override val symbol = "mins"
  }
  case object Seconds extends Interval {
    override val divisor = 1000L * Milliseconds.divisor
    override val symbol = "s"
  }
  case object Milliseconds extends Interval {
    override val divisor = 1000L * Microseconds.divisor
    override val symbol = "ms"
  }
  case object Microseconds extends Interval {
    override val divisor = 1000L * Nanoseconds.divisor
    override val symbol = "μs"
  }
  case object Nanoseconds extends Interval {
    override val divisor = 1L
    override val symbol = "ns"
  }

  /** Return a tuple, where the first part is the nanoseconds
    * taken and the second part is the result of the block
    * execution.
    */
  def time[R](block: => R): (Long, R) = {
    val start = System.nanoTime
    val result = block
    (System.nanoTime - start, result)
  }

  /** Return the time a unit block takes to finish.
    */
  def time[R](block: => Unit): Long = {
    val start = System.nanoTime
    val result = block
    System.nanoTime - start
  }

  /** Execute the block and pass the time taken to the handler. */
  def time[R](block: => R, handler: Long => Any): R = {
    val (ns, result) = time(block)
    handler(ns)
    result
  }

  /** Execute the block and pass the time taken to the handler. */
  def timeThen[R](block: => R)(handler: Long => Any): R = {
    val (ns, result) = time(block)
    handler(ns)
    result
  }

  /** Compute the time since ns. */
  def since(ns: Long): Long = System.nanoTime - ns

  /** Execute the command and print the time taken. */
  def printTime[R](block: => R) {
    time(block, println)
  }

  /** A class to store timing statistics. */
  class Stats(val total: Long, val max: Long, val min: Long, val avg: Long) {
    override def toString =
      "total: " + Milliseconds.format(total) + ",  " +
        "min: " + Milliseconds.format(min) + ",  " +
        "max: " + Milliseconds.format(max) + ",  " +
        "avg: " + Milliseconds.format(avg)
  }

  /** Execute the blocks and report statistics about the time taken. */
  def speedTest(executions: List[() => Any]) = {
    val times = executions.map(e => time({ e() })._1)

    val total = times.reduce(_ + _)
    val max = times.reduce(math.max(_, _))
    val min = times.reduce(math.min(_, _))
    val avg = total / executions.size

    new Stats(total, max, min, avg)
  }
}
