package edu.knowitall.chunkedextractor

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer

@RunWith(classOf[JUnitRunner])
object RelnounSpecTest extends Specification {
  def extract(sentence: String) = {
    val chunker = new OpenNlpChunker
    val relnoun = new Relnoun(true, true, true)
    val chunked = chunker.chunk(sentence)
    val lemmatized = chunked.map(MorphaStemmer.lemmatizeToken)
    relnoun(lemmatized)
  }

  def test(name: String, sentence: String, extraction: (String, String, String)) = {
    name should {
      val extrs = extract(sentence)
      "have a single extraction" in {
        extrs.size must_== 1
      }
      "have the correct extraction" in {
        extrs.head.extr.rel.toString must_== extraction._2
        extrs.head.extr.arg1.toString must_== extraction._1
        extrs.head.extr.arg2.toString must_== extraction._3
      }
    }
  }

  test(
    "VerbBasedExtractor",
    "Barack Obama is the president of the United States.",
    ("Barack Obama", "is the president of", "the United States")
  )

  test(
    "AppositiveExtractor",
    "Barack Obama, the President of the U.S.",
    ("Barack Obama", "[is] the President of", "the U.S.")
  )

  test(
    "AppositiveExtractor_pronoun",
    "He, the President of the U.S.",
    ("He", "[is] the President of", "the U.S.")
  )

  test(
    "AppositiveExtractor2",
    "Lauren Faust, a cartoonist,",
    ("Lauren Faust", "[is]", "a cartoonist")
  )

  test(
    "AppositiveExtractor2_pronoun",
    "He, a cartoonist,",
    ("He", "[is]", "a cartoonist")
  )

  test(
    "AdjectiveDescriptorExtractor_[of]",
    "United States President Barack Obama gave a speech today.",
    ("Barack Obama", "[is] President [of]", "United States")
  )

  test(
    "AdjectiveDescriptorExtractor__[from]",
    "Indian player Sachin Tendulkar received the Arjuna Award in 1994.",
    ("Sachin Tendulkar", "[is] player [from]", "India")
  )

  test(
    "AdjectiveDescriptorExtractor_title",
    "President Barack Obama gave a speech today.",
    ("Barack Obama", "[is] President [of]", "[UNKNOWN]")
  )

  test(
    "AdjectiveDescriptorExtractor_title_more_1",
    "Prime Minister Narendra Modi gave a speech today.",
    ("Narendra Modi", "[is] Prime Minister [of]", "[UNKNOWN]")
  )

  test(
    "AdjectiveDescriptorExtractor_prefix",
    "Indian Vice President Modi.",
    ("Modi", "[is] Vice President [of]", "India")
  )

  test(
    "AdjectiveDescriptorExtractor_pronoun",
    "His father John,",
    ("John", "[is] father [of]", "Him")
  )

  test(
    "AdjectiveDescriptorExtractor_more_1",
    "Foreign Ministry spokesman Qin Gang.",
    ("Qin Gang", "[is] spokesman [of]", "Foreign Ministry")
  )

  test(
    "AdjectiveDescriptorExtractor_more_2",
    "New Yorker's best staff writer Adam.",
    ("Adam", "[is] best staff writer [from]", "New York")
  )

  test(
    "AdjectiveDescriptorExtractor_more_3",
    "General Motors interim chief executive Ed Whitacre.",
    ("Ed Whitacre", "[is] interim chief executive [of]", "General Motors")
  )

  test(
    "AdjectiveDescriptorExtractor_more_4",
    "foreign Indian spokesman Qin Gang.",
    ("Qin Gang", "[is] foreign spokesman [from]", "India")
  )

  test(
    "AdjectiveDescriptorExtractor_more_5",
    "first Indian spokesman Qin Gang.",
    ("Qin Gang", "[is] first spokesman [from]", "India")
  )

  test(
    "AdjectiveDescriptorExtractor_more_6",
    "New Zealand coach Steve Hansen.",
    ("Steve Hansen", "[is] coach [from]", "New Zealand")
  )

  /*test("AdjectiveDescriptorExtractor_more_7",
      "Costa Rican President Luis Guillermo.",
      ("Luis Guillermo", "[is] President [of]", "Costa Rica"))*/

  test(
    "AdjectiveDescriptorExtractor_more_8",
    "New Zealand President Luis Guillermo.",
    ("Luis Guillermo", "[is] President [of]", "New Zealand")
  )

  /*test("AdjectiveDescriptorExtractor_more_9",
      "North Korean President Obama.",
      ("Obama", "[is] President [of]", "North Korea"))*/

  test(
    "AdjectiveDescriptorExtractor_more_10",
    "New York governor Eliot Spitzer.",
    ("Eliot Spitzer", "[is] governor [of]", "New York City")
  )

