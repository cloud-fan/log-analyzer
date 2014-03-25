package cloud.fan

/**
 * Created by cloud on 3/24/14.
 */
trait LogAnalyzer {

  val group: String
  val charts: Array[Chart]
  val command: String

  def apply(nodeType: String, node: String, logDir: String): Unit

  val percentage = "percentage (%)"
  val throughput = "throughput (kb/s)"
}
