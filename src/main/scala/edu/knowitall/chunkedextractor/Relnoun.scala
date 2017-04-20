package edu.knowitall
package chunkedextractor

import edu.knowitall.tool.chunk.ChunkedToken
import edu.knowitall.collection.immutable.Interval
import edu.knowitall.tool.chunk.OpenNlpChunker
import edu.knowitall.tool.stem.MorphaStemmer
import edu.knowitall.tool.stem.Lemmatized
import scala.io.Source
import edu.knowitall.common.{ Resource, Timing }
import scala.collection.JavaConverters._
import edu.knowitall.openregex
import edu.washington.cs.knowitall.regex.Match
import edu.washington.cs.knowitall.regex.RegularExpression
import Relnoun._
import java.io.PrintStream
import java.io.PrintWriter
import java.io.File
import java.nio.charset.MalformedInputException

class Relnoun(val encloseInferredWords: Boolean = true, val includeReverbRelnouns: Boolean = true, val includeUnknownArg2: Boolean = false)
    extends Extractor[Seq[PatternExtractor.Token], BinaryExtractionInstance[Relnoun.Token]] {

  val subextractors: Seq[BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]]] = Seq(
    new AppositiveExtractor(this.encloseInferredWords, this.includeUnknownArg2),
    new AppositiveExtractor2(this.encloseInferredWords, this.includeUnknownArg2),
    new AdjectiveDescriptorExtractor(this.encloseInferredWords, this.includeUnknownArg2),
    new PossessiveExtractor(this.encloseInferredWords, this.includeUnknownArg2),
    new PossessiveAppositiveExtractor(this.encloseInferredWords, this.includeUnknownArg2),
    new PossessiveIsExtractor(this.encloseInferredWords, this.includeUnknownArg2),
    new IsPossessiveExtractor(this.encloseInferredWords, this.includeUnknownArg2),
    new OfIsExtractor(this.encloseInferredWords, this.includeUnknownArg2),
    new OfCommaExtractor(this.encloseInferredWords, this.includeUnknownArg2),
    new PossessiveReverseExtractor(this.encloseInferredWords, this.includeUnknownArg2),
    new ProperNounAdjectiveExtractor(this.encloseInferredWords, this.includeUnknownArg2)
  ) ++
    (if (includeReverbRelnouns) Seq(new VerbBasedExtractor(this.encloseInferredWords, this.includeUnknownArg2)) else Seq.empty)

  def apply(tokens: Seq[Lemmatized[ChunkedToken]]): Seq[BinaryExtractionInstance[Relnoun.Token]] = {
    val extrs = for (
      sub <- subextractors;
      extr <- sub(tokens)
    ) yield extr

    //removing duplicate [UNKNOWN] extractions
    var final_extrs = Seq.empty[BinaryExtractionInstance[Relnoun.Token]]

    for (extr1 <- extrs) {

      if (extr1.extr.arg2.text.equals(UNKNOWN)) {
        val arg1_1 = extr1.extr.arg1.text
        val rel_1 = extr1.extr.rel.text

        var isDuplicate = false
        for (extr2 <- extrs) {
          val arg1_2 = extr2.extr.arg1.text
          val rel_2 = extr2.extr.rel.text
          val arg2_2 = extr2.extr.arg2.text

          if (arg1_1.equals(arg1_2) && !arg2_2.equals(UNKNOWN)) isDuplicate = true
        }

        if (!isDuplicate) final_extrs = final_extrs :+ (extr1)
      } else {
        final_extrs = final_extrs :+ (extr1)
      }
    }

    final_extrs
  }
}

object Relnoun {

  type Token = ChunkedToken

  val demonyms_url = Option(this.getClass.getResource("demonyms.csv")).getOrElse {
    throw new IllegalArgumentException("Could not load demonyms.csv")
  }

  val demonyms_iter = Source.fromInputStream(demonyms_url.openStream(), "UTF-8").getLines().map(_.split(","))

  val prp_mapping_url = Option(this.getClass.getResource("prp_mapping.csv")).getOrElse {
    throw new IllegalArgumentException("Could not load prp_mapping.csv")
  }

  val prp_mapping_iter = Source.fromInputStream(prp_mapping_url.openStream(), "UTF-8").getLines().map(_.split(","))

  val nouns_url = Option(this.getClass.getResource("nouns.txt")).getOrElse {
    throw new IllegalArgumentException("Could not load nouns.txt")
  }

