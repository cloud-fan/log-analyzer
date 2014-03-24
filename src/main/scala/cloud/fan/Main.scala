package cloud.fan

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import java.util.concurrent.Executors
import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Created by cloud on 3/21/14.
 */
object Main {

  val futures = ListBuffer.empty[Future[Unit]]
  implicit val threadPool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(64))

  def doWork(nodes: Array[String], groups: Array[String], nodeType: String, logDir: String) {
    ChartSender.sendNodes(nodeType, nodes)
    for(node <- nodes) {
      ChartSender.sendGroups(nodeType, node, groups)
      for (group <- groups) {
        futures += future {
          analyzeLog(nodeType, node, logDir, group)
        }
      }
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
    doWork(serverNames, serverGroups, "server", logDir)

    if (args.length == 6) {
      val clientNames = args(4).split(",")
      val clientGroups = args(5).split(",")
      doWork(clientNames, clientGroups, "client", logDir)
    }

    sys.addShutdownHook {
      ChartSender.finish(webPath)
      Files.move(Paths.get(logDir), Paths.get(webPath, "logs"))
      System.out.close()
      System.err.close()
    }

    futures.foreach(Await.ready(_, Duration.Inf))
  }
}
