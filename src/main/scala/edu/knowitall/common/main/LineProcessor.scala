package edu.knowitall.common.main

import java.util.Scanner

import edu.knowitall.common.Timing.time

/** This class is to be extended by an object to provide a simple main class
  * that processes lines.
  *
  * If the flag "-i" is specified, the program executes interactively.
  * Otherwise lines are expected from stdin.
  *
  * @author  Michael Schmitz
  */
abstract class LineProcessor {
  def init(args: Array[String]) {}
  def exit(ns: Long) {}
  def process(line: String): String
  def main(args: Array[String]) {
    init(args)
    val scanner = new Scanner(System.in, "UTF-8")

    val condition =
      if (args.length > 0 && args.contains("-i")) {
        () => true
      }
      else {
        () => scanner.hasNextLine
      }

    val ns = time {
      while (condition()) {
        println(process(scanner.nextLine))
      }
    }

    exit(ns)
  }
}
