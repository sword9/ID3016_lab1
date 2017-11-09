class Document (key:String, shingling:Shingling) extends Serializable {

  private val compareSets:CompareSets = new CompareSets()
  private var minHash:Vector[Int] = null

  def compareJaccardSimilarity(other:Document): Float = {
    compareSets.compareJaccardSimilarity(shingling.toSet(), other.getShingling().toSet())
  }

  def getKey(): String = {
    key
  }

  def getShingling(): Shingling ={
    shingling
  }

  def setMinHash(h:Vector[Int]): Unit = {
    minHash = h
  }

  def getMinHash(): Vector[Int] = {
    minHash
  }
}
