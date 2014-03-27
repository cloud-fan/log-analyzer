package cloud.fan

import scala.collection.mutable.ArrayBuffer
import scala.sys.process._

/**
 * Created by cloud on 3/24/14.
 */
object analyzeMPStatLog extends LogAnalyzer {

  val group: String = "CPU"
  charts += Chart("allCPU", "CPU usage across cores", percentage)
  val command: String = "mpstat -P ALL 10"

  def apply(nodeType: String, node: String, logDir: String) {
    val cpuCount = Seq("ssh", node, "grep processor /proc/cpuinfo|wc -l").!!.trim.toInt
    val logIterator = analyzeLog.getLogContentIterator(command, node, logDir)
    charts.head.series = (0 until cpuCount).map("cpu" + _).toArray
    analyzeLog.initCharts(nodeType, node, group, charts.toArray)
    val block = ArrayBuffer.empty[String]
    logIterator.next()
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
