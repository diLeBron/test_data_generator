package test_data_generator

import java.io._
import java.util._

object ConfigParse {
  def getConfig: Properties = {
    val f = new File("application.properties")
    val props = new Properties
    val fis = new InputStreamReader(new FileInputStream(f), "UTF-8")
    props.load(fis)
    fis.close()
    props
  }
}
