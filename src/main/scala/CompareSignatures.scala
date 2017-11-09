class CompareSignatures {

  def compareSimilarity(a:Vector[Int], b:Vector[Int]): Float ={

    //They need to be of same length
    if(a.size != b.size){
      throw new Exception()
    }

    var similarities = 0
    for(i <- 0 to a.size -1){

      if(a(i) == b(i)){
        similarities += 1
      }
    }

    similarities.toFloat / a.size.toFloat
  }
}
