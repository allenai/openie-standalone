package edu.knowitall.srlie

import edu.knowitall.srlie.SrlExtraction.{Context, Relation}
import nl.jqno.equalsverifier.{EqualsVerifier, Warning}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SrlExtractionTest extends FunSuite {

  test("Relation") {
    EqualsVerifier.forClass(classOf[Relation])
        .suppress(Warning.NULL_FIELDS)
        .suppress(Warning.NONFINAL_FIELDS)
        .suppress(Warning.STRICT_INHERITANCE)
        .withRedefinedSuperclass()
        .verify
  }

  test("Context") {
    EqualsVerifier.forClass(classOf[Context])
        .suppress(Warning.NULL_FIELDS)
        .suppress(Warning.NONFINAL_FIELDS)
        .suppress(Warning.STRICT_INHERITANCE)
        .withRedefinedSuperclass()
        .verify
  }

}

