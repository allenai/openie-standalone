package edu.knowitall
package tool
package parse
package graph

import edu.knowitall.collection.immutable.graph.Graph
import edu.knowitall.collection.immutable.graph.Graph._
import scala.collection.immutable.SortedSet
import edu.knowitall.collection.immutable.Interval
import tool.stem.{ Stemmer, IdentityStemmer }
import tool.postag.PostaggedToken

/** A representation for a node in the graph of dependencies.  A node
  * represents one or more adjacent tokens in the source sentence.
  */
class DependencyNode(string: String, postag: String, val tokenInterval: Interval, offset: Int) extends PostaggedToken(Symbol(postag), string, offset) with Ordered[DependencyNode] {
  require(string != null)
  require(postag != null)

  /* create a node with a single index */
  def this(text: String, postag: String, index: Int, offset: Int) =
    this(text, postag, Interval.singleton(index), offset)

  def this(postagged: PostaggedToken, tokenInterval: Interval) =
    this(postagged.string, postagged.postag, tokenInterval, postagged.offset)

  def text = string

  def index = {
    require(indices.size == 1, "node must span a single index")
    indices.head
  }
  def indices = tokenInterval

  // extend Ordered[DependencyNode]
  override def compare(that: DependencyNode) = {
    if (this == that) {
      0
    }
    else if (this.tokenInterval intersects that.tokenInterval) {
      implicitly[Ordering[Tuple3[String, String, Int]]].compare(
        (this.string, this.postag, this.offset),
        (that.string, that.postag, that.offset)
      )
    } else {
      this.tokenInterval.compare(that.tokenInterval)
    }
  }

  // extend Object
  override def toString() = toFullString
  def canEqual(that: Any) = that.isInstanceOf[DependencyNode]
  override def equals(that: Any) = that match {
    case that: DependencyNode => that.text.equals(this.text) &&
      that.postag.equals(this.postag) &&
      that.tokenInterval.equals(this.tokenInterval)
    case _ => false
  }
  override def hashCode() = this.text.hashCode * 37 + this.postag.hashCode * 37 + this.tokenInterval.hashCode

  private var plemma: String = null
  def lemma(implicit stemmer: Stemmer) = {
    if (plemma == null) {
      plemma = stemmer.lemmatize(text)
    }
    plemma
  }

  def toFullString = this.text + "_" + this.postag + "_" + this.tokenInterval.mkString("_")

  def lemmatize(stemmer: Stemmer) = new DependencyNode(stemmer.lemmatize(text), postag, tokenInterval, offset)

  @deprecated("Use StringFormat instead.", "2.4.5")
  def serialize = {
    DependencyNode.stringFormat.write(this)
  }
}

object DependencyNode {
  object stringFormat extends Format[DependencyNode, String] {
    def write(node: DependencyNode): String = {
      if (node.tokenInterval.length > 1) throw new IllegalStateException("cannot serialize node spanning multiple tokenInterval")
      def clean(string: String): String = string.replaceAll(";", ":")
      val cleanText = clean(node.text).replaceAll("[[_()][^\\p{Graph}]]", "")
      Iterator(cleanText, clean(node.postag), node.tokenInterval.start, node.offset).mkString("_")
    }

    def read(pickled: String): DependencyNode = {
      val Array(text, postag, index, offset) = try (pickled.split("_"))
      catch {
        case e: Throwable => throw new SerializationException("could not deserialize dependency node: " + pickled, e);
      }

      new DependencyNode(text, postag, index.toInt, offset.toInt)
    }
  }

  implicit def merge(nodes: Traversable[DependencyNode]) = {
    if (nodes.isEmpty) throw new IllegalArgumentException("argument nodes empty")
    val sorted = nodes.toList.sorted
    val text = sorted.map(_.text).mkString(" ")
    val postag =
      // if they all have the same postag, use that
      if (nodes.forall(_.postag.equals(nodes.head.postag))) {
        nodes.head.postag
      }
      // otherwise use the first postag
      else {
        sorted.map(_.postag).head
      }

    // union the intervals, or throw a more informative exception if they are
    // not adjacent
    val tokenInterval = try {
      Interval.union(sorted.map(_.tokenInterval))
    } catch {
      case e: Throwable =>
        throw new IllegalArgumentException("A set of non-adjacent intervals cannot be merged: " + e.toString)
    }

    new DependencyNode(text, postag, tokenInterval, sorted.iterator.map(_.offset).min)
  }

  /** Merge nodes, keeping the postag of the superior node of the set.
    *
    * @throws  IllegalArgumentException  there is no superior of the set
    * @return  the superior node of the set
    */
  implicit def directedMerge(graph: Graph[DependencyNode])(nodes: Traversable[DependencyNode]) = {
    if (nodes.isEmpty) throw new IllegalArgumentException("argument nodes empty")
    val sorted = nodes.toList.sorted
    val text = sorted.map(_.text).mkString(" ")
    val postag = graph.superior(nodes.toSet).postag

    // union the intervals, or throw a more informative exception if they are
    // not adjacent
    val tokenInterval = try {
      Interval.union(sorted.map(_.tokenInterval))
    } catch {
      case e: Throwable =>
        throw new IllegalArgumentException("A set of non-adjacent intervals cannot be merged: " + e.toString)
    }

    new DependencyNode(text, postag, tokenInterval, sorted.iterator.map(_.offset).min)
  }

  @deprecated("Use StringFormat instead.", "2.4.5")
  def deserialize(string: String) = {
    stringFormat.read(string)
  }

  class SerializationException(message: String, cause: Throwable)
    extends RuntimeException(message, cause)
}
