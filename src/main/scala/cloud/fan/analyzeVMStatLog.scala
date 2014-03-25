package cloud.fan

/**
 * Created by cloud on 3/24/14.
 */
object analyzeVMStatLog extends LogAnalyzer {

  val group: String = "memory"
  val charts: Array[Chart] = Array(Chart("DRAMallocation", "system DRAM allocation", "size (mb)"))
  val command: String = "vmstat -n 10 -S M"

  def apply(nodeType: String, node: String, logDir: String) {
    val logIterator = analyzeLog.getLogContentIterator(command, node, logDir)
    charts.head.series = Array("swaped", "free", "buff", "cache")
    analyzeLog.initCharts(nodeType, node, group, charts)
    logIterator.next()
    logIterator.next()
    logIterator.foreach{line =>
      val data = line.trim().split("\\s+")
      ChartSender.sendData(nodeType, node, group, charts.head.name, Array(data(2), data(3), data(4), data(5)))
    }
  }
}
