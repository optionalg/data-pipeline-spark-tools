package org.alghimo.sparkPipelines.dataManager

import org.apache.spark.sql.DataFrame

/**
  * Created by D-KR99TU on 21/02/2017.
  */
class CompositeDataManager(dataManagers: Seq[DataManager], override val options: Map[String, String] = Map(), dmStrategy: (Seq[DataManager], String) => DataManager = CompositeDataManager.headDataManagerStrategy) extends DataManager {

  def hasResource(key: String): Boolean = {
    dataManagers.map(_.hasResource(key)).max
  }

  def resourceName(key: String): String = {
    val dmsWithResource = dmForKey(key)

    dmsWithResource.resourceName(key)
  }

  /**
    * Returns a DataFrame for the provided key.
    * @param key
    * @return DataFrame
    */
  def get(key: String): DataFrame = {
    val dm = dmForKey(key)

    dm.get(key)
  }

  /**
    * Writes the DataFrame to the provided key. Write mode can be one of the standard spark write modes.
    * @param key
    * @param df
    */
  def save(key: String, df: DataFrame, mode: String): Unit = {
    val dm = dmForKey(key)

    dm.save(key, df, mode)
  }

  private def dmForKey(key: String) = {
    val dms = dataManagers.filter(_.hasResource(key))
    dmStrategy(dms, key)
  }
}

object CompositeDataManager {
  def hiveAndCsvDataManager(@transient spark: org.apache.spark.sql.SparkSession, options: Map[String, String]): CompositeDataManager = {
    val csvDataManager = CsvFileDataManager(spark, options)
    val hiveDataManager = HiveDataManager(spark, options)
    new CompositeDataManager(Seq(hiveDataManager, csvDataManager), options, hiveDataManagerStrategy)
  }

  def headDataManagerStrategy(dms: Seq[DataManager], key: String): DataManager = {
    if (dms.isEmpty) {
      throw new RuntimeException("No data manager has resource " + key)
    }
    dms.head
  }

  def hiveDataManagerStrategy(dms: Seq[DataManager], key: String): DataManager = {
    val hiveDms = dms.filter(_ match {
      case x: HiveDataManager => true
      case _ => false
    })
    if (hiveDms.isEmpty) {
      dms.head
    } else {
      hiveDms.head
    }
  }
}
