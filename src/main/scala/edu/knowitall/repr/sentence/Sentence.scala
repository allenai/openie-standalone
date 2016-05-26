package edu.knowitall.repr.sentence

class Sentence(val text: String) {
  override def toString = s"Sentence($text)"

  def canEqual(that: Sentence) = that.isInstanceOf[Sentence]
  override def equals(that: Any) = that match {
    case that: Sentence => (that canEqual this) && this.text == that.text
  }
  override def hashCode = text.hashCode
}
