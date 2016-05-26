package edu.knowitall.tool
package tokenize

class RemoteTokenizer(val urlString: String) extends Tokenizer with Remote {
  def tokenize(sentence: String) = {
    val response = post(sentence)
    Tokenizer.multilineStringFormat.read(response)
  }
}