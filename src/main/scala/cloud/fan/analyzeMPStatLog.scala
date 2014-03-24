package cloud.fan

import scala.collection.mutable.ArrayBuffer

/**
 * Created by cloud on 3/24/14.
 */
object analyzeMPStatLog extends LogAnalyzer {

  val group: String = "CPU"
  val charts: Array[Chart] = Array(Chart("allCPU", "CPU usage across cores", percentage))

  def apply(nodeType: String, node: String, logDir: String) {
    val logIterator = analyzeLog.getLogContentIterator("mpstat -P ALL 10", node, logDir)
    val pattern = """(\d+)\sCPU""".r
    val cpuCount = pattern.findFirstMatchIn(logIterator.next()).get.group(1).toInt
    charts.head.series = (0 until cpuCount).map("cpu" + _).toArray
    analyzeLog.initCharts(nodeType, node, group, charts)
    val block = ArrayBuffer.empty[String]
    getBlock(logIterator, block, cpuCount)
    while (true) {
      ChartSender.sendData(nodeType, node, group, charts.head.name, block.toArray)
      getBlock(logIterator, block, cpuCount)
    }
  }

  def getBlock(i: Iterator[String], block: ArrayBuffer[String], cpuCount: Int) {
    block.clear()
    i.next()
    i.next()
    i.next()
    (0 until cpuCount).foreach{_ =>
      val line = i.next().trim.split("\\s+")
      block += "%.2f".format(line(3).toDouble+line(5).toDouble)
    }
  }
}
