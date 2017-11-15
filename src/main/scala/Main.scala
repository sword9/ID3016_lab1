import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.storage.StorageLevel

import scala.io.Source

object Main {
  def main(args: Array[String]): Unit = {

    //Create the spark session
    val spark = SparkSession
      .builder()
      .appName("Spark SQL basic example")
      .config("spark.master", "local[*]")
      .getOrCreate()

    val sc = spark.sqlContext

    //I use this other hardcoded path for a bigger data dump, you can ignore this.
    //val filePath = "/home/victor/Desktop/data/descriptions.csv"
    val filePath = getClass.getResource("descriptions.csv").getPath

    //Load the csv data with mulitline and univocity parser
    val doc: DataFrame =  sc.read
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .option("inferSchema", "true")
      .option("parserLib", "univocity")
      .option("multiLine", "true")
      .load(filePath).cache()

    val rdd: RDD[Row] = doc.select("*").rdd

    //Tunable parameters for the similarity functions
    val k = 4
    val n = 50
    val KEY_LENGTH = 40
    val MINHASH_THRESHOLD = 0.20
    val LSH_THREASHOLD = 0.30
    val JACCARD_THREASHOLD = 0.10
    val LSH_BANDS = 5

    //Parse the data to RDDs
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


    //Calculate the Jaccard rating
    val jaccardRating = documentRdd.cartesian(documentRdd).filter((f: (Document, Document)) => {
      f._1.getKey().hashCode < f._2.getKey().hashCode
    }).map((t:(Document, Document)) => {
      val similarity: Float = t._1.compareJaccardSimilarity(t._2)
      (similarity,  t._1.getKey(), t._2.getKey())
    }).filter((f:(Float, String, String)) => {
      //f._2 != f._3 && f._1 > JACCARD_THREASHOLD
      f._2 != f._3
    }).sortBy(_._1, false).top(10).foreach((f: (Float, String, String)) => {
      println("Jaccard => Rating: " + f._1 + " DocumentA:" + f._2 + " Document B:" + f._3)
    })


    //Do the min hashing and cache
    val minHashedRdd = documentRdd.map((f: Document) => {
      val minHashing:MinHashing = new MinHashing(n)
      f.setMinHash(minHashing.buildMinHash(f.getShingling().shingles))
      f
    }).persist(StorageLevel.MEMORY_AND_DISK)


    //Do comaprisons with the min hash
    minHashedRdd.cartesian(minHashedRdd).filter((f: (Document, Document)) =>{
      f._1.getKey().hashCode < f._2.getKey().hashCode
    }).map((f: (Document, Document)) =>  {
      val cmp:CompareSignatures = new CompareSignatures()
      val similarity: Float =  cmp.compareSimilarity(f._1.getMinHash(), f._2.getMinHash())
      (similarity, f._1.getKey(), f._2.getKey())
    }).filter((f:(Float, String, String)) => {
      f._2 != f._3 && f._1 > MINHASH_THRESHOLD
    }).sortBy(_._1, false).top(20).foreach((f: (Float, String, String)) => {
      println("MinHASH => Rating: " + f._1 + " DocumentA:" + f._2 + " Document B:" + f._3)
    })


    //Perform the LSH
    val bandsRdd = minHashedRdd.map((f: Document) => {
      val lsh:LSH = new LSH(f.getMinHash(), LSH_BANDS)
      (f.getKey(), lsh.getBands())
    }).persist(StorageLevel.MEMORY_AND_DISK)

    //Compare the documents with LSH
    bandsRdd.cartesian(bandsRdd).filter((f: ((String, Vector[Int]), (String, Vector[Int]))) => {
      f._1._1.hashCode < f._2._1.hashCode
    }).map((f: ((String, Vector[Int]), (String, Vector[Int]))) =>{
      val cmp:CompareSignatures = new CompareSignatures()
      val similarity: Float = cmp.compareSimilarity(f._1._2, f._2._2)
      (similarity, f._1._1, f._2._1)
    }).filter((f:(Float, String, String)) => {
      f._2 != f._3 && f._1 > LSH_THREASHOLD
    }).sortBy(_._1, false).top(20).foreach((f: (Float, String, String)) => {
      println("LSH => Rating: " + f._1 + " DocumentA:" + f._2 + " Document B:" + f._3)
    })
  }
}

