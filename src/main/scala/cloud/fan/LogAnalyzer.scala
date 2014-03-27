package cloud.fan

import scala.collection.mutable.ArrayBuffer

/**
 * Created by cloud on 3/24/14.
 */
trait LogAnalyzer {

  val group: String
  val charts = ArrayBuffer.empty[Chart]
  val command: String

  val percentage = "percentage (%)"
  val throughput = "throughput (kb/s)"
}
