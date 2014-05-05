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
import java.net.InetAddress
import cloud.fan.analyze._

/**
 * Created by cloud on 3/21/14.
 */
object Main {

  val futures = ListBuffer.empty[Future[Unit]]
  implicit val threadPool = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  @volatile var interval = 0

  val HOST_NAME = InetAddress.getLocalHost.getHostName

  def doWork(nodes: Array[String], groups: Array[String], nodeType: String, logDir: String) {
    ChartSender.sendNodes(nodeType, nodes)
    for(node <- nodes) {
      // not support analyze FDF yet
      ChartSender.sendGroups(nodeType, node, groups.map("(^[a-zA-Z]+).*".r.findFirstMatchIn(_).get.group(1)).filterNot(_.contains("FDF")))
      for (group <- groups) {
        futures += future {
          analyzeLog(nodeType, node, logDir, group)
        }
      }
    }
  }

  def main(args: Array[String]) {
    interval = args(0).toInt
    val tag = args(1)
    val dirName = s"$tag-${new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date)}"
    val webPath = Paths.get(args(2), dirName).toAbsolutePath.toString
    val logDir = Paths.get("/tmp", dirName).toAbsolutePath.toString
    Files.createDirectory(Paths.get(logDir))

    def convertLocalhost(node: String) = if (node == "localhost") HOST_NAME else node

    val serverNames = args(3).split(",").map(convertLocalhost)
    val serverGroups = args(4).split(",").map(convertLocalhost)
    doWork(serverNames, serverGroups, "server", logDir)

    if (args.length == 7) {
      val clientNames = args(5).split(",")
      val clientGroups = args(6).split(",")
      doWork(clientNames, clientGroups, "client", logDir)
    }

    sys.addShutdownHook {
      ChartSender.finish(webPath)
      try {
        Files.move(Paths.get(logDir), Paths.get(webPath, "logs"))
      } catch {
        case _: java.nio.file.NoSuchFileException => deleteDir(Paths.get(logDir))
      }

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