  val ofNouns_url = Option(this.getClass.getResource("nouns_of.txt")).getOrElse {
    throw new IllegalArgumentException("Could not load nouns_of.txt")
  }

  val orgsWords_url = Option(this.getClass.getResource("org_words.txt")).getOrElse {
    throw new IllegalArgumentException("Could not load org_words.txt")
  }

  val relnoun_prefixes_url = Option(this.getClass.getResource("relnoun_prefixes.txt")).getOrElse {
    throw new IllegalArgumentException("Could not load relnoun_prefixes.txt")
  }

  var prp_mapping_map = scala.collection.mutable.Map[String, String]()
  while (prp_mapping_iter.hasNext) {
    val arr = prp_mapping_iter.next
    prp_mapping_map += arr(0) -> arr(1)
  }

  var demonyms_map = scala.collection.mutable.Map[String, String]()
  while (demonyms_iter.hasNext) {
    val arr = demonyms_iter.next
    demonyms_map += arr(0) -> arr(1)
    demonyms_map += ("South" + " " + arr(0)) -> ("South" + " " + arr(1))
    demonyms_map += ("North" + " " + arr(0)) -> ("North" + " " + arr(1))
    demonyms_map += ("East" + " " + arr(0)) -> ("East" + " " + arr(1))
    demonyms_map += ("West" + " " + arr(0)) -> ("West" + " " + arr(1))
    demonyms_map += ("Southern" + " " + arr(0)) -> ("Southern" + " " + arr(1))
    demonyms_map += ("Northern" + " " + arr(0)) -> ("Northern" + " " + arr(1))
    demonyms_map += ("Eastern" + " " + arr(0)) -> ("Eastern" + " " + arr(1))
    demonyms_map += ("Western" + " " + arr(0)) -> ("Western" + " " + arr(1))
  }

  val (demonyms_key, demonyms_val) = demonyms_map.toSeq.unzip
  val locations = (demonyms_key ++ demonyms_val)

  val nounChunk = "(?:<chunk=\"B-NP\"> <chunk=\"I-NP\">*)"
  val properNounChunk = "(?:<chunk=\"B-NP\" & pos=\"NNPS?\"> <chunk=\"I-NP\">*) | (?:<chunk=\"B-NP\" & pos=\"NNPS?\"> <chunk=\"I-NP\">* <chunk=\"I-NP\" & pos=\"NNPS?\"> <chunk=\"I-NP\">*)";
  val properRelnounChunk = "(<relnoun>: <string=\"${relnoun}\" & pos=\"NN|NNP\">+) | (<ofNoun>: <string=\"${ofNoun}\" & pos=\"NN|NNP\">+)"

  val pronoun = "<pos=\"PRP\">"
  val pronoun_possessive = "<pos=\"PRP\\$\">"

  val relnoun = "(string='${relnoun}' | string='${ofNoun}')";
  val relnoun_prefix = "string=\"${relnoun_prefixes}\""
  val relnoun_prefix_noPrefixCheck = "!(string=\"${demonyms}\")"

  val relnoun_prefix_pos = " & pos=\"JJS?|VBDS?|VBNS?|NNS?|NNPS?|RBS?\" & !(string=\"${orgwords}\")"

  val relnoun_prefix_tagged = "<" + relnoun_prefix + relnoun_prefix_pos + ">*"
  val relnoun_prefix_tagged_noPrefixCheck = "<" + relnoun_prefix_noPrefixCheck + relnoun_prefix_pos + ">*"

  val input_nouns = Source.fromInputStream(nouns_url.openStream(), "UTF-8").getLines().map(_.trim()).toArray
  val ex_nouns = input_nouns.map { x => "ex-" + x }
  val nouns = input_nouns ++ ex_nouns

  private final val orgs = Source.fromInputStream(orgsWords_url.openStream(), "UTF-8").getLines().map(_.trim()).toArray
  private final val ofNouns = Source.fromInputStream(ofNouns_url.openStream(), "UTF-8").getLines().map(_.trim()).toArray
  private final val adjs = Source.fromInputStream(relnoun_prefixes_url.openStream(), "UTF-8").getLines().map(_.trim()).toArray

