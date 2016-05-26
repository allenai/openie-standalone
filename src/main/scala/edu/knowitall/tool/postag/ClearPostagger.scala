package edu.knowitall
package tool
package postag

import java.util.zip.ZipInputStream
import scala.collection.JavaConverters.asScalaIteratorConverter
import com.clearnlp.nlp.NLPGetter
import com.clearnlp.nlp.NLPMode
import com.clearnlp.dependency.DEPNode
import com.clearnlp.dependency.DEPTree
import edu.knowitall.common.Resource.using
import edu.knowitall.tool.tokenize.Tokenizer
import edu.knowitall.tool.tokenize.ClearTokenizer
import edu.knowitall.tool.tokenize.Token

class ClearPostagger(override val tokenizer: Tokenizer = new ClearTokenizer) extends Postagger {

  val clearPosTagger = NLPGetter.getComponent("general-en", "en", NLPMode.MODE_POS);

  override def postagTokenized(tokens: Seq[Token]) = {
    val tree = new DEPTree()
    tokens.zipWithIndex.foreach {
      case (token, i) =>
        tree.add(new DEPNode(i + 1, token.string))
    }

    clearPosTagger.process(tree)

    val postaggedTokens = for ((treeNode, token) <- (tree.iterator.asScala.drop(1).toSeq zip tokens)) yield {
      PostaggedToken(token, treeNode.pos)
    }

    postaggedTokens
  }
}

object ClearPostaggerMain extends PostaggerMain {
  override val tagger = new ClearPostagger()
}
