package edu.knowitall
package tool
package parse
package graph

import edu.knowitall.collection.immutable.graph._
import edu.knowitall.collection.immutable.graph.Graph._
import edu.knowitall.collection.immutable.graph.pattern._
import scala.util.matching.Regex
import java.util.regex.{ Pattern => JPattern }
import tool.stem.Stemmer
import tool.stem.IdentityStemmer

/** A pattern over a graph of dependencies.
  */
class DependencyPattern(matchers: List[Matcher[DependencyNode]])
    extends Pattern[DependencyNode](matchers) {
  require(matchers != null)

  def this(pattern: Pattern[DependencyNode]) = this(pattern.matchers)
}

object DependencyPattern {
  import scala.util.parsing.combinator._

  /** A parser combinator for deserializing patterns over graphs of dependencies.
    */
  class Parser extends RegexParsers {
    def textNodeMatcher: Parser[NodeMatcher[DependencyNode]] = "text=" ~> """\w+""".r ^^ { s =>
      new TextNodeMatcher(s)
    }
    def regexNodeMatcher(implicit lemmatizer: Stemmer): Parser[NodeMatcher[DependencyNode]] = "regex=" ~> """[^<>{}:\p{Space}]+""".r ^^ { r =>
      new RegexNodeMatcher(new Regex(r))(lemmatizer)
    }
    def postagNodeMatcher: Parser[NodeMatcher[DependencyNode]] = "postag=" ~> """[^:}\p{Space}]+""".r ^^ { postag =>
      new PostagNodeMatcher(postag)
    }

    def simpleNodeMatcher(implicit lemmatizer: Stemmer): Parser[NodeMatcher[DependencyNode]] = regexNodeMatcher(lemmatizer) | postagNodeMatcher | textNodeMatcher
    def simpleNodeMatcherSeq(implicit lemmatizer: Stemmer): Parser[List[NodeMatcher[DependencyNode]]] = (simpleNodeMatcher(lemmatizer) ~ ":" ~ simpleNodeMatcherSeq(lemmatizer) ^^ { case m ~ ":" ~ ms => m :: ms }) | (simpleNodeMatcher(lemmatizer) ^^ { case m => List(m) })

    def trivialCaptureNodeMatcher: Parser[CaptureNodeMatcher[DependencyNode]] = "{" ~> """\w+""".r <~ "}" ^^ { s => new CaptureNodeMatcher[DependencyNode](s) }
    def nestedCaptureNodeMatcher(implicit lemmatizer: Stemmer): Parser[CaptureNodeMatcher[DependencyNode]] = "{" ~> """\w+""".r ~ ":" ~ simpleNodeMatcherSeq(lemmatizer) <~ "}" ^^ {
      case alias ~ ":" ~ seq =>
        seq match {
          case x :: Nil => new CaptureNodeMatcher[DependencyNode](alias, x)
          case seq @ x :: xs => new CaptureNodeMatcher[DependencyNode](alias, new ConjunctiveNodeMatcher(seq.toSet))
          case Nil => throw new IllegalArgumentException()
        }
    }

    def captureNodeMatcher(implicit lemmatizer: Stemmer) = nestedCaptureNodeMatcher(lemmatizer) | trivialCaptureNodeMatcher

    def nodeMatcher[V](implicit lemmatizer: Stemmer): Parser[NodeMatcher[DependencyNode]] = simpleNodeMatcher(lemmatizer) | captureNodeMatcher(lemmatizer) | ("""\w+""" ^^ { s => new TextNodeMatcher(s) })

    def regexEdgeMatcher = "regex=" ~> """[^<>{}:\p{Space}]+""".r ^^ {
      case regex =>
        new RegexEdgeMatcher(regex.r)
    }
    def simpleEdgeMatcher = """[^<>{}:\p{Space}]+""".r ^^ { s => new LabelEdgeMatcher(s) }
    def edgeMatcher: Parser[EdgeMatcher[DependencyNode]] = regexEdgeMatcher | simpleEdgeMatcher

    def trivialCaptureEdgeMatcher: Parser[EdgeMatcher[DependencyNode]] = "{" ~> """\w+""".r <~ "}" ^^ {
      case alias =>
        new CaptureEdgeMatcher(alias, new TrivialEdgeMatcher)
    }
    def complexCaptureEdgeMatcher: Parser[EdgeMatcher[DependencyNode]] = "{" ~> """\w+""".r ~ ":" ~ edgeMatcher <~ "}" ^^ {
      case alias ~ ":" ~ m =>
        new CaptureEdgeMatcher(alias, m)
    }
    def captureEdgeMatcher: Parser[EdgeMatcher[DependencyNode]] = trivialCaptureEdgeMatcher | complexCaptureEdgeMatcher

