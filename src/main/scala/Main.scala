import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.storage.StorageLevel

object Main {
  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .appName("Spark SQL basic example")
      .config("spark.master", "local[*]")
      .getOrCreate()

    val sc = spark.sqlContext

    val filePath = "/home/victor/Desktop/ScalaProjects/DMLab1/descriptions.csv"
    //val filePath = "/home/victor/Desktop/data/descriptions.csv"

    val doc: DataFrame =  sc.read
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .option("inferSchema", "true")
      .option("parserLib", "univocity")
      .option("multiLine", "true")
      .load(filePath).cache()


    val rdd: RDD[Row] = doc.select("*").rdd
    val k = 9
    val n = 50
    val KEY_LENGTH = 40
    val MINHASH_THRESHOLD = 0.20
    val LSH_THREASHOLD = 0.10
    val JACCARD_THREASHOLD = 0.10
    val LSH_BANDS = 5

    val documentRdd = rdd.filter((e: Row) => {
      val key = e.get(1).asInstanceOf[String]

      key != null && key.length() == KEY_LENGTH
    }).map(e => {
      val key = e.get(1).asInstanceOf[String]
      val desc = e.get(0).asInstanceOf[String]

      val shingling:Shingling = new Shingling(k, (key, desc))
      shingling.grab()

      new Document(key, shingling)
    }).persist(StorageLevel.MEMORY_AND_DISK)



    /*
    val jaccardRating = documentRdd.cartesian(documentRdd).map((t:(Document, Document)) => {
      val similarity: Float = t._1.compareJaccardSimilarity(t._2)
      (similarity,  t._1.getKey(), t._2.getKey())
    }).filter((f:(Float, String, String)) => {
      //f._2 != f._3 && f._1 > JACCARD_THREASHOLD
      f._2 != f._3
    }).sortBy(_._1, false).top(10).foreach((f: (Float, String, String)) => {
      println("Jaccard => Rating: " + f._1 + " DocumentA:" + f._2 + " Document B:" + f._3)
    })
    */

    val minHashedRdd = documentRdd.map((f: Document) => {
      val minHashing:MinHashing = new MinHashing(n)
      f.setMinHash(minHashing.buildMinHash(f.getShingling().shingles))
      f
    }).persist(StorageLevel.MEMORY_AND_DISK)

    /*
    minHashedRdd.cartesian(minHashedRdd).map((f: (Document, Document)) =>  {
      val cmp:CompareSignatures = new CompareSignatures()
      val similarity: Float =  cmp.compareSimilarity(f._1.getMinHash(), f._2.getMinHash())
      (similarity, f._1.getKey(), f._2.getKey())
    }).filter((f:(Float, String, String)) => {
      f._2 != f._3 && f._1 > MINHASH_THRESHOLD
    }).sortBy(_._1, false).top(20).foreach((f: (Float, String, String)) => {
      println("MinHASH => Rating: " + f._1 + " DocumentA:" + f._2 + " Document B:" + f._3)
    })
    */

    val bandsRdd = minHashedRdd.map((f: Document) => {
      val lsh:LSH = new LSH(f.getMinHash(), LSH_BANDS)
      (f.getKey(), lsh.getBands())
    }).persist(StorageLevel.MEMORY_AND_DISK)

    bandsRdd.cartesian(bandsRdd).filter((f: ((String, Vector[Int]), (String, Vector[Int]))) => {
      f._1._1.hashCode < f._2._1.hashCode
    }).map((f: ((String, Vector[Int]), (String, Vector[Int]))) =>{
      val cmp:CompareSignatures = new CompareSignatures()

      val similarity: Float =  cmp.compareSimilarity(f._1._2, f._2._2)
      (similarity, f._1._1, f._2._1)
    }).filter((f:(Float, String, String)) => {
      f._2 != f._3 && f._1 > LSH_THREASHOLD
    }).sortBy(_._1, false).top(20).foreach((f: (Float, String, String)) => {
      println("LSH => Rating: " + f._1 + " DocumentA:" + f._2 + " Document B:" + f._3)
    })
  }
}
