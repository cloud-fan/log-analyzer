package cloud.fan

/**
 * Created by cloud on 3/24/14.
 */
object analyzeIFStatLog extends LogAnalyzer {

  val group = "network"
  val charts = Array(Chart("network", "Network Bandwidth", throughput))
  val command: String = "ifstat -n -T 10"

  def apply(nodeType: String, node: String, logDir: String) {
    val logIterator = analyzeLog.getLogContentIterator(command, node, logDir)
    val series = logIterator.next().trim.split("\\s+").zipWithIndex.
      filter(s => s._1.startsWith("eth") || s._1 == "Total").
      flatMap(s => Array((s._1 + "_in", s._2 * 2), (s._1 + "_out", s._2 * 2 + 1)))
    charts.head.series = series.map(_._1)
    val validDataIndex = series.map(_._2)
    analyzeLog.initCharts(nodeType, node, group, charts)
    logIterator.next()
    logIterator.foreach{line =>
      val allData = line.trim.split("\\s+")
      val validData = validDataIndex.map(allData(_))
      ChartSender.sendData(nodeType, node, group, charts.head.name, validData)
    }
  }
}