    def complexEdgeMatcher: Parser[EdgeMatcher[DependencyNode]] = edgeMatcher | captureEdgeMatcher

    def upEdgeMatcher = "<" ~> complexEdgeMatcher <~ "<" ^^ {
      case c: CaptureEdgeMatcher[_] => new CaptureEdgeMatcher[DependencyNode](c.alias, new DirectedEdgeMatcher(Direction.Up, c.matcher))
      case m => new DirectedEdgeMatcher(Direction.Up, m)
    }
    def downEdgeMatcher = ">" ~> complexEdgeMatcher <~ ">" ^^ {
      case c: CaptureEdgeMatcher[_] => new CaptureEdgeMatcher[DependencyNode](c.alias, new DirectedEdgeMatcher(Direction.Down, c.matcher))
      case m => new DirectedEdgeMatcher(Direction.Down, m)
    }

    def directedEdgeMatcher = upEdgeMatcher | downEdgeMatcher

    // def edgeMatcher[V]: Parser[EdgeMatcher[DependencyNode]] = (captureUpEdgeMatcherPostag | captureDownEdgeMatcherPostag | captureUpEdgeMatcher | captureDownEdgeMatcher | upEdgeMatcher | downEdgeMatcher) ^^ { s => s.asInstanceOf[EdgeMatcher[DependencyNode]] }

    def chain[V](implicit lemmatizer: Stemmer): Parser[List[Matcher[DependencyNode]]] = nodeMatcher(lemmatizer) ~ directedEdgeMatcher ~ chain ^^ { case n ~ e ~ ch => n :: e :: ch } | nodeMatcher ^^ { List(_) }

    def parse(s: String)(implicit lemmatizer: Stemmer) = {
      parseAll(chain(lemmatizer), s)
    }

    def apply(s: String)(implicit lemmatizer: Stemmer): DependencyPattern = {
      this.parse(s)(lemmatizer) match {
        case Success(matchers, _) => new DependencyPattern(matchers)
        case fail: Failure =>
          throw new IllegalArgumentException("improper pattern syntax. " + fail.msg + ": " + s)
        case error: Error =>
          throw new IllegalArgumentException("error on pattern syntax '" + s + "': " + error.toString)
      }
    }
  }

  class StringFormat(implicit lemmatizer: Stemmer) extends Format[DependencyPattern, String] {
    private val parser = new Parser

    def read(pickled: String): DependencyPattern = {
      try {
        parser.synchronized {
          parser(pickled)(lemmatizer)
        }
      } catch {
        case e: Throwable => e.printStackTrace(); throw new DependencyPatternSerializationException(e.getMessage(), e)
      }
    }

    def write(pattern: DependencyPattern): String = {
      pattern.serialize
    }
  }

  /** A more intuitive constructor that builds the pattern from a
    * bidirectional path though the tree.
    */
  def create(bipath: Bipath[DependencyNode]) = new Pattern[DependencyNode](
    bipath.path.map(dedge => DependencyEdgeMatcher(dedge)),
    new DependencyNodeMatcher(bipath.path.head.start) :: bipath.path.map(dedge => new DependencyNodeMatcher(dedge.end))
  )

  @deprecated("Use stringFormat instead.", "2.4.5")
  def deserialize(string: String)(implicit lemmatizer: Stemmer): DependencyPattern =
    new StringFormat().read(string)

  def main(args: Array[String]) {
    val patternFormat = new DependencyPattern.StringFormat()(IdentityStemmer.instance)
    def print(tab: Int, m: Matcher[_]): Unit = {
      println(" " * (tab * 2) + m)
      println(" " * (tab * 2) + m.getClass.getSimpleName)
      m match {
        case m: WrappedNodeMatcher[_] => print(tab + 1, m.matcher)
        case m: ConjunctiveNodeMatcher[_] => m.matchers.foreach { print(tab + 1, _) }
        case m: WrappedEdgeMatcher[_] => print(tab + 1, m.matcher)
        case _ =>
      }
    }

    val pattern = patternFormat.read(args(0))
    println(pattern)
    for (m <- pattern.matchers) {
      print(0, m)
    }
  }
}

class DependencyPatternSerializationException(message: String, cause: Throwable)
  extends RuntimeException(message, cause)

abstract class DependencyEdgeMatcher extends BaseEdgeMatcher[DependencyNode]
object DependencyEdgeMatcher {
  def apply(dedge: DirectedEdge[DependencyNode]) =
    new DirectedEdgeMatcher(dedge.dir, new LabelEdgeMatcher(dedge.edge.label))
}

