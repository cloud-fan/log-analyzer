package cloud.fan.analyze

import cloud.fan.{ShUtil, ProcessFinder}
import scala.sys.process._

/**
 * Created by cloud on 5/4/14.
 */
object analyzeFDFStatLog extends LogAnalyzer {

  val group: String = "FDF"
  val command: Seq[String] = Seq("tail", "-n", "+0", "-f")

  def apply(nodeType: String, node: String, logDir: String, process: String) {
    ProcessFinder.getUniqueProcessId(process, node) map { pid =>
      val fileName = s"/tmp/fdfstats.log-$pid"
      ensureFileExist(fileName, node)
      val logIterator = analyzeLog.getLogContentIterator(command :+ fileName, node, logDir, "fdfstats")
      while(true)
        logIterator.next()
    }
  }

  private def ensureFileExist(fileName: String, node: String, reTryCount: Int = 50) {
    val result = ShUtil.generateCommand(Seq("ls", fileName), node) ! ProcessLogger(a => (), a => ())
    if (result == 2) {
      Thread.sleep(2000)
      ensureFileExist(fileName, node, reTryCount - 1)
    } else if (result != 0) {
      System.err.println(s"can not find file $fileName at $node!")
    }
  }

  val pattern = s"$group:([a-zA-Z]+)".r
}
