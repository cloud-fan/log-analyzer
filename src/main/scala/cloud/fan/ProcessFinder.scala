package cloud.fan

import scala.collection.mutable

/**
 * Created by cloud on 4/29/14.
 */
object ProcessFinder {

  val cachedProcessId = mutable.HashMap.empty[String, Option[Array[String]]]

  def getProcessIds(processName: String, node: String) = {
    if (cachedProcessId.contains(processName)) {
      cachedProcessId(processName)
    } else {
      cachedProcessId.synchronized {
        if (cachedProcessId.contains(processName)) {
          cachedProcessId(processName)
        } else {
          val result = doWork(processName, node)
          cachedProcessId += processName -> result
          result
        }
      }
    }
  }

  private def doWork(processName: String, node: String) = {
    val command = Seq(Seq("ps", "aux"), Seq("grep", processName), Seq("grep", "-v", "grep"), Seq("grep", "-v", "log-analyzer"), Seq("awk", "{print $2}"))
    val output = ShUtil.generatePipedCommand(command, node).!!
    if (output.length > 0) {
      Some(output.split('\n'))
    } else {
      None
    }
  }

  def getUniqueProcessId(processName: String, node: String, reTryCount: Int = 40): Option[String] = {
    getProcessIds(processName, node) match {
      case Some(pid) =>
        if (pid.size == 1) {
          Some(pid.head)
        } else {
          System.err.println("non unique process name! pid are: " + pid.mkString(","))
          None
        }
      case None =>
        if (reTryCount > 0) {
          Thread.sleep(2000)
          getUniqueProcessId(processName, node, reTryCount - 1)
        } else {
          System.err.println(s"process $processName can not found at remote server $node!")
          None
        }
    }
  }
}
