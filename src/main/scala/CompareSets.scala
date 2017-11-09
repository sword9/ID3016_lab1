class CompareSets extends Serializable {

  def compareJaccardSimilarity(setA:Set[Int], setB:Set[Int]): Float = {
    /*
      Scenario a: 1 in both columns
      Scenario b: only setA contains
      Scenario c: only setB contains
      Scenario d: neither one contains (ignored here)
    */
    var a = 0
    var b = 0
    var c = 0

    setA.foreach((f: Int) => {
      if(setB.contains(f)){
        a += 1
      }else{
        b += 1
      }
    })

    setB.foreach((f:Int) => {
      if(!setA.contains(f)){
        c += 1
      }
    })

    //Return Jaccard similarity
    a.toFloat / (a + b + c).toFloat
  }
}
