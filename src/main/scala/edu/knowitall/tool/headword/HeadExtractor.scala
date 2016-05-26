package edu.knowitall.tool.headword

import edu.knowitall.tool.postag.PostaggedToken

trait HeadExtractor {

  /** Given a Seq[PostaggedToken] representing a relation, will return those
    * tokens comprising the headword(s) of the relation, possibly empty if
    * headword(s) couldn't be determined.
    */
  def relationHead(tokens: Seq[PostaggedToken]): Seq[PostaggedToken]

  /** Given a Seq[PostaggedToken] representing a argument, will return those
    * tokens comprising the headword(s) of the argument, possibly empty if
    * headword(s) couldn't be determined.
    */
  def argumentHead(tokens: Seq[PostaggedToken]): Seq[PostaggedToken]
}
