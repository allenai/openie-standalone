package edu.knowitall
package chunkedextractor

import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.stem.MorphaStemmer
import scala.language.implicitConversions

class Nesty
    extends BinaryPatternExtractor[Nesty.ExtractionInstance](Nesty.pattern) {

  lazy val reverb = new ReVerb

  override def apply(tokens: Seq[PatternExtractor.Token]): Iterable[Nesty.ExtractionInstance] = {
    val reverbExtractions = reverb.extract(tokens.map(_.token))
    this.apply(tokens, reverbExtractions.map(_.extr))
  }

  def apply(tokens: Seq[PatternExtractor.Token], reverbExtractions: Iterable[BinaryExtraction[ChunkedToken]]): Iterable[Nesty.ExtractionInstance] = {
    val transformed =
      tokens.iterator.zipWithIndex.map {
        case (t, i) =>
          val ext = reverbExtractions.flatMap {
            case extr if (extr.arg1.tokenInterval.start == i) =>
              Some("B-ARG1")
            case extr if (extr.arg1.tokenInterval superset Interval.singleton(i)) =>
              Some("I-ARG1")
            case extr if (extr.rel.tokenInterval.start == i) =>
              Some("B-REL")
            case extr if (extr.rel.tokenInterval superset Interval.singleton(i)) =>
              Some("I-REL")
            case extr if (extr.arg2.tokenInterval.start == i) =>
              Some("B-ARG2")
            case extr if (extr.arg2.tokenInterval superset Interval.singleton(i)) =>
              Some("I-ARG2")
            case _ => None
          }.mkString(":")

          t.copy(token = ChunkedToken(t.token.chunk + ":" + ext, t.token.postag, t.token.string, t.token.offset))
      }.toSeq

    super.apply(transformed)
  }

  override def buildExtraction(tokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
    implicit def patternTokenAsToken2(lemmatized: PatternExtractor.Token): edu.knowitall.tool.tokenize.Token = lemmatized.token
    val relation = ExtractionPart.fromSentenceTokens[Nesty.Token](tokens.map(_.token), PatternExtractor.intervalFromGroup(m.group("baseRelation").get))

    val extr = new Nesty.NestedExtraction(
      ExtractionPart.fromSentenceTokens[Nesty.Token](tokens.map(_.token), PatternExtractor.intervalFromGroup(m.group("arg1").get)),
      ExtractionPart.fromSentenceTokens[Nesty.Token](tokens.map(_.token), PatternExtractor.intervalFromGroup(m.group("nestedRelation").get)),
      new BinaryExtraction[Nesty.Token](
        ExtractionPart.fromSentenceTokens[Nesty.Token](tokens.map(_.token), PatternExtractor.intervalFromGroup(m.group("baseArg1").get)),
        relation,
        ExtractionPart.fromSentenceTokens[Nesty.Token](tokens.map(_.token), PatternExtractor.intervalFromGroup(m.group("baseArg2").get))
      )
    )

    Some(new Nesty.ExtractionInstance(extr, tokens.map(_.token)))
  }
}

object Nesty {
  type Token = ChunkedToken

  class ExtractionInstance(override val extr: Nesty.NestedExtraction, sent: Seq[Nesty.Token])
    extends BinaryExtractionInstance(extr, sent)

  class NestedExtraction(arg1: ExtractionPart[Token], rel: ExtractionPart[Token], val nested: BinaryExtraction[Nesty.Token])
      extends BinaryExtraction(arg1, rel, new ExtractionPart[Token](nested.text, nested.tokens, nested.tokenInterval)) {
  }

