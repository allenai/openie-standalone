package edu.knowitall.collection.immutable.graph

import edu.knowitall.collection.immutable.graph.Graph.Edge
import nl.jqno.equalsverifier.{EqualsVerifier, Warning}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BipathTest extends FunSuite {

  val e : Edge[String] = new Edge[String]("source", "dest", "label")
  val ue1 = new UpEdge[String](e)
  val de2 = new DownEdge[String](e)

  test("Bipath") {
    EqualsVerifier.forClass(classOf[Bipath[_]])
        .suppress(Warning.NULL_FIELDS)
        .suppress(Warning.STRICT_INHERITANCE)
        .withPrefabValues(classOf[List[_]], List(ue1), List(de2))
        .verify
  }

}
