import org.apache.spark.sql.Row

import scala.collection.SortedSet


class Shingling (k:Int, doc:(String, String))  extends Serializable {

  var shingles = SortedSet[Int]()
  val key = doc._1

  def grab(): Unit ={
    val desc = doc._1

    desc.sliding(k, 1).foreach(pt => {
      shingles += pt.hashCode
    })

    shingles
  }

  def toSet() : Set[Int] = {
    shingles.asInstanceOf[Set[Int]]
  }
}
