package sparkapps

import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object MoviesRecommenderSystemALS {

  def sparkJob() = {

    val conf = new SparkConf()
      .setAppName("Spark movies recommender system with ALS")
      .setMaster("local")

    val sc = new SparkContext(conf)

    // Load and parse the data
    val data = sc.textFile("movies.csv")

    // Extract features as Rating
    val ratings = data.map(_.split("\\s+") match {
      case Array(user, item, rate) => Rating(user.toInt, item.toInt, rate.toDouble)
    })

    val splits = ratings.randomSplit(Array(0.8, 0.2))
    val training = splits(0).cache()
    val test = splits(1)

    // Build the recommendation model using ALS from training data
    val model = ALS.train(training, rank = 10, iterations = 20, 0.01)

    // Evaluate the model
    val userMovies = test.map {
      case Rating(user, movie, rate) => (user, movie)
    }
    val predictions = model.predict(userMovies).map {
      case Rating(user, movie, rate) => ((user, movie), rate)
    }

    val ratesAndPreds = test.map {
      case Rating(user, movie, rate) => ((user, movie), rate)
    }.join(predictions)

    val MSE = ratesAndPreds.map { case ((user, movie), (r1, r2)) =>
      val err = (r1 - r2)
      err * err
    }.mean()

    println("Mean Squared Error = " + MSE)

    sc.stop()

  }

  def main(args: Array[String])= sparkJob()

}
