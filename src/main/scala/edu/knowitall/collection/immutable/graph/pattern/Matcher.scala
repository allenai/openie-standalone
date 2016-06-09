package edu.knowitall.collection.immutable.graph.pattern

import scala.Option.option2Iterable

import edu.knowitall.collection.immutable.graph.{ Graph, DirectedEdge }
import edu.knowitall.collection.immutable.graph.{ UpEdge, DownEdge, Direction }

/** Abstract superclass for all matchers.
  */
sealed abstract class Matcher[T] {
  override def toString = toStringF(identity[String])
  def toStringF(f: String => String): String
}

/** Trait to match dependency graph edges.
  */
sealed abstract class EdgeMatcher[T] extends Matcher[T] {
  def apply(edge: DirectedEdge[T]) = this.matchText(edge)

  def matches(edge: DirectedEdge[T]) = this.matchText(edge).isDefined
  def matchText(edge: DirectedEdge[T]): Option[String]

  def canMatch(edge: Graph.Edge[T]): Boolean = this.matches(new UpEdge(edge)) || this.matches(new DownEdge(edge))
  def flip: EdgeMatcher[T]

  def baseEdgeMatcher: BaseEdgeMatcher[T]
}

abstract class BaseEdgeMatcher[T] extends EdgeMatcher[T] {
  override def baseEdgeMatcher = this
}

abstract class WrappedEdgeMatcher[T](val matcher: EdgeMatcher[T]) extends EdgeMatcher[T] {
  def canEqual(that: Any) = that.isInstanceOf[WrappedEdgeMatcher[_]]
  override def equals(that: Any) = that match {
    case that: WrappedEdgeMatcher[_] => (that canEqual this) && this.matcher == that.matcher
    case _ => false
  }

  override def baseEdgeMatcher = matcher.baseEdgeMatcher