/** Match a `DirectedEdge[DependencyNode]`.
  */
class LabelEdgeMatcher(val label: String) extends DependencyEdgeMatcher {
  override def matchText(edge: DirectedEdge[DependencyNode]) =
    if (label == edge.edge.label) Some(label)
    else None

  override def flip = this

  // extend Object
  override def toStringF(f: String => String) = f(label)
  def canEqual(that: Any) = that.isInstanceOf[LabelEdgeMatcher]
  override def equals(that: Any) = that match {
    case that: LabelEdgeMatcher => (that canEqual this) && this.label == that.label
    case _ => false
  }
  override def hashCode = this.label.hashCode
}

/** Match a `DirectedEdge[DependencyNode]`.
  */
class RegexEdgeMatcher(val labelRegex: Regex) extends DependencyEdgeMatcher {
  override def matchText(edge: DirectedEdge[DependencyNode]) = edge.edge.label match {
    case labelRegex(group) => Some(group)
    case _ => None
  }

  override def flip = this

  // extend Object
  override def toStringF(f: String => String) = f("regex=" + labelRegex.toString)
  def canEqual(that: Any) = that.isInstanceOf[RegexEdgeMatcher]
  override def equals(that: Any) = that match {
    case that: RegexEdgeMatcher => (that canEqual this) && this.labelRegex.toString == that.labelRegex.toString
    case _ => false
  }
  override def hashCode = this.labelRegex.toString.hashCode
}

/** Match a `DependencyNode`.
  */
class DependencyNodeMatcher(val text: String, val postag: String)
    extends BaseNodeMatcher[DependencyNode] {
  override def matches(node: DependencyNode) = node.text == text && node.postag == postag
  override def matchText(node: DependencyNode) = if (matches(node)) Some(node.text) else None

  // extend Object
  override def toStringF(f: String => String) = f(text)
  def canEqual(that: Any) = that.isInstanceOf[DependencyNodeMatcher]
  override def equals(that: Any) = that match {
    case that: DependencyNodeMatcher => (that canEqual this) && this.text == that.text && this.postag == that.postag
    case _ => false
  }
  override def hashCode = text.hashCode + 39 * postag.hashCode

  def this(node: DependencyNode) = this(node.text, node.postag)
}

/** Match a `DependencyNode`.
  */
class TextNodeMatcher(val text: String) extends BaseNodeMatcher[DependencyNode] {
  def this(node: DependencyNode) = this(node.text)

  def matchText(node: DependencyNode) = if (matches(node)) Some(node.text) else None
  override def matches(node: DependencyNode) = node.text == text

  // extend Object
  override def toStringF(f: String => String) = f("text=" + text)
  def canEqual(that: Any) = that.isInstanceOf[TextNodeMatcher]
  override def equals(that: Any) = that match {
    case that: TextNodeMatcher => (that canEqual this) && this.text == that.text
    case _ => false
  }
  override def hashCode = text.hashCode + 39
}

class PostagNodeMatcher(val postag: String) extends BaseNodeMatcher[DependencyNode] {
  def this(node: DependencyNode) = this(node.postag)

  def matchText(node: DependencyNode) = if (matches(node)) Some(node.text) else None
  override def matches(node: DependencyNode) = node.postag == postag

  // extend Object
  override def toStringF(f: String => String) = f("postag=" + postag)
  def canEqual(that: Any) = that.isInstanceOf[PostagNodeMatcher]
  override def equals(that: Any) = that match {
    case that: PostagNodeMatcher => (that canEqual this) && this.postag == that.postag
    case _ => false
  }
  override def hashCode = postag.hashCode + 39
}

class RegexNodeMatcher(val regex: Regex)(implicit lemmatizer: Stemmer) extends BaseNodeMatcher[DependencyNode] {
  override def matches(node: DependencyNode) = node.lemma match {
    case regex() => true
    case _ => false
  }

  def matchText(node: DependencyNode) = node.lemma match {
    case regex(group) => Some(group)
    case regex() => Some(node.text)
    case _ => None
  }

  override def toStringF(f: String => String) = f("regex=" + regex.toString)
  def canEqual(that: Any) = that.isInstanceOf[RegexNodeMatcher]
  override def equals(that: Any) = that match {
    case that: RegexNodeMatcher => (that canEqual this) && this.regex.toString == that.regex.toString
    case _ => false
  }
  override def hashCode = this.regex.toString.hashCode
}
