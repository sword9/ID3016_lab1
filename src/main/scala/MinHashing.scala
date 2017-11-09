import scala.collection.SortedSet

class MinHashing (n:Int){

  private val rnd = new scala.util.Random
  private val hashers: Vector[Int => Int] = buildHashers()
  //c is a prime number
  private val c =  39916801

  //Scope of the random linear equation
  private val MAX_A = 1024
  private val MAX_B = 1024

  private def getRandom(min:Int, max:Int): Int = {
    rnd.nextInt((max - min) + 1)
  }

  private def buildHashers(): Vector[Int => Int] ={
    var hashersTmp: Vector[Int => Int] = scala.collection.immutable.Vector.empty

    for(i <- 0 to n -1){

      val hasher: Int => Int = (x:Int) => {
        /*
        val a = getRandom(1, MAX_A)
        val b = getRandom(1, MAX_B)
        */
        val a = i + 1
        val b = i + 2

        (a*x+b)%c
      }

      hashersTmp = hashersTmp :+ hasher
    }

    hashersTmp
  }

  def buildMinHash(set:SortedSet[Int]): Vector[Int] = {

    //Apply all hashers
    hashers.map(hasher => {
      var min:Int = Int.MaxValue

      //Calculate the min
      set.foreach((v: Int) => {
        val hashValue:Int = hasher(v)
        if(hashValue < min){
          min = hashValue
        }
      })
      min
    })
  }
}
