package cloud.fan

import scalaj.http.{HttpOptions, Http}

/**
 * Created by cloud on 3/21/14.
 */
object ChartSender {

  val baseURL = "http://localhost:9000"

  def sendNodes(nodeType: String, nodes: Array[String]) {
    Http(s"$baseURL/$nodeType/init").param("names", nodes.map(parseNodeName).mkString(",")).option(HttpOptions.readTimeout(10000)).asString
  }

  def sendGroups(nodeType: String, node: String, groups: Array[String]) {
    Http(s"$baseURL/$nodeType/${parseNodeName(node)}/init").param("groups", groups.mkString(",")).asString
  }

  def sendCharts(nodeType: String, node: String, group: String, charts: Array[String]) {
    Http(s"$baseURL/$nodeType/${parseNodeName(node)}/$group/init").param("charts", charts.mkString(",")).asString
  }

  def sendChart(nodeType: String, node: String, group: String, chart: String, series: Array[String], title: String, yAxisTitle: String) {
    Http(s"$baseURL/$nodeType/${parseNodeName(node)}/$group/$chart/init")
      .params("series" -> series.mkString(","), "title" -> title, "yAxisTitle" -> yAxisTitle).asString
  }

  def sendData(nodeType: String, node: String, group: String, chart: String, data: Array[String]) {
    Http(s"$baseURL/$nodeType/${parseNodeName(node)}/$group/$chart/ingest").param("data", data.mkString(",")).asString
  }

  def finish(resultPath: String) {
    Http(s"$baseURL/finish").param("path", resultPath).option(HttpOptions.readTimeout(100000)).asString
  }

  def parseNodeName(node: String) = node.split("@").last
}
