package edu.knowitall.tool.srl

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.tokenize.Token
import edu.knowitall.tool.parse.graph.DependencyNode
import edu.knowitall.tool.parse.graph.DependencyGraph

case class Frame(relation: Relation, arguments: Seq[Argument]) {
  override def toString = relation.toString + ":" + arguments.mkString("[", ", ", "]")
  def nodes = arguments.map(_.node).toSet + relation.node
  def serialize = relation.serialize + ":" + arguments.map(_.serialize).mkString("[", ", ", "]")
  def argument(role: Role): Option[Argument] = this.arguments.find(_.role == role)
}
object Frame {
  val pickledRegex = """([^:]*):\[(.*)\]""".r
  def deserialize(dgraph: DependencyGraph)(pickled: String) = {
    pickled match {
      case pickledRegex(relation, arguments) =>
        val rel = Relation.deserialize(dgraph)(relation)
        val args = arguments.split(",\\s+") filter (!_.trim.isEmpty) map Argument.deserialize(dgraph)
        Frame(rel, args)
      case _ => throw new IllegalArgumentException("Could not deserialize: " + pickled)
    }
  }
}

case class Relation(node: DependencyNode, name: String, sense: String) {
  require(!(name matches """.*[:\[\]].*"""), "Invalid relation name: " + name)
  override def toString = name + "." + sense
  def serialize = name + "_" + node.index + "." + sense
}
object Relation {
  val relationRegex = "(.*)\\.(.*)".r
  def fromString(node: DependencyNode, string: String) = {
    val (name, sense) = string match {
      case relationRegex(name, sense) => (name, sense)
      case _ => throw new MatchError("Could not create relation with node " + node + " from string: " + string)
    }

    // replace restricted characters
    def cleanName = name
      .replaceAll("\\[", "(")
      .replaceAll("\\]", ")")
      .replaceAll(":", "-")
    Relation(node, cleanName, sense)
  }

  def deserialize(dgraph: DependencyGraph)(pickled: String) = {
    val (label, sense) = pickled.split("\\.") match {
      case Array(label, sense) => (label, sense)
      case _ => throw new MatchError("Could not deserialize relation: " + pickled)
    }
    val (name, nodeIndex) = label.split("_") match {
      case Array(name, nodeIndex) => (name, nodeIndex)
      case _ => throw new MatchError("Could not deserialize relation label: " + label)
    }

    val node = dgraph.nodes.find(_.index == nodeIndex.toInt).get
    Relation(node, name, sense)
  }
}

case class Argument(node: DependencyNode, role: Role) {
  override def toString = role + "=" + node.string
  def serialize = role + "=" + node.string + "_" + node.index
}
object Argument {
  def deserialize(dgraph: DependencyGraph)(pickled: String) = {
    val (roleString, rest) = pickled.span(_ != '=')
    val (string, nodeIndex) = rest.drop(1).split("_") match {
      case Array(string, nodeIndex) => (string, nodeIndex)
      case _ => throw new MatchError("Could not deserialize argument: " + rest)
    }
    val node = dgraph.nodes.find(_.index == nodeIndex.toInt).get
    require(node.text == string, node.text + " != " + string)
    Argument(node, Roles(roleString))
  }
}

abstract class Role(val description: String) {
  override def toString = label
  def label = this.getClass.getSimpleName.replaceAll("_", "-").takeWhile(_ != '$')
}
abstract class ArgumentRole(description: String) extends Role(description)
abstract class ArgumentModifierRole(description: String) extends Role(description)
object Roles {
  def apply(label: String) = {
    val APattern = """A(\d+)""".r
    val CPattern = """C-([-a-zA-Z_0-9]+)""".r
    val RPattern = """R-([-a-zA-Z_0-9]+)""".r
    label match {
      case "A0" => A0
      case "A1" => A1
      case "A2" => A2
      case "A3" => A3
      case "A4" => A4
      case "A5" => A5
      case CPattern(s) => new C(s)
      case RPattern(s) => new R(s)
      case "AM-ADV" => AM_ADV
      case "AM-DIR" => AM_DIR
      case "AM-DIS" => AM_DIS
      case "AM-EXT" => AM_EXT
      case "AM-LOC" => AM_LOC
      case "AM-MNR" => AM_MNR
      case "AM-MOD" => AM_MOD
      case "AM-NEG" => AM_NEG
      case "AM-PRD" => AM_PRD
      case "AM-PRP" => AM_PRP
      case "AM-REC" => AM_REC
      case "AM-TMP" => AM_TMP
      case _ => UnknownRole(label)
    }
  }
  case object A0 extends ArgumentRole("subject")
  case object A1 extends ArgumentRole("object")
  case object A2 extends ArgumentRole("indirect object")
  case object A3 extends ArgumentRole("???")
  case object A4 extends ArgumentRole("???")
  case object A5 extends ArgumentRole("???")
  case class C(string: String) extends Role("continuation") {
    def role: Role = Roles(string)
    override def label = "C-" + string
  }
  case class R(string: String) extends Role("reference") {
    def role: Role = Roles(string)
    override def label = "R-" + string
  }
  case object AM_ADV extends ArgumentModifierRole("adverbial modification")
  case object AM_DIR extends ArgumentModifierRole("direction")
  case object AM_DIS extends ArgumentModifierRole("discourse marker")
  case object AM_EXT extends ArgumentModifierRole("extent")
  case object AM_LOC extends ArgumentModifierRole("location")
  case object AM_MNR extends ArgumentModifierRole("manner")
  case object AM_MOD extends ArgumentModifierRole("general modification")
  case object AM_NEG extends ArgumentModifierRole("negation")
  case object AM_PRD extends ArgumentModifierRole("secondary predicate")
  case object AM_PRP extends ArgumentModifierRole("purpose")
  case object AM_REC extends ArgumentModifierRole("recipricol")
  case object AM_TMP extends ArgumentModifierRole("temporal")
  case class UnknownRole(val role: String) extends Role(role) {
    override def label = role
  }
}
