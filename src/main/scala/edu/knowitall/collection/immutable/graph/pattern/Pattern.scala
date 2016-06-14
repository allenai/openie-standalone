package edu.knowitall.collection.immutable.graph.pattern

import edu.knowitall.common.enrich.Iterables
import edu.knowitall.collection.immutable.graph.{ Graph, DirectedEdge, Bipath }

/** Represents a pattern with which graphs can be searched.
  * A pattern will start and end with a node matcher, and every
  * matcher (necessarily) alternates between a NodeMatcher and
  * and EdgeMatcher.
  *
  * @author  Michael Schmitz
  */
class Pattern[T](
    /** a list of matchers, alternating between `NodeMatcher`s and `EdgeMatcher`s. */
    val matchers: List[Matcher[T]]
) extends Function[Graph[T], List[Match[T]]] {

  require(matchers != null)

  // ensure that the matchers alternate
  matchers.iterator.zipWithIndex.foreach {
    case (m, i) =>
      (m, (i % 2)) match {
        case (m: NodeMatcher[_], 0) =>
        case (m: EdgeMatcher[_], 1) =>
        case _ => throw new IllegalArgumentException("matchers must start with a node matcher and alternate")
      }
  }

  def this(edgeMatchers: List[EdgeMatcher[T]], nodeMatchers: List[NodeMatcher[T]]) = {
    this(Iterables.interleave(nodeMatchers, edgeMatchers).toList)
  }

  // extend Object
  override def toString = toStringF(identity[String])
  def toStringF(f: String => String) = {
    matchers.iterator.map(_.toStringF(f)).mkString(" ")
  }
  def serialize = toString

  def canEqual(that: Any) = that.isInstanceOf[Pattern[_]]
  override def equals(that: Any) = that match {
    case that: Pattern[_] => (that canEqual this) && this.matchers == that.matchers
    case _ => false
  }
  override def hashCode = this.matchers.hashCode

  /** Find all matches of this pattern in the graph. */
  def apply(graph: Graph[T]): List[Match[T]] = {
    graph.vertices.flatMap(apply(graph, _))(scala.collection.breakOut)
  }

  def baseEdgeMatchers = {
    this.edgeMatchers.map(_.baseEdgeMatcher)
  }

  def baseNodeMatchers = {
    this.nodeMatchers.flatMap(_.baseNodeMatchers)
  }

  /** Find all matches of this pattern in the graph starting at `vertex`. */
  def apply(graph: Graph[T], vertex: T): List[Match[T]] = {
    def rec(
      matchers: List[Matcher[T]],
      vertex: T,
      edges: List[DirectedEdge[T]],
      nodeGroups: List[(String, Match.NodeGroup[T])],
      edgeGroups: List[(String, Match.EdgeGroup[T])]
    ): List[Match[T]] = matchers match {

      case (m: CaptureNodeMatcher[_]) :: xs =>
        m.matchText(vertex).map(text => rec(xs, vertex, edges, (m.alias, Match.NodeGroup(vertex, text)) :: nodeGroups, edgeGroups)).getOrElse(List())
      case (m: NodeMatcher[_]) :: xs if m.matches(vertex) =>
        if (m.matches(vertex)) {
          rec(xs, vertex, edges, nodeGroups, edgeGroups)
        }
        else {
          List()
        }
      case (m: EdgeMatcher[_]) :: xs =>
        // only consider edges that have not been used
        val uniqueEdges = graph.dedges(vertex) -- edges.iterator.flatMap(e => List(e, e.flip))
        // search for an edge that matches
        uniqueEdges.iterator.flatMap { edge =>
          m.matchText(edge).map(text => (edge, text))
        }.flatMap {
          case (dedge, matchText) =>
            val groups = m match {
              case m: CaptureEdgeMatcher[_] => (m.alias, Match.EdgeGroup(dedge, matchText)) :: edgeGroups
              case _ => edgeGroups
            }
            // we found one, so recurse
            rec(xs, dedge.end, dedge :: edges, nodeGroups, groups)
        }.toList
      case _ => List(new Match(this, new Bipath(edges.reverse), nodeGroups.toMap, edgeGroups.toMap))
    }

    rec(this.matchers, vertex, List(), List(), List())
  }

  /** A list of just the edge matchers, in order. */
  def edgeMatchers = matchers.collect { case m: EdgeMatcher[_] => m }
  /** A list of just the node matchers, in order. */
  def nodeMatchers = matchers.collect { case m: NodeMatcher[_] => m }

  def reflection = new Pattern(this.matchers.reverse.map {
    case m: EdgeMatcher[_] => m.flip
    case m: NodeMatcher[_] => m
  })
}
