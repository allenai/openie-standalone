package edu.knowitall
package tool
package parse

import graph._

/** A constituency parser turns a sentence into a constituency
  * tree, a structure that is somewhat like chunking but
  * hierarchical.
  */
trait ConstituencyParser {
  def parse(string: String): ParseTree
}

/** A representation of the constituency parse. */
abstract class ParseTree(val token: String, var index: Int, val children: Array[ParseTree]) extends Iterable[ParseTree] {

  /** Prints the tree in Penn treebank format. */
  override def toString() =
    if (children.size == 0) {
      token
    }
    else {
      "(" + token + " " + children.map(child => child.toString).mkString(" ") + ")"
    }

  def value = token

  def iterator = {
    def preorder(node: ParseTree): List[ParseTree] = node :: node.children.toList.flatMap(preorder(_))
    preorder(this).iterator
  }

  def print {
    def print(tree: ParseTree, indent: Int) {
      if (tree.children.isEmpty) {
        println(" " * indent + "(" + tree.token + ")")
      } else {
        println(" " * indent + "(" + tree.token)
        tree.children.foreach { tree => print(tree, indent + 2) }
        println(" " * indent + ")")
      }
    }

    print(this, 0)
  }

  def printDOT(writer: java.lang.Appendable) {
    def quote(string: String) = "\"" + string + "\""
    def nodeString(node: ParseTree) = node.token
    val indent = " " * 2

    writer.append("digraph g {\n")

    for (node <- this) {
      val shape = node match {
        case node: ParseTreePhrase => "box"
        case node: ParseTreePostag => "invtriangle"
        case node: ParseTreeToken => "circle"
      }
      writer.append(indent + node.index + " [label=" + quote(nodeString(node)) + ", shape=" + quote(shape) + "]\n")
    }

    for (node <- this) {
      for (child <- node.children) {
        writer.append(indent + node.index.toString + " -> " + child.index.toString + "\n")
      }
    }
    writer.append("}")
  }
}

class ParseTreePhrase(token: String, index: Int, children: Array[ParseTree]) extends ParseTree(token, index, children) {}
class ParseTreePostag(token: String, index: Int, children: Array[ParseTree]) extends ParseTree(token, index, children) {}
class ParseTreeToken(token: String, index: Int, children: Array[ParseTree]) extends ParseTree(token, index, children) {}
