package edu.knowitall
package tool
package tokenize

import org.junit._
import org.junit.Assert._
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
object OpenNlpParserTest extends Specification {
  "tokenize example sentence" in {
    val text = "This is a test of the OpenNlp tokenizer."
    val tokenizer = new OpenNlpTokenizer

    val tokenized = tokenizer.tokenize(text)
    tokenized.mkString(" ") must_== "This@0 is@5 a@8 test@10 of@15 the@18 OpenNlp@22 tokenizer@30 .@39"
  }
}

