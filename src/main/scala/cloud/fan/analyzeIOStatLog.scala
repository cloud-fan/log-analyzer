package cloud.fan

import scala.collection.mutable.ArrayBuffer

/**
 * Created by cloud on 3/24/14.
 */
object analyzeIOStatLog extends LogAnalyzer {

  val group = "disk"
  val charts = Array(
    Chart("flashUtilization", "Flash Utilization", percentage),
    Chart("writeBandwidth", "Flash Write Bandwidth", throughput),
    Chart("readBandwidth", "Flash Read Bandwidth", throughput)
  )

  def apply(nodeType: String, node: String, logDir: String) {
    val logIterator = analyzeLog.getLogContentIterator("iostat -xk 10", node, logDir)
    val block = ArrayBuffer.empty[Array[String]]
    getBlock(logIterator, block)
    charts.foreach(_.series = block.map(_.head).toArray)
    analyzeLog.initCharts(nodeType, node, group, charts)
    while (true) {
      ChartSender.sendData(nodeType, node, group, charts(0).name, block.map(_.last).toArray)
      ChartSender.sendData(nodeType, node, group, charts(1).name, block.map(_(6)).toArray)
      ChartSender.sendData(nodeType, node, group, charts(2).name, block.map(_(5)).toArray)
      getBlock(logIterator, block)
    }
  }

  def getBlock(i: Iterator[String], block: ArrayBuffer[Array[String]]) {
    block.clear()
    i.find(_.startsWith("Device:"))
    var line = i.next()
    while (line != "") {
      if (!line.trim.startsWith("dm-")) {
        block += line.trim.split("\\s+")
      }
      line = i.next()
    }
  }
}
