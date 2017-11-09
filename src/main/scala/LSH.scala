import scala.collection.mutable.ArrayBuffer

class LSH (minHash: Vector[Int], rowsInBand:Int) {

  if(minHash.size % rowsInBand != 0){

      throw new IllegalArgumentException("The minhash size mustbe a multiple of the rows in band")
  }

  def getBands(): Vector[Int] = {

    val bands = new ArrayBuffer[Vector[Int]]()


    for{
      i <- 0 to minHash.size
      if i % rowsInBand == 0
    } {

      val newItems: Vector[Int] = minHash.slice(i, i + rowsInBand)

      bands.append(newItems)
    }

    Vector[Int]() ++ bands.map(band => band.hashCode()).toList
  }
}
