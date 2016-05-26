package edu.knowitall.tool
package stem

class RemoteStemmer(val urlString: String) extends Stemmer with Remote {
  override def stem(word: String) = {
    post(word)
  }
}