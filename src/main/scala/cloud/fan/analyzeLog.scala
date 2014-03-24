package cloud.fan

import java.nio.file.{Paths, Files}
import java.nio.charset.Charset

/**
 * Created by cloud on 3/24/14.
 */
object analyzeLog {

  def apply(nodeType: String, node: String, baseLogDir: String, group: String) {
    group match {
      case analyzeIFStatLog.group => analyzeIFStatLog(nodeType, node, baseLogDir)
      case analyzeIOStatLog.group => analyzeIOStatLog(nodeType, node, baseLogDir)
      case analyzeMPStatLog.group => analyzeMPStatLog(nodeType, node, baseLogDir)
      case analyzeVMStatLog.group => analyzeVMStatLog(nodeType, node, baseLogDir)
    }
  }

  def getLogContentIterator(command: String, node: String, logDir: String) = {
    val path = Paths.get(logDir, s"${ChartSender.parseNodeName(node)}-${command.split("\\s+").head}.log")
    import scala.sys.process._
    val writer = Files.newBufferedWriter(path, Charset.forName("utf-8"))
    Seq("ssh", node, command).lines.iterator.map {s =>
      writer.write(s)
      writer.newLine()
      writer.flush()
      s
    }
  }

  def initCharts(nodeType: String, node: String, group: String, charts: Array[Chart]) {
    ChartSender.sendCharts(nodeType, node, group, charts.map(_.name))
    charts.foreach{chart =>
      ChartSender.sendChart(nodeType, node, group, chart.name, chart.series, chart.title, chart.yAxisTitle)
    }
  }
}

case class Chart(name: String, title: String, yAxisTitle: String) {
  private[fan] var series: Array[String] = _
}