  test(
    "AdjectiveDescriptorExtractor_more_11",
    "Seattle Badminton Player Michael.",
    ("Michael", "[is] Badminton Player [from]", "Seattle")
  )

  /*test("AdjectiveDescriptorExtractor_more_12",
      "Badminton Player Michael.",
      ("Michael", "[is] Player [of]", "Badminton"))*/

  test(
    "AdjectiveDescriptorExtractor_more_13",
    "West Bengali chief minister Mamata Banerjee.",
    ("Mamata Banerjee", "[is] chief minister [of]", "West Bengal")
  )

  test(
    "AdjectiveDescriptorExtractor_demonym",
    "Indian President Pranab Mukherjee gave a speech today.",
    ("Pranab Mukherjee", "[is] President [of]", "India")
  )

  test(
    "PossessiveExtractor_[of]",
    "United States' President Barack Obama was in a debate on Wednesday.",
    ("Barack Obama", "[is] President [of]", "United States")
  )

  test(
    "PossessiveExtractor_[from]",
    "India's player Tendulkar received the Arjuna Award in 1994.",
    ("Tendulkar", "[is] player [from]", "India")
  )

  test(
    "PossessiveExtractor_more_1",
    "New Zealand's President Luis Guillermo.",
    ("Luis Guillermo", "[is] President [of]", "New Zealand")
  )

  test(
    "PossessiveAppositiveExtractor_[of]",
    "United States' President, Barack Obama, was in a debate on Wednesday.",
    ("Barack Obama", "[is] President [of]", "United States")
  )

  test(
    "PossessiveAppositiveExtractor_[from]",
    "India's player, Tendulkar, received the Arjuna Award in 1994.",
    ("Tendulkar", "[is] player [from]", "India")
  )

  test(
    "PossessiveIsExtractor_[of]",
    "America's President is Barack Obama.",
    ("Barack Obama", "is President [of]", "America")
  )

  test(
    "PossessiveIsExtractor_[from]",
    "India's Player is Sachin.",
    ("Sachin", "is Player [from]", "India")
  )

  test(
    "IsPossessiveExtractor_[of]",
    "Barack Obama is America's President.",
    ("Barack Obama", "is President [of]", "America")
  )

  test(
    "IsPossessiveExtractor_[from]",
    "Tendulkar is India's player.",
    ("Tendulkar", "is player [from]", "India")
  )

  test(
    "IsPossessiveExtractor_more_1",
    "Luis Guillermo is New Zealand's President.",
    ("Luis Guillermo", "is President [of]", "New Zealand")
  )

  test(
    "IsPossessiveExtractor_pronoun",
    "He is America's President.",
    ("He", "is President [of]", "America")
  )

  test(
    "OfIsExtractor",
    "The President of the United States is Barack Obama.",
    ("Barack Obama", "is The President of", "the United States")
  )

  test(
    "OfIsExtractor_pronoun",
    "The President of the United States is he.",
    ("he", "is The President of", "the United States")
  )

  test(
    "OfCommaExtractor",
    "The Chairperson of the Commission of the African Union, Jean Ping, on Tuesday...",
    ("Jean Ping", "[is] The Chairperson of", "the Commission of the African Union")
  )

  test(
    "OfCommaExtractor_pronoun",
    "The Chairperson of the Commission of the African Union, he, on Tuesday...",
    ("he", "[is] The Chairperson of", "the Commission of the African Union")
  )

  test(
    "PossessiveReverseExtractor_[of]",
    "Barack Obama, America's President, gave a debate on Wednesday.",
    ("Barack Obama", "[is] President [of]", "America")
  )

  test(
    "PossessiveReverseExtractor_[from]",
    "Tendulkar, India's player, received the Arjuna Award in 1994.",
    ("Tendulkar", "[is] player [from]", "India")
  )

  test(
    "PossessiveReverseExtractor_pronoun",
    "He, America's President, gave a debate on Wednesday.",
    ("He", "[is] President [of]", "America")
  )

  test(
    "ProperNounAdjectiveExtractor_[of]",
    "Barack Obama, the US President, gave a debate on Wednesday.",
    ("Barack Obama", "[is] the President [of]", "United States")
  )

  test(
    "ProperNounAdjectiveExtractor_[from]",
    "Tendulkar, the Indian player, received the Arjuna Award in 1994.",
    ("Tendulkar", "[is] the player [from]", "India")
  )

  test(
    "ProperNounAdjectiveExtractor_pronoun",
    "He, the Indian player, received the Arjuna Award in 1994.",
    ("He", "[is] the player [from]", "India")
  )
}

