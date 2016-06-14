package edu.knowitall
package tool
package parse

import scala.collection.JavaConverters._
import edu.knowitall.tool.tokenize.Tokenizer
import edu.knowitall.tool.tokenize.Token
import graph.Dependency
import edu.knowitall.tool.parse.graph.DependencyGraph
import edu.knowitall.tool.parse.graph.DependencyNode
import java.lang.ProcessBuilder
import java.io.PrintWriter

//import com.googlecode.clearnlp.component.pos.CPOSTagger
//import com.googlecode.clearnlp.component.dep.CDEPPassParser
import java.util.zip.ZipInputStream

//import com.googlecode.clearnlp.nlp.NLPDecode
//import com.googlecode.clearnlp.dependency.DEPTree
//import com.googlecode.clearnlp.dependency.DEPNode

import edu.knowitall.tool.tokenize.ClearTokenizer
import edu.knowitall.common.Resource.using

//import com.googlecode.clearnlp.component.morph.CEnglishMPAnalyzer
import edu.knowitall.tool.postag.Postagger
import edu.knowitall.tool.postag.PostaggedToken
import edu.knowitall.tool.postag.ClearPostagger

import com.clearnlp.nlp.NLPGetter
import com.clearnlp.nlp.NLPMode
import com.clearnlp.dependency.DEPNode
import com.clearnlp.dependency.DEPTree

class ClearParser(val postagger: Postagger = new ClearPostagger()) extends DependencyParser {

  val clearMorpha = NLPGetter.getComponent("general-en", "en", NLPMode.MODE_MORPH)

  val clearDepParser = NLPGetter.getComponent("general-en", "en", NLPMode.MODE_DEP)

  def dependencyGraphPostagged(tokens: Seq[PostaggedToken]): DependencyGraph = {
    val tree = new DEPTree()
    tokens.zipWithIndex.foreach {
      case (token, i) =>
        val node = new DEPNode(i + 1, token.string)
        node.pos = token.postag
        tree.add(node)
    }

    clearMorpha.process(tree)
    clearDepParser.process(tree)

    ClearParser.graphFromTree(tree, tokens)
  }
}

object ClearParser {
  def graphFromTree(tree: DEPTree, tokens: Seq[Token]) = {
    val nodeMap = (for ((node, i) <- tree.iterator.asScala.zipWithIndex) yield {
      if (i == 0) {
        node.id -> new DependencyNode(node.form, node.pos, -1, -1)
      }
      else {
        node.id -> new DependencyNode(node.form, node.pos, i - 1, tokens(i - 1).offset)
      }
    }).toMap

    val deps = for {
      sourceNode <- tree.iterator.asScala.toList
      if sourceNode.hasHead
      if sourceNode.id != 0
      label = sourceNode.getLabel
      destNode = sourceNode.getHead
      if destNode.id != 0
    } yield {
      new Dependency(nodeMap(destNode.id), nodeMap(sourceNode.id), label)
    }

    new DependencyGraph(nodeMap.values filterNot (_.index == -1), deps)
  }
}

object ClearDependencyParserMain extends DependencyParserMain {
  lazy val dependencyParser = new ClearParser()
}
