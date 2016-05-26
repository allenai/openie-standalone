package edu.knowitall.collection.immutable.graph.pattern

import edu.knowitall.collection.immutable.graph.Bipath
import edu.knowitall.collection.immutable.graph.Graph
import edu.knowitall.collection.immutable.graph.DirectedEdge

/** A representation of a match of a pattern in a graph.
  *
  * @author  Michael Schmitz
  */
class Match[T](
    /** the pattern that was applied */
    val pattern: Pattern[T],
    /** the matched path through the graph */
    val bipath: Bipath[T],
    /** the pattern groups in the match */
    val nodeGroups: Map[String, Match.NodeGroup[T]],
    val edgeGroups: Map[String, Match.EdgeGroup[T]]
) {
  // extend Object
  override def toString = bipath.toString + ": " + nodeGroups.toString + " and " + edgeGroups.toString

  def groups: Map[String, Match.Group] = nodeGroups ++ edgeGroups

  def nodes: Iterable[T] = bipath.nodes
  def edges: Iterable[Graph.Edge[T]] = bipath.edges
}

object Match {
  sealed abstract class Group(val text: String)
  case class NodeGroup[T](node: T, matchText: String) extends Group(matchText)
  case class EdgeGroup[T](dedge: DirectedEdge[T], matchText: String) extends Group(matchText)
}
