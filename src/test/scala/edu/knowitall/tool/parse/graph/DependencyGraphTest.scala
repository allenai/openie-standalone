package edu.knowitall.tool.parse.graph

import nl.jqno.equalsverifier.{EqualsVerifier, Warning}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DependencyGraphTest extends FunSuite {

  test("DependencyGraph") {
    EqualsVerifier.forClass(classOf[DependencyGraph])
        .suppress(Warning.NULL_FIELDS)
        .verify
  }

}
