package cloud.fan.analyze

import cloud.fan.{Main, ChartSender, ProcessFinder}


/**
 * Created by cloud on 4/29/14.
 */
object analyzeGCLog extends LogAnalyzer {

  val group: String = "GC"
  charts += new Chart("totalGCTime", "Total GC Time", "time (s)", Array("GC"))
  val command: Seq[String] = Seq("jstat", "-gcutil")

  def apply(nodeType: String, node: String, logDir: String, process: String) {
    ProcessFinder.getUniqueProcessId(process, node) map { pid =>
      analyzeLog.initCharts(nodeType, node, group, charts.toArray)
      val logIterator = analyzeLog.getLogContentIterator(command :+ pid :+ (Main.interval * 1000).toString, node, logDir, "gc")
      logIterator.next()
      logIterator foreach { line =>
        val data = line.trim().split("\\s+").last
        ChartSender.sendData(nodeType, node, group, charts.head.name, Array(data))
      }
    }
  }

  val pattern = s"$group:([a-zA-Z]+)".r
}
