package edu.knowitall.repr.document

class Document(val text: String) {
  override def toString = {
    if (text.length > 80)
      s"Document(${text.take(80) + "..."})"
    else
      s"Document($text)"
  }

  def canEqual(that: Document) = that.isInstanceOf[Document]
  override def equals(that: Any) = that match {
    case that: Document => (that canEqual this) && this.text == that.text
  }
  override def hashCode = text.hashCode
}
