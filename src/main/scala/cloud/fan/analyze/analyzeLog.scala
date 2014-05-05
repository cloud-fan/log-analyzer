package cloud.fan.analyze

import cloud.fan._
import java.nio.file.{Files, Paths}
import java.nio.charset.Charset

/**
 * Created by cloud on 3/24/14.
 */
object analyzeLog {

  def apply(nodeType: String, node: String, logDir: String, group: String) {
    group match {
      case analyzeIFStatLog.group => analyzeIFStatLog(nodeType, node, logDir)
      case analyzeIOStatLog.group => analyzeIOStatLog(nodeType, node, logDir)
      case analyzeMPStatLog.group => analyzeMPStatLog(nodeType, node, logDir)
      case analyzeVMStatLog.group => analyzeVMStatLog(nodeType, node, logDir)
      case analyzeTopLog.pattern(process) => analyzeTopLog(nodeType, node, logDir, process.intern())
      case analyzeGCLog.pattern(process) => analyzeGCLog(nodeType, node, logDir, process.intern())
      case analyzeFDFStatLog.pattern(process) => analyzeFDFStatLog(nodeType, node, logDir, process.intern())
    }
  }

  private def cleanRemoteProcesses(node: String, process: String) {
    ProcessFinder.getProcessIds(process, node).map(pid => ShUtil.generateCommand(Seq("kill", "-9") ++ pid, node).!)
  }

  def getLogContentIterator(command: Seq[String], node: String, logDir: String, logFileName: String = null) = {
    cleanRemoteProcesses(node, command.head)
    val path = Paths.get(logDir, s"${ChartSender.parseNodeName(node)}-${if (logFileName == null) command.head else logFileName}.log")
    val writer = Files.newBufferedWriter(path, Charset.forName("utf-8"))
    ShUtil.generateCommand(command, node).lines.iterator map { s =>
      writer.write(s)
      writer.newLine()
      writer.flush()
      s
    }
  }

  def initCharts(nodeType: String, node: String, group: String, charts: Array[Chart]) {
    ChartSender.sendCharts(nodeType, node, group, charts.map(_.name))
    charts foreach { chart =>
      ChartSender.sendChart(nodeType, node, group, chart.name, chart.series, chart.title, chart.yAxisTitle)
    }
  }
}

case class Chart(name: String, title: String, yAxisTitle: String) {
  private[fan] var series: Array[String] = _

  def this(name: String, title: String, yAxisTitle: String, series: Array[String]) {
    this(name, title, yAxisTitle)
    this.series = series
  }
}