package cloud.fan

/**
 * Created by cloud on 3/24/14.
 */
object analyzeIFStatLog extends LogAnalyzer {

  val group = "network"
  val charts = Array(Chart("network", "Network Bandwidth", throughput))

  def apply(nodeType: String, node: String, logDir: String) {
    val logIterator = analyzeLog.getLogContentIterator("ifstat -n -T 10", node, logDir)
    val series = logIterator.next().trim.split("\\s+").flatMap(s => Array(s + "_in", s + "_out"))
    charts.head.series = series
    analyzeLog.initCharts(nodeType, node, group, charts)
    logIterator.next()
    logIterator.foreach{line =>
      ChartSender.sendData(nodeType, node, group, charts.head.name, line.trim.split("\\s+"))
    }
  }
}
