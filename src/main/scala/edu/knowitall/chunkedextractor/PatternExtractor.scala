package edu.knowitall.chunkedextractor

import java.util.regex.Pattern
import com.google.common.base.{ Function => GuavaFunction }
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.tool.stem.Lemmatized
import edu.knowitall.openregex
import edu.washington.cs.knowitall.logic.{ Expression => LExpression }
import edu.washington.cs.knowitall.logic.LogicExpression
import scala.language.implicitConversions

object PatternExtractor {
  type Token = Lemmatized[ChunkedToken]
  object Token {
    implicit def patternTokenAsToken(lemmatized: PatternExtractor.Token): edu.knowitall.tool.tokenize.Token = lemmatized.token
  }

  implicit def guavaFromFunction[A, B](f: A => B) = new GuavaFunction[A, B] {
    override def apply(a: A) = f(a)
  }

  implicit def logicArgFromFunction[T](f: T => Boolean) = new LExpression.Arg[T] {
    override def apply(token: T) = f(token)
  }

  def compile(pattern: String) =
    openregex.Pattern.compile(pattern, (expression: String) => {
      val valuePattern = Pattern.compile("([\"'])(.*)\\1")

      val deserializeToken: String => (Token => Boolean) = (argument: String) => {
        val Array(base, value) = argument.split("=")

        val matcher = valuePattern.matcher(value)
        if (!matcher.matches()) {
          throw new IllegalArgumentException("Value not enclosed in quote (\") or ('): " + argument)
        }

        val string = matcher.group(2)

        base match {
          case "string" => new Expressions.StringExpression(string)
          case "lemma" => new Expressions.LemmaExpression(string)
          case "pos" => new Expressions.PostagExpression(string)
          case "chunk" => new Expressions.ChunkExpression(string)
        }
      }

      val logic: LogicExpression[Token] =
        LogicExpression.compile(expression, deserializeToken andThen logicArgFromFunction[Token])

      (token: Token) => {
        logic.apply(token)
      }
    })

  def intervalFromGroup(group: openregex.Pattern.Group[_]): Interval = {
    val range: Range = group.interval

    if (range.start == -1 || range.end == -1) {
      Interval.empty
    } else {
      Interval.open(range.start, range.end)
    }
  }
}

abstract class BinaryPatternExtractor[B](val expression: openregex.Pattern[PatternExtractor.Token])
    extends Extractor[Seq[PatternExtractor.Token], B] {
  def this(pattern: String) = this(PatternExtractor.compile(pattern))

  def apply(tokens: Seq[PatternExtractor.Token]): Iterable[B] = {
    val matches = expression.findAll(tokens.toList);

    for (
      m <- matches;
      extraction = buildExtraction(tokens, m);

      if !filterExtraction(extraction)
    ) yield extraction.get
  }

  protected def filterExtraction(extraction: Option[B]): Boolean =
    extraction match {
      case None => true
      case _ => false
    }

  protected def buildExtraction(tokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]): Option[B]
}
