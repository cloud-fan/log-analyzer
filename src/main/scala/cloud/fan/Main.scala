package cloud.fan

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import java.util.concurrent.Executors
import java.nio.file._
import java.text.SimpleDateFormat
import java.util.Date
import java.nio.file.attribute.BasicFileAttributes
import java.io.IOException

/**
 * Created by cloud on 3/21/14.
 */
object Main {

  val futures = ListBuffer.empty[Future[Unit]]
  implicit val threadPool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(64))

  def doWork(nodes: Array[String], groups: Array[String], nodeType: String, logDir: String) {
    ChartSender.sendNodes(nodeType, nodes)
    for(node <- nodes) {
      ChartSender.sendGroups(nodeType, node, groups.map("(^[a-zA-Z]+).*".r.findFirstMatchIn(_).get.group(1)))
      for (group <- groups) {
        futures += future {
          analyzeLog(nodeType, node, logDir, group)
        }
      }
    }
  }

  def cleanRemoteProcesses(nodes: Array[String], groups: Array[String]) {
    def kill(node: String, process: String) {
      import scala.sys.process._
      Seq("ssh", node, "ps aux|grep "+process+"|grep -v grep|awk '{print $2}'|xargs kill -9 > /dev/null 2>&1").!
    }
    groups.foreach {
      case analyzeIFStatLog.group => nodes.foreach(kill(_, "ifstat"))
      case analyzeIOStatLog.group => nodes.foreach(kill(_, "iostat"))
      case analyzeMPStatLog.group => nodes.foreach(kill(_, "mpstat"))
      case analyzeVMStatLog.group => nodes.foreach(kill(_, "vmstat"))
      case analyzeTopLog.pattern(_) => nodes.foreach(kill(_, "top"))
    }
  }

  def main(args: Array[String]) {
    val tag = args(0)
    val dirName = s"$tag-${new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date)}"
    val webPath = Paths.get(args(1), dirName).toAbsolutePath.toString
    val logDir = Paths.get("/tmp", dirName).toAbsolutePath.toString
    Files.createDirectory(Paths.get(logDir))
    val serverNames = args(2).split(",")
    val serverGroups = args(3).split(",")
    cleanRemoteProcesses(serverNames, serverGroups)
    doWork(serverNames, serverGroups, "server", logDir)

    if (args.length == 6) {
      val clientNames = args(4).split(",")
      val clientGroups = args(5).split(",")
      cleanRemoteProcesses(clientNames, clientGroups)
      doWork(clientNames, clientGroups, "client", logDir)
    }

    sys.addShutdownHook {
      ChartSender.finish(webPath)
      try {
        Files.move(Paths.get(logDir), Paths.get(webPath, "logs"))
      } catch {
        case _: java.nio.file.NoSuchFileException => deleteDir(Paths.get(logDir))
      }
      System.out.close()
      System.err.close()

      def deleteDir(dir: Path) {
        Files.walkFileTree(dir, new SimpleFileVisitor[Path] {

          override def visitFile(file: Path, attrs: BasicFileAttributes) = {
            Files.delete(file)
            FileVisitResult.CONTINUE
          }

          override def postVisitDirectory(dir: Path, e: IOException) = {
            if (e == null) {
              Files.delete(dir)
            } else {
              throw e
            }
            FileVisitResult.CONTINUE
          }
        })
      }
    }

    futures.foreach(Await.ready(_, Duration.Inf))
  }
}
