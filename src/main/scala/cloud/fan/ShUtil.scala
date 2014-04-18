package cloud.fan

import java.net.InetAddress
import scala.sys.process._

/**
 * Created by cloud on 4/18/14.
 */
object ShUtil {
  def generateCommand(command: Seq[String], node: String): ProcessBuilder = {
    if (node == "localhost" || node == InetAddress.getLocalHost().getHostName()) {
      command
    } else {
      Seq("ssh", node, command.map(handleSpecialParameter).mkString(" "))
    }
  }

  def generatePipedCommand(command: Seq[Seq[String]], node: String): ProcessBuilder = {
    if (node == "localhost" || node == InetAddress.getLocalHost().getHostName()) {
      command.map(stringSeqToProcess).reduceLeft(_ #| _)
    } else {
      Seq("ssh", node, command.map(_.map(handleSpecialParameter).mkString(" ")).mkString("|"))
    }
  }

  private def handleSpecialParameter(s: String) = {
    if (s.contains(" ")) {
      s"'$s'"
    } else {
      s
    }
  }
}
