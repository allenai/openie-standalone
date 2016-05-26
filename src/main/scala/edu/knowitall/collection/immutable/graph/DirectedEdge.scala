package edu.knowitall.collection.immutable.graph

import edu.knowitall.collection.immutable.graph.Graph.Edge

import Graph.Edge

/** represents a direction in a graph */
sealed trait Direction {
  // extend Object
  override def toString = name

  def name: String
  /* return the opposite direction */
  def flip: Direction
}
object Direction {
  case object Up extends Direction {
    override def name = "Up"
    override def flip = Down
  }
  case object Down extends Direction {
    override def name = "Down"
    override def flip = Up
  }
}

/** an edge with a direction.  This is useful for representing paths
  * that go up edges as well as down edges.  It is also useful for
  * considering all edges from a vertex at once but still having
  * the information of whether the edges go up or down.
  */
sealed abstract class DirectedEdge[T](val edge: Edge[T]) {
  require(edge != null)

  /* the direction across this edge */
  def dir: Direction
  /* the start vertex when traversing the edge in the `dir` direction */
  def start: T
  /* the end vertex when traversing the edge in the `dir` direction */
  def end: T

  def switchStart(newStart: T): DirectedEdge[T]
  def switchEnd(newEnd: T): DirectedEdge[T]

  /* return a `DirectedEdge` with this `edge` but the opposite direction */
  def flip: DirectedEdge[T]

  // extend Object
  override def toString() = edge.toString
  def canEqual(that: Any): Boolean
  override def equals(that: Any) = that match {
    case that: DirectedEdge[_] => (that canEqual this) && that.edge == this.edge
    case _ => false
  }
}

/** an edge that is traversed in the `Up` direction.  In other words,
  * starting at the edge's dest and moving to the edge's source.
  */
case class UpEdge[T](override val edge: Edge[T]) extends DirectedEdge[T](edge) {
  def start = edge.dest
  def end = edge.source
  def dir = Direction.Up
  def switchStart(newStart: T) =
    new UpEdge(new Edge[T](edge.source, newStart, edge.label))
  def switchEnd(newEnd: T) =
    new UpEdge(new Edge[T](newEnd, edge.dest, edge.label))
  def flip = new DownEdge[T](edge)

  // extend Object
  override def toString() = "Up(" + super.toString + ")"
  override def canEqual(that: Any) = that.isInstanceOf[UpEdge[_]]
  override def hashCode() = (edge.hashCode + 2) * 37
}

/** an edge that is traversed in the `Down` direction.  In other words,
  * starting at the edge's source and moving to the edge's dest.
  */
case class DownEdge[T](override val edge: Edge[T]) extends DirectedEdge[T](edge) {
  def start = edge.source
  def end = edge.dest
  def dir = Direction.Down
  def switchStart(newStart: T) =
    new DownEdge(edge.copy(source = newStart))
  def switchEnd(newEnd: T) =
    new DownEdge(edge.copy(dest = newEnd))
  def flip = new UpEdge[T](edge)

  // extend Object
  override def toString() = "Down(" + super.toString + ")"
  override def canEqual(that: Any) = that.isInstanceOf[DownEdge[_]]
  override def hashCode() = (edge.hashCode + 1) * 37
}
