package edu.knowitall
package tool
package parse
package graph

import scala.Option.option2Iterable
import scala.collection.immutable
import scala.util.{ Try, Success, Failure }

import org.slf4j.LoggerFactory

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.collection.immutable.graph.Direction
import edu.knowitall.collection.immutable.graph.DownEdge
import edu.knowitall.collection.immutable.graph.Graph
import edu.knowitall.collection.immutable.graph.Graph.Edge
import edu.knowitall.collection.immutable.graph.UpEdge
import edu.knowitall.tool.Format
import edu.knowitall.tool.tokenize.Tokenizer
import edu.knowitall.tool.stem.Stemmer
import edu.knowitall.tool.postag.Postagger

/** A representation of a graph over dependencies.
  * This richer representation may include the text of the original sentence,
  * the original nodes (before collapsing), and the original dependencies.
  */
class DependencyGraph(
    /** the `DependencyNode`s from the parser */
    val nodes: immutable.SortedSet[DependencyNode],
    /** the `Dependency`s from the parser */
    val dependencies: immutable.SortedSet[Dependency],
    /** a graph representation dependencies */
    val graph: Graph[DependencyNode]
) {

  require(nodes != null)
  require(dependencies != null)
  require(graph != null)

  // check that the nodes match the nodes in the dependencies
  /*
  for (vertex <- graph.vertices) {
    nodes.find(node => vertex.indices == node.indices).map(_.text) match {
      case None => if (vertex.indices.length == 1) throw new IllegalArgumentException("no node at index: " + vertex.indices + " (" + vertex + ")")
      case Some(v) => require(v == vertex.text, "text at index " + vertex.indices + " does not match: " + vertex.text + " != " + v)
    }
  }
  */

  // constructors
  def this(
    nodes: immutable.SortedSet[DependencyNode],
    dependencies: immutable.SortedSet[Dependency]
  ) =
    this(
      immutable.SortedSet[DependencyNode]() ++ nodes,
      immutable.SortedSet[Dependency]() ++ dependencies,
      new Graph[DependencyNode](dependencies.flatMap(dep => Set(dep.source, dep.dest)).toSet, dependencies)
    )

  def this(
    nodes: Iterable[DependencyNode],
    dependencies: Iterable[Dependency]
  ) =
    this(
      immutable.SortedSet[DependencyNode]() ++ nodes,
      immutable.SortedSet[Dependency]() ++ dependencies
    )

  /** the text of the source sentence */
  val text = Tokenizer.originalText(this.nodes)

  def canEqual(that: Any) = that.isInstanceOf[DependencyGraph]
  override final def equals(that: Any) = that match {
    case that: DependencyGraph => that.canEqual(this) &&
      this.text == that.text &&
      this.nodes == that.nodes &&
      this.dependencies == that.dependencies &&
      this.graph == that.graph
    case _ => false
  }
  override def toString = DependencyGraph.stringFormat.write(this)

  def interval = Interval.open(0, nodes.size)

  def length = nodes.size

  @deprecated("Use StringFormat instead.", "2.4.5")
  def serialize = {
    DependencyGraph.stringFormat.write(this)
  }

  def serializeSeq = {
    val extra = this.nodes filterNot (this.dependencies.flatMap(dep => Set(dep.source, dep.dest)).contains(_))

    val pickledNodes = (extra.iterator).map(node => "(" + DependencyNode.stringFormat.write(node) + ")")
    val pickledDeps = (this.dependencies.iterator).map(Dependency.stringFormat.write)

    pickledNodes ++ pickledDeps
  }

  def map(f: DependencyNode => DependencyNode) = {
    val nodes = this.nodes.map(f)
    val graph = this.graph.map(f)
    val deps = this.dependencies.map(_.mapNodes(f))

    new DependencyGraph(nodes, deps, graph)
  }

  def mapGraph(f: Graph[DependencyNode] => Graph[DependencyNode]) = {
    new DependencyGraph(this.nodes, this.dependencies, f(this.graph))
  }

  def collapse = {
    def edgifyPrepositions(graph: Graph[DependencyNode]): Graph[DependencyNode] = {
      var g = graph

      // rename prep edges
      g = new Graph[DependencyNode](g.vertices, g.edges.map { e =>
        e.label match {
          case "prep" | "prepc" =>
            val qualifier = if (graph.dedges(e.dest) exists { case DownEdge(e) => e.label == "pcomp" case _ => false }) "c" else ""
            e.copy(label = e.label + qualifier + "_" + e.dest.string.toLowerCase.replaceAll(" ", "_"))
          case _ => e
        }
      })

      // NOTE: conjunctions must be distributed before pobj edges
      // are collapsed.  Otherwise some won't have incoming prep
      // edges to their targets yet.

      // collapse edges (pobj) preceeded by prep
      try {
        g = g.collapse { edge =>
          edge.label == "pobj" && (g.incoming(edge.source) exists (_.label startsWith "prep"))
        }((nodes: Traversable[DependencyNode]) =>
          nodes.find(n => g.edges(n).exists(e => e.label == "pobj" && e.dest == n)).get)
      } catch {
        case e: Throwable => DependencyGraph.logger.error("could not collapse pobj.", e)
      }

      // collapse edges (pcomp) preceeded by prep
      try {
        g = g.collapse { edge =>
          edge.label == "pcomp" && (g.incoming(edge.source) exists (_.label startsWith "prep"))
        }((nodes: Traversable[DependencyNode]) => {
          nodes.find(n => g.edges(n).exists(e => e.label == "pcomp" && e.dest == n)).get
        })
      } catch {
        case e: Throwable => DependencyGraph.logger.error("could not collapse pcomp.", e)
      }

      g
    }

    def collapseMultiwordPrepositions(graph: Graph[DependencyNode]): Graph[DependencyNode] = {
      val preps = graph.edges.filter(edge => edge.label == "prep" || edge.label == "pcomp").toList.sortBy(_.dest.indices)(Ordering[Interval].reverse)

      // follow up prep, advmod, dep, amod edges
      def cond(e: Graph.Edge[DependencyNode]) = e.label == "prep" || e.label == "advmod" || e.label == "dep" || e.label == "amod"

      preps.foldLeft(graph) {
        case (graph, prep) =>
          if (!(graph.edges contains prep)) graph else {
            val last = prep.dest
            val predecessors = graph.vertices.filter(_ <= last).toList.sortBy(_.indices)(Ordering[Interval].reverse)

            DependencyGraph.reversedSplitMultiwordPrepositions.filter(p => predecessors.map(_.text).startsWith(p)).toSeq match {
              case Seq() => graph
              case mtches =>
                val removeVertices = predecessors.take(mtches.maxBy(_.length).length).drop(1).flatMap(graph.inferiors(_, _.dest != last)).toSet.toList.sorted
                val joinVertices = removeVertices :+ last

                // keep last connected in case we remove some
                // of it's parents
                var parent = last
                while ((joinVertices contains parent) && (graph.indegree(parent) == 1)) {
                  parent = graph.incoming(parent).head.source
                }

                if (joinVertices contains parent) {
                  // we removed parents up to the root--abort
                  graph
                } else {
                  // add an edge from the closest remaining parent
                  // to last, if we need to
                  val extraEdges =
                    if (graph.neighbors(last) contains parent) {
                      Nil
                    }
                    else {
                      List(new Graph.Edge[DependencyNode](parent, last, "prep"))
                    }

                  val text = joinVertices.iterator.map(_.text).mkString(" ")
                  new Graph[DependencyNode](
                    extraEdges ++ graph.edges.filterNot(_.vertices exists (removeVertices contains _))
                  ).map(vertex =>
                    if (vertex == prep.dest) {
                      new DependencyNode(text, prep.dest.postag, Interval.span(joinVertices.map(_.indices)), joinVertices.head.offset)
                    }
                    else {
                      vertex
                    }
                  )
                }
            }
          }
      }
    }

    def collapseJunctions(graph: Graph[DependencyNode]) = {
      val conjGraph = graph.edges.filter(edge =>
        // conj edges to a node with no children
        edge.label == "conj" &&
          // source of conj edges has a child cc edge
          graph.dedges(edge.source).exists { case DownEdge(e) => e.label == "cc" case _ => false }).foldLeft(graph) {
        case (graph, conj) =>
          val ccNodes = graph.dedges(conj.source).filter {
            case DownEdge(e) => e.label == "cc"
            case _ => false
          }.iterator.map(_.edge.dest).toList

          // look left (negative distance) and then right.
          val bestCC = ccNodes.minBy {
            case cc =>
              val dist = cc.indices distance conj.dest.indices
              if (dist < 0) {
                -ccNodes.length - dist
              }
              else {
                dist
              }
          }

          val newEdges = scala.collection.Set[Edge[DependencyNode]]() ++ graph.edges - conj + conj.copy(label = "conj_" + bestCC.text)

          new Graph[DependencyNode](graph.vertices, newEdges)
      }

      new Graph[DependencyNode](conjGraph.edges filterNot (_.label == "cc"))
    }

    /** Distribute some edges to other nodes connected by conj_and.
      *
      * Incoming/outgoing are defined as a direction relative to the
      * connected component joined by the conjunction.
      *
      * 1.  Distribute nsubj.
      *     a.  "Michael, Rob, and NJ went to Than Vi."
      *     b.  "The apple was crisp and fresh."
      * 2.  Distribute nsubjpass.
      *     a.  incoming: "The bullet and gunpowder was loaded and fired."
      *     b.  outgoing: "The bullet was loaded and fired."
      * 3.  Distribute incoming advmod edges
      *     a.  incoming: "He spoke wisely and surely."
      *     b.  outgoing: "Just write them down and I will edit it for you."
      * 4.  Distribute incoming acomp edges
      *     a.  incoming: "The water looked blue and refreshing.
      * 5.  Distribute incoming amod edges
      *     a.  incoming: "The blue and cool water felt nice."
      *     b.  outgoing: "Pills raise clotting , high blood pressure , heart attack , and stroke . "
      * 6.  Distribute incoming dobj edges
      *     a.  incoming: "Michael found rocks and spiders."
      *     b.  outgoing: "Michael went to the beach and found rocks."
      * 7.  Distribute incoming rcmod edges
      *     a.  incoming: "The woman, who wore a black dress and spoke in the theater, ate cheese."
      *     b.  outgoing:
      * 8.  Distribute incoming ccomp edges
      *     a.  incoming: "He says you swim fast and eat cherries."
      * 9.  Distribute incoming xcomp edges
      *     a.  incoming: "He says you like to swim fast and eat cherries."
      * 10. Distribute incoming prep edges
      *     a.  incoming: "Michael and George went to the beach in Spring and Fall."
      *     b.  outgoing: "Michael and George went to the beach and slept."
      */
    def distributeConjunctions(graph: Graph[DependencyNode]) = {
      // find components connected by conj_and
      val components = graph.components(e => (e.label equalsIgnoreCase "conj_and") || e.label == "conj_&")

      val newEdges = components.flatMap { vertices =>
        val dedges = vertices.flatMap(graph.dedges(_))

        // find new edges needed to distribute conjunction
        for (
          dedge <- dedges;
          if (dedge.edge.label == "nsubj" ||
            dedge.edge.label == "nsubjpass" ||
            dedge.dir == Direction.Down && (
              // distribute "to" in: "I want to swim and eat cherries"
              dedge.edge.label == "aux"
            ) ||
              dedge.dir == Direction.Up && (
                dedge.edge.label == "advmod" ||
                dedge.edge.label == "amod" ||
                dedge.edge.label == "acomp" ||
                dedge.edge.label == "dobj" ||
                dedge.edge.label == "rcmod" ||
                dedge.edge.label == "ccomp" ||
                dedge.edge.label == "xcomp" ||
                (dedge.edge.label startsWith "prep")
              ));
          if !(vertices contains dedge.end);
          v <- vertices;
          newEdge = dedge match {
            case DownEdge(e) => e.copy(source = v)
            case UpEdge(e) => e.copy(dest = v)
          };
          if !(newEdge.source == newEdge.dest);
          if !(graph.edges contains newEdge)
        ) yield (newEdge)
      }

      new Graph[DependencyNode](graph.vertices, graph.edges ++ newEdges)
    }

    new DependencyGraph(this.nodes, this.dependencies,
      edgifyPrepositions(distributeConjunctions(collapseJunctions(collapseMultiwordPrepositions(this.graph)))))
  }

  def collapseXNsubj =
    new DependencyGraph(this.nodes, this.dependencies,
      new Graph[DependencyNode](graph.edges.map { dep =>
        if (dep.label.equals("xsubj") || dep.label.equals("nsubj")) {
          new Dependency(dep.source, dep.dest, "subj")
        }
        else {
          dep
        }
      }))

  def collapseNNPOf = {
    def pred(edge: Edge[DependencyNode]) = (edge.source.indices distance edge.dest.indices) == 2 &&
      edge.label.equals("prep_of") && edge.source.postag == "NNP" && edge.dest.postag == "NNP"
    def merge(nodes: Traversable[DependencyNode]) = {
      if (nodes.isEmpty) throw new IllegalArgumentException("argument nodes empty")
      val sorted = nodes.toList.sorted.view
      sorted.sliding(2).foreach(l => require((l.head.indices distance l.last.indices) == 2, "two nodes to merge don't have a distance of 2 (distance is " + (l.head.indices distance l.last.indices) + "): " + l.mkString(", ")))
      new DependencyNode(
        sorted.map(_.text).mkString(" of "),
        if (nodes.forall(_.postag.equals(nodes.head.postag))) {
          nodes.head.postag
        }
        else {
          sorted.map(_.postag).mkString(" of ")
        }, Interval.span(sorted.map(_.indices)), sorted.iterator.map(_.offset).min
      )
    }

    new DependencyGraph(this.nodes, this.dependencies, graph.collapse(pred(_))(merge))
  }

  /** Find components that are connected by the predicate.
    * Then, split components into subcomponents in which
    * all vertices correspond to adjacent words in the
    * source sentence.
    */
  def adjacentComponents(pred: Edge[DependencyNode] => Boolean): Set[Set[DependencyNode]] = {
    def splitByAdjacency(nodes: List[DependencyNode]): List[List[DependencyNode]] = {
      def rec(nodes: List[DependencyNode], result: List[DependencyNode]): List[List[DependencyNode]] = nodes match {
        case x :: Nil => (x :: result) :: Nil
        case x :: y :: xs => if (x.indices borders y.indices) rec(y :: xs, x :: result) else (x :: result) :: rec(y :: xs, Nil)
        case Nil => Nil
      }

      rec(nodes, Nil)
    }

    val groups: Set[Set[DependencyNode]] = (for (dep <- graph.edges; if pred(dep)) yield {
      graph.connected(dep.source, dedge => pred(dedge.edge)).toSet
    })(scala.collection.breakOut)

    (for {
      // for each connect nn component
      group <- groups
      // split the component by POS tag
      nodes = group.toList.sorted
      part <- splitByAdjacency(nodes)
      if part.size > 1
    } yield (part.toSet))(scala.collection.breakOut)
  }

  def collapseAdjacentGroups(pred: Edge[DependencyNode] => Boolean)(implicit merge: Traversable[DependencyNode] => DependencyNode) = {
    val components = adjacentComponents(edge => pred(edge))
    val graph = this.graph.collapseGroups(components)(merge)
    new DependencyGraph(this.nodes, this.dependencies, graph)
  }

  def collapseNounGroups(dividors: List[String] = List.empty) = {
    val lowerCaseDividors = dividors.map(_.toLowerCase)

    def pred(edge: Edge[DependencyNode]) = edge.label == "nn"
    val groups = adjacentComponents(pred)

    def splitByDividor(nodes: List[DependencyNode]): List[List[DependencyNode]] = nodes match {
      case x :: xs if lowerCaseDividors.contains(x.text.toLowerCase) => List(x) :: splitByDividor(xs)
      case x :: xs =>
        val (part, rest) = nodes.span(node => !lowerCaseDividors.contains(node.text.toLowerCase))
        part :: splitByDividor(rest)
      case Nil => Nil
    }

    // segment ordered dependency nodes by POS tag
    def postagEqual(a: String, b: String) = a == b || a.startsWith("NNP") && b.startsWith("NNP")
    def splitByPos(nodes: List[DependencyNode]): List[List[DependencyNode]] = nodes match {
      case x :: xs => nodes.takeWhile(node => postagEqual(node.postag, x.postag)) ::
        splitByPos(nodes.dropWhile(node => postagEqual(node.postag, x.postag)))
      case Nil => Nil
    }

    val groupsToCollapse: Set[Set[DependencyNode]] = (for {
      // for each connect nn component
      group <- groups
      // split the component by POS tag
      nodes = group.toList.sorted
      dividorSplit <- splitByDividor(nodes)
      part <- splitByPos(dividorSplit)
      if part.size > 1
    } yield (part.toSet))(scala.collection.breakOut)

    new DependencyGraph(this.nodes, this.dependencies, graph.collapseGroups(groupsToCollapse))
  }

  def directedAdjacentCollapse(labels: Set[String]): DependencyGraph = {
    def pred(edge: Edge[DependencyNode]) = labels.contains(edge.label)

    // If we get a component that is not connected, remove it from consideration.
    // It is often a mistake due to a strange parse.  It may also be an unusual edge.
    val components = adjacentComponents(pred) filter (this.graph.areConnected)
    val graph = this.graph.collapseGroups(components)(DependencyNode.directedMerge(this.graph))
    new DependencyGraph(this.nodes, this.dependencies, graph)
  }

  def directedAdjacentCollapse(label: String): DependencyGraph = directedAdjacentCollapse(Set(label))

  def collapseWeakLeaves =
    directedAdjacentCollapse(Set("neg", "det", "aux", "amod", "num", "quantmod", "advmod"))

  def normalize = collapseNounGroups().collapseNNPOf.simplifyPostags.collapseWeakLeaves

  def mapPostags(f: String => String): DependencyGraph = {
    def mapNode(node: DependencyNode): DependencyNode =
      new DependencyNode(node.text, f(node.postag), node.indices, node.offset)

    new DependencyGraph(
      this.nodes.map(mapNode),
      this.dependencies.map(_.mapNodes(mapNode)), graph.map(mapNode)
    )
  }

  def simplifyPostags = {
    def simplifyPostag(postag: String) = postag match {
      // obvious winners
      case "JJS" => "JJ"
      case "NNS" => "NN"
      case "NNPS" => "NNP"
      // others can stay the same
      case x => x
    }

    mapPostags(simplifyPostag)
  }

  def simplifyVBPostags = {
    def simplifyPostag(postag: String) = postag match {
      /* VB - Verb, base form
         VBD - Verb, past tense
         VBG - Verb, gerund or present participle
         VBN - Verb, past participle
         VBP - Verb, non-3rd person singular present
         VBZ - Verb, 3rd person singular present */
      case "VB" | "VBD" | "VBG" | "VBN" | "VBP" | "VBZ" => "VERB"
      // others can stay the same
      case x => x
    }

    mapPostags(simplifyPostag)
  }

  def dot(title: String = this.text): String = {
    val buffer = new StringBuffer(4092)
    printDot(buffer, title)
    buffer.toString
  }

  def dotWithHighlights(title: String, specialNodes: Set[DependencyNode], specialEdges: Set[Edge[DependencyNode]]): String = {
    val buffer = new StringBuffer(4092)
    printDotWithHighlights(buffer, title, specialNodes, specialEdges)
    buffer.toString
  }

  def dot(
    title: String,
    nodeStyle: Map[DependencyNode, String],
    edgeStyle: Map[Edge[DependencyNode], String]
  ): String = {
    val buffer = new StringBuffer(4092)
    printDot(buffer, title, nodeStyle, edgeStyle)
    buffer.toString
  }

  def printDot(writer: java.lang.Appendable, title: String = this.text) {
    printDot(writer, title, Map.empty, Map.empty)
  }

  def printDotWithHighlights(writer: java.lang.Appendable, title: String, specialNodes: Set[DependencyNode], specialEdges: Set[Edge[DependencyNode]]) {
    val filledNodes = specialNodes zip Stream.continually("style=filled,fillcolor=lightgray")

    val nodeStyle = filledNodes
    val edgeStyle = (specialEdges zip Stream.continually("style=filled")) ++
      ((this.graph.edges -- specialEdges) zip Stream.continually("style=dotted,color=gray"))

    printDot(writer, title, nodeStyle.toMap, edgeStyle.toMap)
  }

  def printDot(writer: java.lang.Appendable, title: String, nodeStyle: Map[DependencyNode, String], edgeStyle: Map[Edge[DependencyNode], String]) {
    def quote(string: String) = "\"" + string + "\""
    def escape(string: String) = string.replaceAll("\"", "''")
    def nodeString(node: DependencyNode) = {
      val text = escape(node.text)
      val postag = escape(node.postag)
      if (graph.vertices.filter(_.text.equals(text)).size > 1) {
        text + "_" + postag + "_" + node.indices.mkString("_")
      } else {
        text + "_" + postag
      }
    }

    val indent = " " * 2;

    writer.append("digraph g {\n")

    writer.append(indent + "graph [\n")
    writer.append(indent * 2 + "fontname=\"Helvetica-Oblique\"\n")
    writer.append(indent * 2 + "fontsize=\"12\"\n")
    val cleanedTitle = title.replaceAll("\\n", "").replaceAll("\"", "'").replaceAll(";", ",")
    writer.append(indent * 2 + "label=\"" + cleanedTitle + "\"\n")
    writer.append(indent + "]\n\n")

    for (node <- this.graph.vertices.toSeq.sorted) {
      var parts: List[String] = List()
      if (nodeStyle contains node) {
        parts ::= nodeStyle(node)
      }

      val brackets = "[" + parts.mkString(",") + "]"
      writer.append(indent + quote(nodeString(node)) + " " + brackets + "\n")
    }
    writer.append("\n")

    for (node <- nodeStyle.keys.toSeq.sorted) {
      writer.append(indent + quote(nodeString(node)) + " [" + nodeStyle(node) + "]\n")
    }

    writer.append("\n")
    for (dep <- graph.edges.toSeq.sortBy(edge => (edge.source.indices.head, edge.dest.indices.head, edge.label))) {
      val color = dep.label match {
        case "neg" => Some("red")
        case "amod" | "advmod" => Some("lightblue")
        case "det" | "punct" => Some("lightgrey")
        case "aux" => Some("grey")
        case x if x startsWith "prep" => Some("blue")
        case _ => None
      }

      var parts = List("label=\"" + dep.label + "\"")
      if (color.isDefined) parts ::= "color=\"" + color.get + "\""
      if (edgeStyle.contains(dep)) parts ::= edgeStyle(dep)

      val brackets = "[" + parts.mkString(",") + "]"
      writer.append(indent + quote(nodeString(dep.source)) + " -> " + quote(nodeString(dep.dest)) + " " + brackets + "\n")
    }
    writer.append("}")
  }

  def printDependencies() {
    graph.outgoing.keys.foreach { key =>
      println(key + ": " + graph.outgoing(key).map(edge => edge.label + "(" + edge.dest + ")").mkString(", "))
    }
  }

  override final def hashCode(): Int = {
    val state = Seq(text, nodes, dependencies, graph)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object DependencyGraph {
  val logger = LoggerFactory.getLogger(classOf[DependencyGraph])

  class StringFormat(seperator: String) extends Format[DependencyGraph, String] {
    def write(graph: DependencyGraph) = {
      graph.serializeSeq.mkString(seperator)
    }

    val nodeRegex = "\\s*\\((.*)\\)\\s*".r
    def read(pickled: String) = {
      val skipSeperator = ("(?m)\\s*" + seperator + "\\s*")
      val parts = pickled.split(skipSeperator)

      var nodes: Seq[DependencyNode] = Vector.empty[DependencyNode]
      var deps: Seq[Dependency] = Vector.empty[Dependency]
      for (part <- parts) {
        try {
          part match {
            // it's a loose dependency node
            case nodeRegex(node) => nodes :+= DependencyNode.stringFormat.read(node)
            // it's a regular dependency
            case _ => deps :+= Dependency.stringFormat.read(part)
          }
        } catch {
          case e: Exception => throw new SerializationException(s"Could not deserialize `$part` from graph: $pickled", e)
        }
      }

      val depNodes = deps.flatMap(dep => List(dep.source, dep.dest)).toSet
      new DependencyGraph(nodes ++ depNodes, deps)
    }
  }
  object stringFormat extends StringFormat("; ")
  object multilineStringFormat extends StringFormat("\n")

  @deprecated("Use StringFormat instead.", "2.4.5")
  def deserialize(string: String) = {
    stringFormat.read(string)
  }

  class ConllFormat(implicit lemmatizer: Stemmer) extends Format[DependencyGraph, String] {
    def write(graph: DependencyGraph): String = {
      graph.nodes.toSeq.zipWithIndex.map {
        case (node, index) =>
          val deps = graph.dependencies.filter(_.dest == node)
          require(deps.size <= 1, "multiple dependencies from node: " + node)
          val (destIndex, label) = deps.headOption match {
            case Some(dep) =>
              require(dep.source.indices.size == 1, "destination is multiple indices: " + dep)
              (dep.source.indices.head + 1, dep.label)
            case None => (0, "root")
          }

          val cols = Iterable(
            index + 1,
            node.text,
            node.lemma,
            node.postag,
            "_",
            destIndex,
            label
          )

          cols mkString "\t"
      }.mkString("\n")
    }

    // WARNING: this won't restore the actual sentence text because
    // there is no offset information stored in CONLL format.
    def read(iterator: Iterator[String]): DependencyGraph = {
      val section = iterator.takeWhile(!_.trim.isEmpty).toIndexedSeq

      var offset = 0
      val nodes = section.map { line =>
        val Array(index, string, lemma, postag, _, _, _) = line.split("\t")
        val node = new DependencyNode(string, postag, index.toInt - 1, offset)
        offset += string.length + 1

        node
      }

      val deps = section.flatMap { line =>
        val Array(index, string, lemma, postag, _, sourceIndex, edge) = line.split("\t")
        if (sourceIndex.toInt > 0) {
          Some(new Dependency(nodes(sourceIndex.toInt - 1), nodes(index.toInt - 1), edge))
        } else {
          None
        }
      }

      new DependencyGraph(nodes, deps)
    }

    def read(string: String): DependencyGraph = {
      this.read(string.split("\n").iterator)
    }
  }

  class SerializationException(message: String, cause: Throwable)
    extends RuntimeException(message, cause)

  val reversedSplitMultiwordPrepositions = Postagger.complexPrepositions.map(_.split(" ").toList.reverse)
}
