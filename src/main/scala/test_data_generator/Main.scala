package test_data_generator

import java.time.LocalDateTime
import org.apache.spark.sql.SparkSession
import test_data_generator.ConfigParse.getConfig
import test_data_generator.SampleGenerator.{between, datetime, int_generator, random, random_bool}


object Main {
  def main(args: Array[String]): Unit = {
    val database = getConfig.getProperty("schema")
    val source = scala.io.Source.fromFile(getConfig.getProperty("settings.file.absolute_path"))
    val lines = try source.mkString finally source.close()
    val settings = ujson.read(lines)

    val spark = SparkSession.builder.master("local[*]")
      .appName("HiveTestDataGenerator")
      .enableHiveSupport()
      .getOrCreate()

    val rules = settings("rules")
    val map_row_count: Map[String, Int] = upickle.default.read[Map[String, Int]](rules("row_count"))
    val field_rules: Map[String, Map[String, String]] = upickle.default.read[Map[String, Map[String, String]]](rules("field_rules"))

    val table_settings = settings.toString().replaceAll(""",\s*"rules"\s*.*""", "}")
    val table_settings_map: Map[String, List[Map[String, String]]] = upickle.default.read[Map[String, List[Map[String, String]]]](table_settings)

    def mkHiveFields(x: Map[String, String]): String = {
      if (x.getOrElse("dim", "") != "") s"${x("name")} ${x("type")}(${x("dim")})"
      else s"${x("name")} ${x("type")}"
    }

    def drop_template(table: String): String = {
      s"DROP TABLE IF EXISTS $database.$table"
    }

    def create_template(table: String, fields: List[Map[String, String]]): String = {
      s"CREATE TABLE IF EXISTS $database.$table(${fields.map(mkHiveFields).mkString(", ")}) STORED AS PARQUET"
    }

    def create_ext_template(table: String, fields: List[Map[String, String]]): String = {
      s"CREATE EXTERNAL TABLE IF EXISTS $database.$table(${fields.map(mkHiveFields).mkString(", ")}) STORED AS PARQUET LOCATION '/user/data/core/test'"
    }

    def insert_template(table: String, values: List[Any]): String = {
      s"INSERT INTO TABLE $database.$table VALUES (${values.mkString(",")})"
    }

    spark.sql(s"CREATE DATABASE IF NOT EXISTS $database")

    for(table <- table_settings_map.keys) {
      spark.sql(drop_template(table))
      spark.sql(create_template(table, table_settings_map(table)))

      val count = map_row_count.getOrElse(table, 0)
      var data = List.empty[List[Any]]

      for(field <- table_settings_map(table)) {
        val rule = field_rules.getOrElse(field("type"), Map())
        if (field("type") == "int") {
          if (rule.nonEmpty) {
            data = data :+ int_generator(
              count,
              rule.getOrElse("lower_threshold", 0).toString.toInt,
              rule.getOrElse("upper_threshold", 2147483647).toString.toInt,
              rule.getOrElse("initial_value", 0).toString.toInt,
              rule.getOrElse("repeat_number", 1).toString.toInt,
              rule.getOrElse("step", 1).toString.toInt,
            )
          }
          else data = data :+ List.fill(count)(between(0, 2147483647))
        }
        else if (field("type") == "string") {
          if (rule.nonEmpty) data = data :+ random(count, rule("acceptable_values").split(',').toList).map(x => s"'${x}'")
          else data = data :+ random(count).map(x => s"'${x}'")
        }
        else if (field("type") == "varchar") {
          if (rule.nonEmpty) data = data :+ random(count, rule("acceptable_values").split(',').toList).map(x => s"'${x}'")
          else data = data :+ random(count, field("dim").toInt).map(x => s"'${x}'")
        }
        else if (field("type") == "boolean") data = data :+ random_bool(count)
        else if (field("type") == "timestamp") {
          if (rule.nonEmpty) {
            data = data :+ datetime(LocalDateTime.parse(rule("start")), rule("step_by_minutes").toInt, count)
          }
          else data = data :+ List.fill(count)(between(LocalDateTime.now().minusYears(1), LocalDateTime.now()))
        }
        else println(s"Unsupported data type = `${field("type")}`")
      }
      data.transpose.map(x => insert_template(table, x)).map(x => spark.sql(x))

      spark.sql(s"SELECT * FROM $database.$table ORDER BY field_ts").show()
    }

    spark.stop()
  }

}
