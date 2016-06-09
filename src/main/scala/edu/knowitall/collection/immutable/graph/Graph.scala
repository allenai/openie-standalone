package edu.knowitall.collection.immutable.graph

import scala.Option.option2Iterable
import scala.collection.{ mutable, immutable }

import edu.knowitall.collection.immutable.graph.Graph.Edge

/** A graph representation where data is stored in vertices and edges are
  * represented with adjacency lists.
  *
  * @author  Michael Schmitz
  */
class Graph[T](
    val vertices: Set[T],
    val edges: Set[Edge[T]]
) {
  require(vertices != null)
  require(edges != null)

  // shorthands
  type G = Graph[T]
  type E = Edge[T]

  // fields

  val outgoing = edges.groupBy(_.source).toMap.withDefaultValue(Set.empty)
  val incoming = edges.groupBy(_.dest).toMap.withDefaultValue(Set.empty)

  // constructors

  def this(edges: Set[Edge[T]]) =
    this(edges.flatMap(_.vertices), edges)

  def this(edges: Iterable[Edge[T]]) =
    this(edges.toSet)

  def this(vertices: Iterable[T], edges: Iterable[Edge[T]]) =
    this(vertices.toSet, edges.toSet)

  // extend Object
  override def toString = {
    val builder = new java.lang.StringBuilder
    this.print(builder)
    builder.toString
  }
  def canEqual(that: Any) = that.isInstanceOf[Graph[_]]
  override def equals(that: Any) = that match {
    case that: Graph[_] => (that canEqual this) &&
      that.vertices == this.vertices &&
      that.edges == this.edges
    case _ => false
  }

  /* Expand a set of verticess to all neighbors along immediate edges
   * that satisfy the supplied predicate.
   *
   * @param  vertices  the seed vertices
   * @param  pred  the predicate edges must match to be expanded upon
   * @return  the  the set of vertices in the expansion
   */
  def expand(vertices: Set[T], pred: DirectedEdge[T] => Boolean) = {
    vertices ++ vertices.flatMap { vertex => neighbors(vertex, pred) }
  }

  /* Iteratively expand the neighbors of a vertex to all
   * neighbors along an edge that satisfy the supplied predicate.
   *
   * @param  v  the seed vertex
   * @param  pred  the predicate edges must match to be expanded upon
   * @return  the set of vertices in the expansion
   */
  def connected(v: T, pred: DirectedEdge[T] => Boolean): Set[T] = {
    def rec(vertices: Set[T], last: Set[T]): Set[T] = {
      val neighbors: Set[T] = last.flatMap(this.neighbors(_, pred))
      if (neighbors.isEmpty) {
        vertices
      }
      else {
        rec(vertices ++ neighbors, neighbors -- vertices)
      }
    }

    rec(Set(v), Set(v))
  }

  /* create a new graph where a function is applied to all nodes. */
  def map[U](f: T => U) = new Graph(
    this.edges.map(edge => new Edge(f(edge.source), f(edge.dest), edge.label))
  )

  /* collapse all edges where the supplied predicate is true. */
  def collapse(collapsable: E => Boolean)(implicit merge: Traversable[T] => T): G = {
    // find nn edges
    val targetEdges = edges.filter(collapsable)

    // collapse edges by building a map from collapsed vertices
    // to collections of joined vertices
    var map: Map[T, Set[T]] = Map()
    for (edge <- targetEdges) {
      // dest is already collapsed
      if (map.contains(edge.dest)) {
        map += edge.dest -> (map(edge.dest) + edge.source)
        map += edge.source -> map(edge.dest)
      } // source is already collapsed
      else if (map.contains(edge.source)) {
        map += edge.source -> (map(edge.source) + edge.dest)
        map += edge.dest -> map(edge.source)
      } // neither is collapsed
      else {
        var set = Set.empty[T]
        set += edge.source
        set += edge.dest
        map += edge.dest -> set
        map += edge.source -> set
      }
    }

    collapseGroups(map.values)(merge)
  }

  /* collapse edges between all supplied vertices. */
  def collapse(set: Set[T])(implicit merge: Traversable[T] => T) = collapseGroups(Iterable(set))

  /* collapse edges between all vertices in a single set. */
  def collapseGroups(groups: Iterable[Set[T]])(implicit merge: Traversable[T] => T) = {
    // convert collapsed vertices to a single Vertex
    val transformed = groups.flatMap { vertices =>
      vertices.map { v => (v, merge(vertices)) }
    }.toMap

    // map other edges to the new vertices
    val newedges = edges.flatMap { edge =>
      val tsource = transformed.get(edge.source)
      val tdest = transformed.get(edge.dest)

      val source = tsource.getOrElse(edge.source)
      val dest = tdest.getOrElse(edge.dest)

      if (source == dest) {
        List()
      }
      else {
        List(new E(source, dest, edge.label))
      }
    }

    new Graph(newedges)
  }

  /* Find all connected components joined by the specified predicate. */
  def components(pred: Edge[T] => Boolean): Set[Set[T]] = {
    this.vertices.foldLeft(Set.empty[Set[T]]) {
      case (s, v) =>
        val nx = this.connected(v, dedge => pred(dedge.edge))
        if (nx.size > 1) {
          s + (nx + v)
        } else {
          s
        }
    }
  }

  /* all edges leaving or coming into this vertex. */
  def edges(vertex: T): Set[E] = outgoing(vertex) union incoming(vertex)

  /* all `DirectedEdge`s leaving or coming into this vertex. */
  def dedges(vertex: T): Set[DirectedEdge[T]] =
    outgoing(vertex).map(new DownEdge(_): DirectedEdge[T]).union(
      incoming(vertex).map(new UpEdge(_): DirectedEdge[T])
    ).toSet

  /** all vertices seperated from `v` by a single edge that
    * satisfied `pred`.
    */
  def neighbors(v: T, pred: DirectedEdge[T] => Boolean): Set[T] =
    dedges(v).withFilter(pred).map {
      _ match {
        case out: DownEdge[_] => out.end
        case in: UpEdge[_] => in.end
      }
    }

  /** all vertices seperated from `v`. */
  def neighbors(v: T): Set[T] = predecessors(v) union successors(v)

  /** all vertices before incoming edges to `v`. */
  def predecessors(v: T): Set[T] = incoming(v).map(edge => edge.source)

  /** all vertices before incoming edges to `v` that satisfy the supplied predicate. */
  def predecessors(v: T, pred: Edge[T] => Boolean): Set[T] =
    neighbors(v, (dedge: DirectedEdge[T]) => dedge.dir == Direction.Up && pred(dedge.edge))

  /** all vertices after outgoing edges to `v`. */
  def successors(v: T): Set[T] = outgoing(v).map(edge => edge.dest)

  /** all vertices after outgoing edges to `v` that satisfy the supplied predicate. */
  def successors(v: T, pred: Edge[T] => Boolean): Set[T] =
    neighbors(v, (dedge: DirectedEdge[T]) => dedge.dir == Direction.Down && pred(dedge.edge))

  /** Iteratively expand a vertex to all vertices beneath it.
    *
    * @param  vertex  the seed vertex
    * @return  the set of vertices beneath `vertex`
    */
  def inferiors(v: T, cond: E => Boolean = (x => true)): Set[T] = {
    def conditional(dedge: DirectedEdge[T]) = dedge match {
      case down: DownEdge[_] => cond(down.edge)
      case _: UpEdge[_] => false
    }
    connected(v, conditional)
  }

  /** Iteratively expand a vertex to all vertices above it.
    *
    * @param  vertex  the seed vertex
    * @return  the set of vertices beneath `vertex`
    */
  def superiors(v: T, cond: E => Boolean = (x => true)): Set[T] = {
    def conditional(dedge: DirectedEdge[T]) = dedge match {
      case up: UpEdge[_] => cond(up.edge)
      case _: DownEdge[_] => false
    }
    connected(v, conditional)
  }

  /* number of out-edges bordering v */
  def outdegree(v: T) = outgoing(v).size
  /* number of in-edges bordering v */
  def indegree(v: T) = incoming(v).size
  /* number of edges bordering v */
  def degree(v: T) = indegree(v) + outdegree(v)

  private def toBipath(vertices: List[T]) = {
    def toEdgeBipath(vertices: List[T]): List[List[DirectedEdge[T]]] = vertices match {
      case a :: b :: xs =>
        val out = outgoing(a)
        val in = incoming(a)
        val outedge = out.find(edge => edge.dest.equals(b)).map(edge => new DownEdge(edge))
        val inedge = in.find(edge => edge.source.equals(b)).map(edge => new UpEdge(edge))
        val edge = outedge.getOrElse(inedge.getOrElse(throw new IllegalArgumentException))
        (outedge ++ inedge).flatMap(edge => toEdgeBipath(b :: xs).map(path => edge :: path)).toList
      case _ => List(List())
    }
    toEdgeBipath(vertices).map(new Bipath(_))
  }

  def bipaths(start: T, end: T): List[Bipath[T]] = {
    val vertexPaths = vertexBipaths(start, end)
    vertexPaths.flatMap(np => toBipath(np))
  }

  def bipaths(vertices: Set[T], maxLength: Option[Int] = None): Set[Bipath[T]] = {
    val vertexPaths = vertexBipaths(vertices, maxLength)
    vertexPaths.flatMap(np => toBipath(np))
  }

  /** Find a path from vertex (start) to vertex (end).
    */
  def vertexBipaths(start: T, end: T): List[List[T]] = {
    def bipaths(start: T, path: List[T]): List[List[T]] = {
      if (start.equals(end)) {
        List(path)
      }
      else {
        neighbors(start).filter(nb => !path.contains(nb)).toList.flatMap(nb => bipaths(nb, nb :: path))
      }
    }

    bipaths(start, List(start)).map(_.reverse)
  }

  /** Find a path that contains all vertices in (vertices).
    */
  private def vertexBipaths(vertices: Set[T], maxLength: Option[Int] = None): Set[List[T]] = {
    def bipaths(start: T, path: List[T], length: Int): List[List[T]] = {
      if (maxLength.isDefined && length > maxLength.get) {
        List()
      }
      else if (vertices.forall(path.contains(_))) {
        List(path)
      }
      else {
        neighbors(start).filter(nb => !path.contains(nb)).toList.flatMap(nb => bipaths(nb, nb :: path, length + 1))
      }
    }

    vertices.flatMap(start => bipaths(start, List(start), 0).map(_.reverse))
  }

  /** Test if the two nodes border each other.
    */
  def areNeighbors(a: T, b: T): Boolean =
    this.neighbors(a).contains(b)

  /** Test if the nodes are connected.  In other words, for each node,
    * there exists another node in the set that is its neighbor.
    */
  def areConnected(vertices: Iterable[T]): Boolean =
    vertices.forall(v => vertices.exists(w => this.areNeighbors(v, w)))

  /** Find the node which is most superior.
    *
    * @throws  IllegalArgumentException  nodes are not connected or no one superior
    */
  def superior(vertices: Set[T]): T = {
    require(vertices.size > 0, "vertices must not be empty")
    require(this.areConnected(vertices), "vertices are not connected")

    // go down through the vertices to the bottom
    def sink(v: T): T = {
      val children = this.successors(v)
      val targets = children intersect vertices

      if (targets.size == 0) {
        v
      }
      else {
        sink(targets.head)
      }
    }

    // go back up, making sure there is only one option
    def climb(v: T): T = {
      val parents = this.predecessors(v)
      val targets = parents intersect vertices

      if (targets.size == 0) {
        v
      }
      else if (targets.size == 1) {
        climb(targets.head)
      }
      else {
        throw new IllegalArgumentException("there is no single superior")
      }
    }

    climb(sink(vertices.head))
  }

  def print(writer: java.lang.Appendable): Unit = {
    def print(vertex: T, indent: Int) {
      writer.append(" " * indent + vertex + "\n")
      outgoing(vertex).foreach { edge => print(edge.dest, indent + 2) }
    }

    val start = vertices.find(vertex => incoming(vertex).isEmpty).get
    print(start, 0)
  }

  def print(): Unit = print(System.out)
}

object Graph {
  case class Edge[T](
      val source: T,
      val dest: T,
      val label: String
  ) {
    def vertices = List(source, dest)
    override def toString = label + "(" + source + ", " + dest + ")"
  }
}
