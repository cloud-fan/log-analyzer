package cloud.fan

import scala.sys.process._

/**
 * Created by cloud on 3/26/14.
 */
object analyzeTopLog extends LogAnalyzer {

  val group: String = "top"
  val charts: Array[Chart] = Array(null)
  val command: String = "top -b -d 10 -p"

  def apply(nodeType: String, node: String, logDir: String, process: String) {
    val pid = Seq("ssh", node, "ps aux|grep "+process+"|grep -v grep|tail -n 1|awk '{print $2}'").!!.trim
    if (pid.matches("\\d+")) {
      charts(0) = new Chart("top", s"CPU and Memory utilization of $process", percentage, Array("cpu", "memory"))
      analyzeLog.initCharts(nodeType, node, group, charts)
      val logIterator = analyzeLog.getLogContentIterator(command + pid, node, logDir)
      logIterator.filter(_.matches("\\d+.+")).foreach {line =>
        val data = line.trim.split("\\s+")
        ChartSender.sendData(nodeType, node, group, charts.head.name, Array(data(8), data(9)))
      }
    } else {
      System.err.println(s"process $process can not found at remote server $node!")
    }
  }

  val pattern = s"$group:([a-zA-Z]+)".r
}