  val UNKNOWN = "[UNKNOWN]"
  val arg1_notAllowed = List("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
    "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

  abstract class BaseExtractor {
    val pattern: String
  }

  def patternReplace(extractor: BaseExtractor) =
    extractor.pattern
      .replace("${relnoun}", nouns.mkString("|"))
      .replace("${ofNoun}", ofNouns.mkString("|"))
      .replace("${orgwords}", orgs.mkString("|"))
      .replace("${relnoun_prefixes}", adjs.mkString("|"))
      .replace("${demonyms}", locations.mkString("|"))

  protected def finalizeExtraction[B](m: openregex.Pattern.Match[PatternExtractor.Token], encloseInferredWords: Boolean, patternTokens: Seq[PatternExtractor.Token],
    arg1: ExtractionPart[ChunkedToken], relation: ExtractionPart[ChunkedToken], arg2: ExtractionPart[ChunkedToken],
    includeUnknownArg2: Boolean, includeIs: Boolean, includePost: Boolean): Option[BinaryExtractionInstance[Relnoun.Token]] = {

    val tokens = patternTokens.map(_.token)

    var isValidExtraction = true
    var arg2_modified = arg2

    //replacing prp
    val prpMappingVal = Relnoun.prp_mapping_map.get(arg2.text)
    arg2_modified = prpMappingVal match {
      case Some(s) => ExtractionPart.fromSentenceTokens(tokens, arg2_modified.tokenInterval, s)
      case None => arg2_modified
    }

    //Setting arg2 as [UNKNOWN] if not present or if "its"(possible came by AdjectiveDescriptorExtractor(prp))
    if ((arg2.text == "" || arg2.text == "its") && includeUnknownArg2) arg2_modified = ExtractionPart.fromSentenceTokens(tokens, relation.tokenInterval, UNKNOWN)
    if (arg1.text == "it" || arg2_modified.text == "its") isValidExtraction = false

    //replacing demonyms

    val demonymVal = Relnoun.demonyms_map.get(arg2.text)
    arg2_modified = demonymVal match {
      case Some(s) => ExtractionPart.fromSentenceTokens(tokens, arg2_modified.tokenInterval, s)
      case None => arg2_modified
    }

    if (arg2_modified.text == "") isValidExtraction = false

    //remove extractions with arg1 as Sunday,Monday..,January, February...
    if (arg1_notAllowed.contains(arg1.text)) {
      isValidExtraction = false
    }

    if (arg1_notAllowed.contains(arg2_modified.text)) {
      isValidExtraction = false
    }

    if (!isValidExtraction) {
      None
    }
    else {
      val inferredIs = if (encloseInferredWords) "[is]" else "is"

      var rel_text = relation.text
      if (includeIs) {
        rel_text = inferredIs + " " + rel_text
      }
      if (includePost) {
        rel_text = rel_text + " " + inferred_post(m, encloseInferredWords, arg2_modified.text)
      }
      val relation_modified = ExtractionPart.fromSentenceTokens(tokens, relation.tokenInterval, rel_text)

      val extr = new BinaryExtraction(arg1, relation_modified, arg2_modified)
      Some(new BinaryExtractionInstance[Relnoun.Token](extr, tokens))
    }
  }

  def inferred_post(m: openregex.Pattern.Match[PatternExtractor.Token], encloseInferredWords: Boolean, arg2_text: String): String = {
    val inferredOf = if (encloseInferredWords) {
      "[of]"
    } else {
      "of"
    }

    val inferredFrom = if (encloseInferredWords) {
      "[from]"
    }
    else {
      "from"
    }

    if (!locations.contains(arg2_text)) {
      inferredOf // if arg2 is not a demonym, use inferredOf
    }
    else {
      m.group("relnoun") match {
        case None => inferredOf
        case _ => inferredFrom
      }
    }
  }

  /** Extracts relations from phrases such as:
    * "Barack Obama is the president of the United States."
    * (Barack Obama, is the president of, the United States)
    *
    * @author schmmd
    */
  class VerbBasedExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(VerbBasedExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(2)), m.groups(2).tokens.map(_.token.string).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(3)))

      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, false, false)
    }
  }

  object VerbBasedExtractor extends BaseExtractor {
    val pattern =
      // {nouns} (no preposition)
      "(" + nounChunk + ")" +
        // {be} {adverb} {adjective} {relnoun} {prep}
        "(<lemma='be'> <pos='DT'>? <" + relnoun + "> <pos='IN'>)" +
        // {proper np chunk}
        "(" + nounChunk + ")";
  }

  /** Extracts relations from phrases such as:
    * "Chris Curran, a lawyer for Al-Rajhi Banking."
    * (Chris Curran, [is] a lawyer for, Al-Rajhi Banking)
    *
    * @author schmmd
    *
    */
  class AppositiveExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(AppositiveExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(2)), m.groups(2).tokens.map(_.token.string).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(3)))

      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, true, false)
    }
  }

  object AppositiveExtractor extends BaseExtractor {
    val pattern: String =
      // {proper noun}
      "(" + properNounChunk + "|" + pronoun + ")" +
        // {comma}
        "<string=\",\">" +
        // {article}
        "(<string=\"a|an|the\">*" +
        // {adjective or noun}
        "<pos=\"JJ|VBD|VBN|NN|NNP\">*" +
        // {relnoun} {preposition}
        "<" + relnoun + "& pos=\"NN|NNP\"> <pos=\"IN\">)" +
        "(<chunk=\".-NP\"> <chunk=\"I-NP\">*)"
  }

  /** Extracts relations from phrases such as:
    * "Lauren Faust, a cartoonist,"
    * (Lauren Faust; [is]; a cartoonist)
    */
  class AppositiveExtractor2(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(AppositiveExtractor2)
      ) {

    private val inferredIs = if (this.encloseInferredWords) "[is]" else "is"

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)), this.inferredIs)

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(2)))

      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, false, false)
    }
  }

  object AppositiveExtractor2 extends BaseExtractor {
    val pattern: String =
      // {proper noun}
      "(" + properNounChunk + "|" + pronoun + ")" +
        // {comma}
        "<string=\",\">" +
        // adverb
        "<pos=\"RB\">?" +
        // {article}
        "(<string=\"a|an|the\">*" +
        // {adjective or noun}
        "<pos=\"JJ|NN\">*" +
        // {relnoun} {preposition}
        relnoun_prefix_tagged_noPrefixCheck + " <" + relnoun + "& pos=\"NN|NNP\">)" +
        "<string=\",|.\">"
  }

  /** Extracts relations from phrases such as:
    * "United States President Barack Obama"
    * (Barack Obama; [is] President [of]; United States)
    *
    * "Indian player Sachin Tendulkar"
    * (Sachin Tendulkar; [is] player [from]; India)
    *
    * @author schmmd
    *
    */
  class AdjectiveDescriptorExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(AdjectiveDescriptorExtractor)
      ) {

    private val inferredIs = if (this.encloseInferredWords) "[is]" else "is"

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)

      val adjectiveGroup = m.group("adj").get match {
        case g if g.text.isEmpty => None
        case g => Some(g)
      }

      val adjective = adjectiveGroup map { adj =>
        adj.tokens.map(_.token.string).mkString(" ")
      }

      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("pred").get), inferredIs + adjective.map(" " + _ + " ").getOrElse(" ") +
        m.group("pred").get.tokens.map(_.token.string).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("arg1").get))
      var arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("arg2").get))

      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, false, true)
    }
  }

  //arg1: shall contain atleast one nnp that is not a {orgword}
  //arg2: allow “relnoun_prefixes” followed by NNP ("Foreign Ministry spokesman Qin Gang.") 
  //arg2: allow pos=JJ only if the word is in the list of demonyms ("outgoing Chairperson Bonnie Peng.")
  object AdjectiveDescriptorExtractor extends BaseExtractor {
    val pattern =
      // {adjective}
      "(<adj>: <pos=\"JJ|VBD|VBN|RB\" & !(string=\"${demonyms}\") & !(string=\"${orgwords}\")>*)" +
        "(((<arg2>: (<pos=\"NNPS?\">* " + "<!" + relnoun_prefix + " & pos=\"NNPS?\" >+) | (" + pronoun_possessive + ")? )" +
        "(<pred>: " + relnoun_prefix_tagged + properRelnounChunk + "))" + "|" +
        "((<arg2>: (<pos=\"NNPS?|JJ\" & string=\"${demonyms}\" >+) )" +
        "(<pred>: " + relnoun_prefix_tagged_noPrefixCheck + properRelnounChunk + ")))" +
        "<string=\",\">?" + // {comma}
        "(<arg1>: <pos=\"nn\">* <!(string=\"${orgwords}\") & pos=\"nnp\">+ <pos=\"nn|nnp\">*)";
  }

  /** Extracts relations from phrases such as:
    * "Hakani's nephew John"
    * (John, [is] nephew [of], Hakani)
    *
    * "India's player Tendulkar"
    * (Tendulkar; [is] player [from]; India)
    *
    * @author schmmd
    *
    */
  class PossessiveExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(PossessiveExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(2)), m.groups(2).tokens.map(_.token.string).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("arg1").get))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)))
      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, true, true)
    }
  }

  object PossessiveExtractor extends BaseExtractor {
    val pattern =
      // {proper noun} (no preposition)
      "(<pos=\"NNS?\">* <pos=\"NNPS?\">+ <pos=\"NNS?|NNPS?\">*)" +
        // {possessive}
        "<pos=\"POS\">" +
        // {adverb} {adjective} {relnoun}
        "(<pos=\"RB\">*" + relnoun_prefix_tagged_noPrefixCheck + properRelnounChunk + ")" +
        // {proper noun} (no preposition)
        "(<arg1>: <pos=\"NN\">* <!(string=\"${orgwords}\") & pos=\"NNP\">+ <pos=\"NN|NNP\">*)";
  }

  /** Extracts relations from phrases such as:
    * "AUC's leader, Carlos Castano"
    * (Carlos Castano, [is] leader [of], AUC)
    *
    * "India's player, Tendulkar"
    * (Tendulkar; [is] player [from]; India)
    *
    * @author schmmd
    *
    */
  class PossessiveAppositiveExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(PossessiveAppositiveExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(2)), m.groups(2).tokens.map(_.token.string).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("arg1").get))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)))
      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, true, true)
    }
  }

  object PossessiveAppositiveExtractor extends BaseExtractor {
    val pattern: String =
      // {nouns} (no preposition)
      "(<pos=\"NNS?|NNPS?\">+)" +
        // {possessive}
        "<pos=\"POS\">" +
        // {adverb} {adjective} {relnoun}
        "(<pos=\"RB\">* <pos=\"JJ|VBD|VBN\">*" + relnoun_prefix_tagged_noPrefixCheck + properRelnounChunk + ")" +
        // {comma}
        "<string=\",\">" +
        // {proper np chunk}
        "(<arg1>:" + properNounChunk + ")";
  }

  /** Extracts relations from phrases such as:
    * "AUC's leader is Carlos Castano"
    * (Carlos Castano, is leader [of], AUC)
    *
    * "India's Player is Sachin."
    * (Sachin; is Player [from]; India)
    *
    * @author schmmd
    */
  class PossessiveIsExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(PossessiveIsExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(2)), m.group("lemma_be").get.tokens.map(_.token.string).mkString(" ") + " " + m.groups(2).tokens.map(_.token.string).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("arg1").get))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)))
      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, false, true)
    }
  }

  object PossessiveIsExtractor extends BaseExtractor {
    val pattern =
      // {nouns} (no preposition)
      "(<pos='DT'>? <pos='RB.*'>* <pos='JJ.*'>* <pos='NNS?|NNPS?'>+)" +
        // {possessive}
        "<pos='POS'>" +
        // {adverb} {adjective} {relnoun}
        "(<pos='RB'>* <pos='JJ|VBD|VBN'>*" + relnoun_prefix_tagged_noPrefixCheck + properRelnounChunk + ")" +
        // be
        "(<lemma_be>: <lemma=\"be\">)" +
        // {proper np chunk}
        "(<arg1>:" + properNounChunk + ")";
  }

  /** Extracts relations from phrases such as:
    * "Barack Obama is America's President"
    * (Barack Obama; is President [of]; America)
    *
    * "Tendulkar is India's player."
    * (Tendulkar; is player [from]; India)
    *
    * @author schmmd
    */
  class IsPossessiveExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(IsPossessiveExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(4)), m.groups(2).tokens.map(_.token.string).mkString(" ") + " " + m.groups(4).tokens.map(_.token.string).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(3)))
      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, false, true)
    }
  }

  object IsPossessiveExtractor extends BaseExtractor {
    val pattern =
      // {nouns} (no preposition)
      "(" + properNounChunk + "|" + pronoun + ")" +
        "(<lemma=\"be\">)" +
        "(<pos='NNS?|NNPS?'>+)" +
        "<pos='POS'>" +
        "(<pos=\"RB\">* <pos=\"JJ|VBD|VBN\">*" + relnoun_prefix_tagged_noPrefixCheck + properRelnounChunk + ")";
  }

  /** Extracts relations from phrases such as:
    * "the father of Michael is John"
    * (John; is the father of; Michael)
    *
    * @author schmmd
    */
  class OfIsExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(OfIsExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)), m.groups(3).tokens.map(_.token.string).mkString(" ") + " " + m.groups(1).tokens.map(_.token.string).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("arg1").get))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(2)))
      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, false, false)
    }
  }

  object OfIsExtractor extends BaseExtractor {
    val pattern =
      "(<chunk='B-NP'> <chunk='I-NP'>* <" + relnoun + "& pos='NN|NNP' & chunk='I-NP'> " +
        "<string='of'>) " +
        "(<chunk='.-NP'> <chunk='I-NP'>* <string='of'>? <chunk='.-NP'>? <chunk='I-NP'>*) " +
        "(<lemma='be'>) " +
        "(<arg1>: (<chunk='B-NP'> <chunk='I-NP'>*) |" + pronoun + ")";
  }

  /** Extracts relations from phrases such as:
    * "the father of Michael, John,"
    * (John; [is] the father of; Michael)
    *
    * @author harinder
    */
  class OfCommaExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(OfCommaExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)), m.groups(1).tokens.map(_.token.string).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(4)))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(2)))
      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, true, false)
    }
  }

  object OfCommaExtractor extends BaseExtractor {
    val pattern =
      "(<chunk='B-NP'> <chunk='I-NP'>* <" + relnoun + "& pos='NN|NNP' & chunk='I-NP'> " +
        "<string='of'>) " +
        "(<chunk='.-NP'> <chunk='I-NP'>* <string='of'>? <chunk='.-NP'>? <chunk='I-NP'>*) " +
        "(<string=\",\">) " +
        //{proper np chunk}
        "(" + properNounChunk + "|" + pronoun + ")" +
        "(<string=\",\" | string=\".\">) ";
  }

  /** Extracts relations from phrases such as:
    * "Mohammed Jamal, bin Laden's brother"
    * (Mohammed Jamal, [is] brother [of], bin Laden)
    *
    * "Tendulkar, India's player"
    * (Tendulkar; [is] player [from]; India)
    *
    * @author schmmd
    */
  class PossessiveReverseExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(PossessiveReverseExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(3)), m.groups(3).tokens.map(_.token.string).mkString(" "));

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(1)))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.groups(2)))
      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, true, true)
    }
  }

  object PossessiveReverseExtractor extends BaseExtractor {
    val pattern =
      // {proper noun} (no preposition)
      "(" + properNounChunk + "|" + pronoun + ")" +
        // comma
        "<string=\",\">" +
        // {np chunk}
        "(<chunk=\"B-NP\"> <chunk=\"I-NP\">*)" +
        // {possessive}
        "<pos=\"POS\">" +
        "(<pos=\"RB\">* <pos=\"JJ|VBD|VBN\">*" + relnoun_prefix_tagged_noPrefixCheck + properRelnounChunk + ")" +
        "(?:<!chunk=\"I-NP\">|$)";
  }

  /** Extracts relations from phrases such as:
    * "Obama, the US president."
    * (Obama, [is] president [of], United States)
    *
    * "Tendulkar, the Indian player."
    * (Tendulkar; [is] the player [from]; India)
    *
    * @author schmmd
    */
  class ProperNounAdjectiveExtractor(private val encloseInferredWords: Boolean, private val includeUnknownArg2: Boolean)
      extends BinaryPatternExtractor[BinaryExtractionInstance[Relnoun.Token]](
        patternReplace(ProperNounAdjectiveExtractor)
      ) {

    override def buildExtraction(patternTokens: Seq[PatternExtractor.Token], m: openregex.Pattern.Match[PatternExtractor.Token]) = {
      val tokens = patternTokens.map(_.token)
      val relation = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("pred").get),
        (m.groups(2).tokens.map(_.token.string) ++ m.group("pred").get.tokens.map(_.token.string)).mkString(" "))

      val arg1 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("arg1").get))
      val arg2 = ExtractionPart.fromSentenceTokens(tokens, PatternExtractor.intervalFromGroup(m.group("arg2").get))
      finalizeExtraction(m, encloseInferredWords, patternTokens, arg1, relation, arg2, includeUnknownArg2, true, true)
    }
  }

  object ProperNounAdjectiveExtractor extends BaseExtractor {
    val pattern =
      "(<arg1>: " + properNounChunk + "|" + pronoun + ")" +
        "<string=\",\">" +
        "(<string=\"a|an|the\"> <pos=\"JJ|VBD|VBN\">*)" +
        "(((<arg2>: (<pos=\"NNS?|NNPS?\">* " + "<!" + relnoun_prefix + " & pos=\"NNS?|NNPS?\" >+) )" +
        "(<pred>: " + relnoun_prefix_tagged + properRelnounChunk + "))" + "|" +
        "((<arg2>: (<pos=\"NNS?|NNPS?|JJ\" & string=\"${demonyms}\" >+) )" +
        "(<pred>: " + relnoun_prefix_tagged_noPrefixCheck + properRelnounChunk + ")))"
  }

  /** A class that represents the command line configuration
    * of the application.
    *
    * @param  inputFile  The file to use as input
    * @param  outputFile  The file to use as output
    */
  case class Config(
      inputFile: Option[File] = None,
      outputFile: Option[File] = None,
      encoding: String = "UTF-8",
      printPatterns: Boolean = false
  ) {

    /** Create the input source from a file or stdin.
      */
    def source() = {
      inputFile match {
        case Some(file) => Source.fromFile(file, encoding)
        case None => Source.fromInputStream(System.in, encoding)
      }
    }

    /** Create a writer to a file or stdout.
      */
    def writer() = {
      outputFile match {
        case Some(file) => new PrintWriter(file, encoding)
        case None => new PrintWriter(new PrintStream(System.out, true, encoding))
      }
    }
  }

  def main(args: Array[String]) {
    // definition for command-line argument parser
    val argumentParser = new scopt.OptionParser[Config]("openie") {
      opt[String]("input-file") action { (string, config) =>
        val file = new File(string)
        require(file.exists, "input file does not exist: " + file)
        config.copy(inputFile = Some(file))
      } text ("input file")
      opt[String]("ouput-file") action { (string, config) =>
        val file = new File(string)
        config.copy(outputFile = Some(file))
      } text ("output file (deprecated)")
      opt[String]("output-file") action { (string, config) =>
        val file = new File(string)
        config.copy(outputFile = Some(file))
      } text ("output file")
      opt[String]("encoding") action { (string, config) =>
        config.copy(encoding = string)
      } text ("Character encoding")
      opt[Unit]('p', "pattern") action { (_, config) =>
        config.copy(printPatterns = true)
      } text ("Prints the patterns")
    }

    argumentParser.parse(args, Config()) match {
      case Some(config) =>
        try {
          run(config)
        } catch {
          case e: MalformedInputException =>
            System.err.println(
              "\nError: a MalformedInputException was thrown.\n" +
                "This usually means there is a mismatch between what is expected and the input file.\n" +
                "Try changing the input file's character encoding to UTF-8 or specifying the correct character encoding for the input file with '--encoding'.\n"
            )
            e.printStackTrace()
        }
      case None => // usage will be shown
    }
  }

  def run(config: Config) {
    System.out.println("Creating the relational noun extractor... ")
    val relnoun = new Relnoun(true, true, true)
    val conf = confidence.RelnounConfidenceFunction.loadDefaultClassifier()

    config.inputFile.foreach { file =>
      System.err.println("Processing file: " + file)
    }

    if (config.printPatterns) {
      for (extractor <- relnoun.subextractors) {
        System.out.println(extractor.expression);
      }
    } else {

      System.err.println("Creating the sentence chunker... ")
      val chunker = new OpenNlpChunker()
      val stemmer = new MorphaStemmer()

      Timing.timeThen {

        Resource.using(config.source()) { source =>
          Resource.using(config.writer()) { writer =>
            try {
              for (line <- source.getLines) {
                val chunked = chunker.chunk(line);
                val tokens = chunked map stemmer.lemmatizeToken

                writer.println(line)
                for (inst <- relnoun(tokens)) {
                  writer.println(("%1.2f" format conf(inst)) + ": " + inst.extr);
                }

                writer.println();
                writer.flush();
              }
            } catch {
              case e: Exception =>
                e.printStackTrace()
                System.exit(2)
            }
          }
        }
      } { ns =>
        System.err.println("extraction completed in: " + Timing.Seconds.format(ns))
      }

      config.outputFile.foreach { file =>
        System.err.println("Output written to file: " + file)
      }
    }
  }
}
