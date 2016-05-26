package edu.knowitall.common.main

import edu.knowitall.common.Timing

/** This class is to be extended by an object to provide a simple main class
  * that processes lines in parallel.
  *
  * @author  Michael Schmitz
  */
abstract class ParallelLineProcessor {
  val groupSize = 1000

  def init(args: Array[String]) {}
  def exit(ns: Long) {}
  def process(line: String): String
  def main(args: Array[String]) {
    init(args)
    val lines = io.Source.stdin.getLines

    val lock = new Object()

    val ns = Timing.time {
      for (group <- lines.grouped(groupSize)) {
        for (line <- group.par) {
          lock.synchronized {
            println(process(line))
          }
        }
      }
    }

    exit(ns)
  }
}
