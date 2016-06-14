package edu.knowitall.collection.immutable.graph

import nl.jqno.equalsverifier.{EqualsVerifier, Warning}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GraphTest extends FunSuite {

  test("Graph") {
    EqualsVerifier.forClass(classOf[Graph[_]])
        .suppress(Warning.NULL_FIELDS)
        .suppress(Warning.STRICT_INHERITANCE)
        .verify
  }

}
