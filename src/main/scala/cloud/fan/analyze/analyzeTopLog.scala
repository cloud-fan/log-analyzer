package cloud.fan.analyze

import cloud.fan.{ProcessFinder, ChartSender}

/**
 * Created by cloud on 3/26/14.
 */
object analyzeTopLog extends LogAnalyzer {

  val group: String = "top"
  val command = Seq("top", "-b", "-d", "10", "-p")

  def apply(nodeType: String, node: String, logDir: String, process: String) {
    ProcessFinder.getUniqueProcessId(process, node) map { pid =>
      charts += new Chart("cpu", s"CPU utilization of $process", percentage, Array("cpu"))
      charts += new Chart("memory", s"memory utilization of $process", percentage, Array("memory"))
      analyzeLog.initCharts(nodeType, node, group, charts.toArray)
      val logIterator = analyzeLog.getLogContentIterator(command :+ pid, node, logDir)
      logIterator.map(_.trim).filter(_.matches("\\d+.+")) foreach { line =>
        val data = line.split("\\s+")
        ChartSender.sendData(nodeType, node, group, charts.head.name, Array(data(8)))
        ChartSender.sendData(nodeType, node, group, charts.last.name, Array(data(9)))
      }
    }
  }

  val pattern = s"$group:([a-zA-Z]+)".r
}
