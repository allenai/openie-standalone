package edu.knowitall.collection.immutable.graph.pattern

import nl.jqno.equalsverifier.{EqualsVerifier, Warning}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MatcherTest extends FunSuite {

  test("ConjunctiveNodeMatcher") {
    EqualsVerifier.forClass(classOf[ConjunctiveNodeMatcher[_]])
        .suppress(Warning.NULL_FIELDS)
        .suppress(Warning.STRICT_INHERITANCE)
        .verify
  }

  test("WrappedNodeMatcher") {
    EqualsVerifier.forClass(classOf[WrappedNodeMatcher[_]])
        .suppress(Warning.NULL_FIELDS)
        .suppress(Warning.STRICT_INHERITANCE)
        .verify
  }
}
