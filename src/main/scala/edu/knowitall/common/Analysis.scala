package edu.knowitall.common

/** Functions to help with statistical analysis of results.
  *
  * @author  Michael Schmitz
  */
object Analysis {

  /** Compute the area under curve from yield-precision points.
    *
    * @return  a yield, precision point
    */
  def areaUnderCurve(points: Seq[(Int, Double)]) = {
    val max = points.iterator.map(_._1).max

    val it = points.iterator.buffered
    var cur = (0, 1.0)
    var area = 0.0
    while (it.hasNext) {
      // save last point
      val (lastYld, lastPrc) = cur

      // increment iterator
      cur = it.next
      val (yld, prc) = cur

      area += 0.5 * (yld - lastYld) * (prc + lastPrc)
    }

    area
  }

  /** Compute precision yield points for each yield value.
    * Scores should be ordered by confidence, descending.
    *
    * @return  a yield, precision point
    */
  def precisionYield(scores: Seq[Boolean]): Seq[(Int, Double)] = {
    var correct = 0
    var incorrect = 0
    var points = List[(Int, Double)]()

    for (score <- scores) {
      if (score) correct = correct + 1
      else incorrect = incorrect + 1

      if (score) {
        points ::= (correct, precision(correct, incorrect))
      }
    }

    points.reverse
  }

  /** Compute precision yield points for each change in fst.
    *
    * Scored examples are presumed to be in sorted order by T, descending.
    *
    * @return  a triple of T, the yield, and the precision
    */
  def precisionYieldMeta[T](scores: Seq[(T, Boolean)]): Seq[(T, Int, Double)] = {
    if (scores.length == 0) List()
    else {
      var correct = 0
      var incorrect = 0
      var points = List[(T, Int, Double)]()
      var last = scores.head._1

      var i = 0
      for ((meta, score) <- scores) {
        if (score) correct = correct + 1
        else incorrect = incorrect + 1

        if (meta != last || i == scores.length - 1) {
          last = meta
          points ::= (meta, correct, precision(correct, incorrect))
        }

        i = i + 1
      }

      points.reverse
    }
  }

  /** Compute precision from counts of correct and incorrect examples.
    */
  def precision(correct: Int, incorrect: Int): Double =
    (correct.toDouble) / (correct + incorrect).toDouble

  /** Compute precision from a series of evaluations.
    */
  def precision(scores: Seq[Boolean]): Double = {
    val correct = scores.count(_ == true)
    precision(correct, scores.size - correct)
  }
}