  override def hashCode(): Int = {
    val state = Seq(matcher)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

class DirectedEdgeMatcher[T](val direction: Direction, matcher: EdgeMatcher[T]) extends WrappedEdgeMatcher[T](matcher) {
  def matchText(edge: DirectedEdge[T]) =
    if (edge.dir == direction) {
      matcher.matchText(edge)
    }
    else {
      None
    }

  def flip: EdgeMatcher[T] = new DirectedEdgeMatcher(direction.flip, matcher)

  /** symbolic representation used in serialization. */
  def symbol = direction match {
    case Direction.Up => "<"
    case Direction.Down => ">"
  }

  // extend Object
  override def toStringF(f: String => String) = f(symbol + matcher.toStringF(f) + symbol)
  override def canEqual(that: Any) = that.isInstanceOf[DirectedEdgeMatcher[_]]
  override def equals(that: Any) = that match {
    case that: DirectedEdgeMatcher[_] => (that canEqual this) && this.direction == that.direction && super.equals(that)
    case _ => false
  }
  override def hashCode = direction.hashCode + 39 * matcher.hashCode
}

class TrivialEdgeMatcher[T] extends BaseEdgeMatcher[T] {
  override def toStringF(f: String => String) = f(".*")

  def matchText(edge: DirectedEdge[T]) = Some(edge.edge.label)
  def flip = this
}

class CaptureEdgeMatcher[T](val alias: String, matcher: EdgeMatcher[T]) extends WrappedEdgeMatcher[T](matcher) {
  override def matchText(edge: DirectedEdge[T]) = matcher.matchText(edge)
  override def flip = new CaptureEdgeMatcher(alias, matcher.flip)

  // extend Object
  override def toStringF(f: String => String) = f(matcher match {
    case _: TrivialEdgeMatcher[_] => "{" + alias + "}"
    case d: DirectedEdgeMatcher[_] => d.symbol + "{" + alias + ":" + d.matcher.toStringF(f) + "}" + d.symbol
    case m: EdgeMatcher[_] => "{" + alias + ":" + m.toStringF(f) + "}"
  })
  override def canEqual(that: Any) = that.isInstanceOf[CaptureEdgeMatcher[_]]
  override def equals(that: Any) = that match {
    case that: CaptureEdgeMatcher[_] => (that canEqual this) && this.alias == that.alias && super.equals(that)
    case _ => false
  }
  override def hashCode = alias.hashCode + 39 * matcher.hashCode
}

/** Trait to match dependency graph nodes.
  */
sealed abstract class NodeMatcher[T] extends Matcher[T] {
  def apply(node: T) = this.matchText(node)

  def matches(node: T) = this.matchText(node).isDefined
  def matchText(node: T): Option[String]

  def baseNodeMatchers: Seq[BaseNodeMatcher[T]]
}

abstract class BaseNodeMatcher[T] extends NodeMatcher[T] {
  override def baseNodeMatchers = Seq(this)
}

abstract class WrappedNodeMatcher[T](val matcher: NodeMatcher[T])
    extends NodeMatcher[T] {
  def canEqual(that: Any) = that.isInstanceOf[WrappedNodeMatcher[_]]
  override def equals(that: Any) = that match {
    case that: WrappedNodeMatcher[_] => (that canEqual this) && this.matcher == that.matcher
    case _ => false
  }

  override def baseNodeMatchers = this.matcher.baseNodeMatchers

  override def hashCode(): Int = {
    val state = Seq(matcher)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

class ConjunctiveNodeMatcher[T](val matchers: Set[NodeMatcher[T]])
    extends NodeMatcher[T] {
  require(matchers.size > 1)
  require(!matchers.exists(_.isInstanceOf[ConjunctiveNodeMatcher[_]]))

  def this(m: NodeMatcher[T]) = this(Set(m))
  def this(m: NodeMatcher[T]*) = this(Set() ++ m)

  override def matchText(node: T) =
    if (matches(node)) {
      matchers.flatMap(_.matchText(node)).headOption
    }
    else {
      None
    }

  override def matches(node: T) = matchers.forall(_.matches(node))

  override def baseNodeMatchers = this.matchers.toSeq.flatMap(_.baseNodeMatchers)

  override def toStringF(f: String => String) = f(matchers.iterator.map(_.toStringF(f)).mkString(":"))
  def canEqual(that: Any) = that.isInstanceOf[ConjunctiveNodeMatcher[_]]
  override def equals(that: Any) = that match {
    case that: ConjunctiveNodeMatcher[_] => (that canEqual this) && this.matchers == that.matchers
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(matchers)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

/** Always match any node.
  */
class TrivialNodeMatcher[T] extends BaseNodeMatcher[T] {
  override def matchText(node: T) = Some(".*")

  // extend Object
  override def toStringF(f: String => String) = f(".*")
  def canEqual(that: Any) = that.isInstanceOf[TrivialNodeMatcher[_]]
  override def equals(that: Any) = that match {
    case that: TrivialNodeMatcher[_] => that canEqual this
    case _ => false
  }
  override def hashCode = toString.hashCode
}

/** Trait that captures the contents of a node if it's matched.
  * @param  alias  the name of the captured node
  * @param  matcher  the matcher to apply
  */
class CaptureNodeMatcher[T](val alias: String, matcher: NodeMatcher[T])
    extends WrappedNodeMatcher[T](matcher) {
  /** Convenience constructor that uses the TrivialNodeMatcher.
    */
  def this(alias: String) = this(alias, new TrivialNodeMatcher[T]())

  override def matchText(node: T) = matcher.matchText(node)

  // extend Object
  override def toStringF(f: String => String) = f("{" +
    (if (matcher.isInstanceOf[TrivialNodeMatcher[_]]) {
      alias
    } else {
      alias + ":" + matcher.toStringF(f)
    }) + "}")
  override def canEqual(that: Any) = that.isInstanceOf[CaptureNodeMatcher[_]]
  override def equals(that: Any) = that match {
    case that: CaptureNodeMatcher[_] => (that canEqual this) && this.alias == that.alias && super.equals(that)
    case _ => false
  }
  override def hashCode = alias.hashCode + 39 * matcher.hashCode
}