  val verbs = List("be", "say", "have", "believe",
    "tell", "suggest", "argue", "indicate", "claim", "note", "know",
    "show", "state", "find", "conclude", "report", "means", "announce",
    "think", "warn", "write", "add", "demonstrate", "appear", "reveal",
    "agree", "assert", "acknowledge", "hope", "realize", "fear",
    "suspect", "mean", "feel", "see", "explain", "confirm", "mention",
    "ask", "seem", "observe", "estimate", "admit", "recognize",
    "allege", "insist", "require", "discover", "declare", "imply",
    "give", "deny", "understand", "express", "recommend", "worry",
    "point", "maintain", "contend", "stress", "prove", "demand",
    "learn", "hear", "assume", "predict", "inform", "complain",
    "reflect", "provide", "hold", "testify", "request", "notice",
    "assess", "remind", "wish", "speculate", "raise", "forget",
    "decide", "promise", "pray", "expect", "ensure", "challenge",
    "recall", "present", "determine", "doubt", "concede", "reply",
    "reject", "propose", "make", "include", "emphasize", "concern",
    "charge", "take", "remains", "receive", "exist", "assure", "teach",
    "rule", "respond", "remember", "reinforce", "happen", "do",
    "caution", "underscore", "turn", "threaten", "signal", "repeat",
    "release", "relate", "presume", "discuss", "confess", "bear",
    "advise", "trust", "reiterate", "disclose", "dictate", "convince",
    "consider", "anticipate", "answer", "accept", "will", "swear",
    "regret", "order", "issue", "increase", "illustrate", "ignore",
    "establish", "continue", "certify", "bemoan", "affirm", "surface",
    "support", "seize", "sa", "refute", "prompt", "posit", "offer",
    "live", "leave", "lack", "invite", "imagine", "highlight",
    "follow", "fail", "examine", "build", "boast", "begin", "urge",
    "theorize", "represent", "remain", "reason", "proclaim", "pretend",
    "postulate", "perceive", "outline", "mark", "list", "judge",
    "guarantee", "grasp", "go", "face", "emerge", "echo", "dispute",
    "deserve", "describe", "decree", "control", "contain", "cite",
    "cause", "brag", "bolster", "aver", "alert", "wwa", "vow", "voice",
    "use", "underline", "thank", "survey", "strengthen", "spread",
    "specify", "speak", "signify", "sense", "send", "rebuff", "read",
    "reach", "put", "protest", "prescribe", "postpone", "pledge",
    "pick", "persuade", "operate", "object", "need", "nag", "muslim",
    "mirror", "lose", "lessen", "keep", "get", "generate", "form",
    "explore", "eply", "embody", "dismiss", "disagree", "detonate",
    "denounce", "come", "claus", "clarify", "circulate", "call",
    "brush", "bring", "alarm")

  final val nestedRelationPatternString =
    "(?:(?:(?:<pos='RB'>? <pos='MD'>? (?:<chunk='.-VP.*' & pos='VB.?' & lemma='" + verbs.mkString("|") + "'>) <pos='RP|TO'>? <pos='RB'>?)+" +
      "(?:(?:<chunk='B-NP.*'> <chunk='I-NP.*'>*)*)) |" +
      "(?:(?:<pos='RB'>? <pos='MD'>? (?:<chunk='.-VP.*' & pos='VB.?'>) <pos='RP|TO'>? <pos='RB'>?)+" +
      "(?:(?:<chunk='B-NP.*'> <chunk='I-NP.*'>*)* <string='that'>)))"

  // The pattern for a nested relation is defined as...
  final val pattern =
    // A noun phrase, with optional PP attachment (don't allow because)
    "(<arg1>:<chunk='B-NP.*' & !string='who|which'> <chunk='I-NP.*'>* (?: <pos='IN|TO' & !string='so|because'> <chunk='B-NP.*'> <chunk='I-NP.*'>*)* <string='who|which'>?)" +
      // Followed by a nested relation phrase (with negative lookahead to
      // prevent the next noun phrase from "absorbing" part of the nested
      // relation)
      "(<nestedRelation>:" + nestedRelationPatternString + "+)" +
      // Followed by another noun phrase
      // Sometimes OpenNLP classifies "that" as the start of the chunk so we
      // allow the chunk to start with I-NP.
      "(<baseArg1>:<chunk='.*:B-ARG1.*'> <chunk='.*:I-ARG1.*'>*)" +
      // Followed by a base relation phrase (again with negative lookahead)
      "(<baseRelation>:<chunk='.*:B-REL.*'> <chunk='.*:I-REL.*'>*)" +
      // Followed by another noun phrase (possibly starting from inside)
      "(<baseArg2>:<chunk='.*:B-ARG2.*'> <chunk='.*:I-ARG2.*'>*)"

  def main(args: Array[String]) {
    System.out.println("Creating the nested relation extractor... ")
    val nesty = new Nesty()

    if (args.length > 0 && (args(0) equals "--pattern")) {
      System.out.println(Nesty.pattern)
    } else {
      System.out.println("Creating the sentence chunker... ")
      val chunker = new OpenNlpChunker()
      val stemmer = new MorphaStemmer()
      System.out.println("Please enter a sentence:")

      try {
        for (line <- scala.io.Source.stdin.getLines) {
          val chunked = chunker.chunk(line)
          val tokens = chunked map stemmer.lemmatizeToken

          for (extraction <- nesty(tokens)) {
            println(extraction.extr)
          }

          System.out.println()
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          System.exit(2)
      }
    }
  }
}
