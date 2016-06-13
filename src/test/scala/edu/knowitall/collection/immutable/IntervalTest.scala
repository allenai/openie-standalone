package edu.knowitall.collection.immutable

import nl.jqno.equalsverifier.{EqualsVerifier, Warning}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class IntervalTest extends FunSuite {

  test("Interval") {
    EqualsVerifier.forClass(classOf[Interval])
        .suppress(Warning.NULL_FIELDS)
        .verify
  }

}
