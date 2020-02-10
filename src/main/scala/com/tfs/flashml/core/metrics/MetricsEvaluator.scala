package com.tfs.flashml.core.metrics

import com.tfs.flashml.util.conf.FlashMLConstants
import com.tfs.flashml.util.{ConfigUtils, FlashMLConfig}
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.col
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Parent class for classes to compute standard metrics and custom metrics.
 */
object MetricsEvaluator
{
    private val log = LoggerFactory.getLogger(getClass)
    val metricsMap = mutable.Map[String, AnyRef]()
    val csvMetrics = new StringBuilder

  val columnsNames = (ConfigUtils.primaryKeyColumns ++ Array(ConfigUtils.topVariable, ConfigUtils.getIndexedResponseColumn, ConfigUtils.pageColumn, "probability"))
    .filter(_.nonEmpty)
    .distinct


  def evaluateStandardMetrics(predictionArray: Array[DataFrame]) :Unit = {
    StandardMetricsEvaluator.generateMetrics(predictionArray)
  }

  def evaluateCustomMetrics(predictionArray: Array[DataFrame], step: String):Unit={

    val dfArray =
      if (ConfigUtils.isPageLevelModel)
        pageLevelTransform(predictionArray)
      else predictionArray.map(_.select(columnsNames.map(col): _*))

    dfArray
      .tail
      .map(_.cache)

    val metricType = if (step.equals("metrics")) "Custom" else "Publish"

    log.info(s"\n$metricType Metrics")

    WebCustomMetricsEvaluator.customMetricsSimulation(dfArray,step)

    dfArray.tail.map(_.unpersist())
  }

  private def pageLevelTransform(inputArray: Array[DataFrame]): Array[DataFrame] = {
    var tempDf: DataFrame = null

    val outputArray: ArrayBuffer[DataFrame] = ArrayBuffer[DataFrame]()
    for (x <- inputArray.indices) {
      if (x % ConfigUtils.numPages == 0) {

        tempDf = inputArray(x)
          .select(columnsNames.map(col): _*)
      }
      else {
        tempDf = tempDf
          .union(inputArray(x)
            .select(columnsNames
              .map(col): _*))

        if ((x + 1) % ConfigUtils.numPages == 0) {
          outputArray.append(tempDf)
        }
      }
    }
    outputArray.toArray
  }

}
