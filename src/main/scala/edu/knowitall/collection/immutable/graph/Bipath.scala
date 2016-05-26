package edu.knowitall.collection.immutable.graph

import Graph._

/** A representation of a path through a graph.  The path is represented
  * by a list of directed edges.
  *
  * @author  Michael Schmitz
  */
class Bipath[T](val path: List[DirectedEdge[T]]) {
  require(path != null)

  // extend Object
  override def toString = "[" + path.mkString(", ") + "]";
  def canEqual(that: Any) = that.isInstanceOf[Bipath[_]]
  override def equals(that: Any) = that match {
    case that: Bipath[_] => (that canEqual this) && that.path == this.path
    case _ => false
  }

  /** the undirected edges of the path */
  def edges: Set[Edge[T]] = path.foldRight[Set[Edge[T]]](Set()) {
    case (item, set) => set + item.edge
  }

  /** the unique vertices along the path */
  def nodes: List[T] = path.head.start :: path.map(_.end)

  /** the first vertex in the path */
  def start: T = path.head.start

  /** collapse edges in the path that match `pred` */
  def collapse(pred: Edge[T] => Boolean, merge: (T, T) => T) = {
    if (path.forall(dep => pred(dep.edge))) {
      this
    } else {
      val array = path.toArray
      for (i <- array.indices) {
        val current = array(i)
        if (pred(current.edge)) {
          // TODO: sorted
          val merged = merge(current.start, current.end)
          if (current.isInstanceOf[UpEdge[_]]) {
            if (array.indices contains (i + 1)) {
              array(i + 1) = array(i + 1).switchStart(merged)
            }

            if (array.indices contains (i - 1)) {
              array(i - 1) = array(i - 1).switchEnd(merged)
            }
          } else if (current.isInstanceOf[DownEdge[_]]) {
            if (array.indices contains (i + 1)) {
              array(i + 1).switchStart(merged)
            }

            if (array.indices contains (i - 1)) {
              array(i - 1) = array(i - 1).switchEnd(merged)
            }
          }
        }
      }

      new Bipath(array.filter(dep => !pred(dep.edge)).toList)
    }
  }
}
