package edu.knowitall
package tool
package chunk

import org.junit._
import org.junit.Assert._
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
object OpenNlpParserTest extends Specification {
  "chunk example sentence" in {
    val text = "This is a test of the OpenNlp chunker."
    val chunker = new OpenNlpChunker

    val chunked = chunker.chunk(text)
    chunked.mkString(" ") must_== "This/DT/B-NP@0 is/VBZ/B-VP@5 a/DT/B-NP@8 test/NN/I-NP@10 of/IN/B-PP@15 the/DT/B-NP@18 OpenNlp/NNP/I-NP@22 chunker/NN/I-NP@30 ././O@37"
  }
}

