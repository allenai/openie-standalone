package edu.knowitall.tool.srl

import java.util.zip.ZipInputStream

import scala.Option.option2Iterable
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.io.Source

import com.clearnlp.nlp.NLPGetter
import com.clearnlp.nlp.NLPMode
import com.clearnlp.dependency.DEPNode
import com.clearnlp.dependency.DEPTree

import edu.knowitall.collection.immutable.Interval
import edu.knowitall.common.Resource.using
import edu.knowitall.tool.parse.ClearParser
import edu.knowitall.tool.parse.graph.DependencyGraph

class ClearSrl extends Srl {
  /* val clearMorpha = using(this.getClass.getResource("/edu/knowitall/tool/tokenize/dictionary-1.2.0.zip").openStream()) { input =>
    new CEnglishMPAnalyzer(new ZipInputStream(input))
  }
  val clearRoles = using(this.getClass.getResource("/knowitall/models/clear/ontonotes-en-role-1.3.0.jar").openStream()) { input =>
    new CRolesetClassifier(new ZipInputStream(input))
  }
  val clearPred = using(this.getClass.getResource("/knowitall/models/clear/ontonotes-en-pred-1.3.0.jar").openStream()) { input =>
    new CPredIdentifier(new ZipInputStream(input))
  }
  val clearSrl = using(this.getClass.getResource("/knowitall/models/clear/ontonotes-en-srl-1.3.0.jar").openStream()) { input =>
    new CSRLabeler(new ZipInputStream(input))
  }*/

  private val modelType = "general-en"
  private val language = "en"

  private val clearMorpha = NLPGetter.getComponent(modelType, language, NLPMode.MODE_MORPH)
  private val clearRoles = NLPGetter.getComponent(modelType, language, NLPMode.MODE_ROLE)
  private val clearPred = NLPGetter.getComponent(modelType, language, NLPMode.MODE_PRED)
  private val clearSrl = NLPGetter.getComponent(modelType, language, NLPMode.MODE_SRL)

  def apply(graph: DependencyGraph): Seq[Frame] = {
    val tree = new DEPTree()

    // During a from-scratch compile of this project, Scala appears to resolve
    // graph.nodes.zipWithIndex to an implementation in IterableLike. (In this
    // case, foreach returns nodes out-of-order with respect to the index
    // position.) A recompilation of just this source file in a previously
    // compiled project appears to cause Scala to select the zipWithIndex
    // implementation in Iterable. (In this case, foreach returns nodes
    // in-order with respect to index position.)
    //
    // The cause is possibly related a change in code loading order and the use
    // implicit variables (for example, in IterableLike.)
    //
    // Ultimately this subtle difference causes DEPTree to receive nodes in a
    // different order, causing radically different final results. By
    // explicitly requesting an iterator from nodes, Scala consistently selects
    // the zipWithIndex implementation in Iterable resulting in consistent final
    // results.
    graph.nodes.iterator.zipWithIndex.foreach {
      case (token, i) =>
        val node = new DEPNode(i + 1, token.string)
        node.pos = token.postag
        tree.add(node)
    }

    for (edge <- graph.dependencies) {
      val source = tree.get(edge.source.indices.head + 1)
      val dest = tree.get(edge.dest.indices.head + 1)
      dest.setHead(source, edge.label)
    }

    // link all nodes other than the root (hence the drop 1)
    // to the root node.
    for (node <- tree.iterator.asScala.drop(1)) {
      if (node.getHead == null) {
        node.setHead(tree.get(0), "root")
      }
    }

    clearMorpha.process(tree)
    clearPred.process(tree)
    clearRoles.process(tree)
    clearSrl.process(tree)

    val treeNodes = tree.asScala.toSeq
    val relations = treeNodes.flatMap { node =>
      val index = node.id - 1
      Option(node.getFeat("pb")).map(index -> Relation.fromString(graph.nodes.find(_.indices == Interval.singleton(index)).get, _))
    }

    val arguments = treeNodes.flatMap { node =>
      val index = node.id - 1
      node.getSHeads().asScala.map { head =>
        (head.getNode.id - 1) -> Argument(graph.nodes.find(_.indices == Interval.singleton(index)).get, Roles(head.getLabel))
      }
    }

    relations.map {
      case (index, rel) =>
        val args = arguments.filter(_._1 == index)
        new Frame(rel, args.map(_._2))
    }
  }
}

object ClearSrlMain extends SrlMain {
  override val srl = new ClearSrl()
}
