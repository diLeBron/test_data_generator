name := "test_data_generator"

version := "0.1"

scalaVersion := "2.12.12"

lazy val sparkVersion = "2.4.0"
lazy val uVersion = "0.6.6"
lazy val osVersion = "0.7.6"

val main_class: String = "test_data_generator.Main"

javacOptions ++= Seq("-source", "1.8")
compileOrder := CompileOrder.JavaThenScala

//assemblyOption in assembly := (assemblyOption in assembly).value.withIncludeScala(true).withIncludedDepedency(true)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-hive" % sparkVersion,
  "com.lihaoyi" %% "ujson" % uVersion,
  "com.lihaoyi" %% "upickle" % uVersion,
  "com.lihaoyi" %% "os-lib" % osVersion
)


