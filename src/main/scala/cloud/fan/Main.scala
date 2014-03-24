package cloud.fan

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import java.util.concurrent.Executors

/**
 * Created by cloud on 3/21/14.
 */
object Main {

  val futures = ListBuffer.empty[Future[Unit]]
  implicit val threadPool = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(64))

  def doWork(nodes: Array[String], groups: Array[String], nodeType: String, baseLogDir: String) {
    ChartSender.sendNodes(nodeType, nodes)
    for(node <- nodes) {
      ChartSender.sendGroups(nodeType, node, groups)
      for (group <- groups) {
        futures += future {
          analyzeLog(nodeType, node, baseLogDir, group)
        }
      }
    }
  }

  def main(args: Array[String]) {
    val baseLogDir = args(0)
    val webPath = args(1)
    val serverNames = args(2).split(",")
    val serverGroups = args(3).split(",")
    doWork(serverNames, serverGroups, "server", baseLogDir)

    if (args.length == 6) {
      val clientNames = args(4).split(",")
      val clientGroups = args(5).split(",")
      doWork(clientNames, clientGroups, "client", baseLogDir)
    }

    sys.addShutdownHook {
      ChartSender.finish(webPath)
      System.out.close()
      System.err.close()
    }

    futures.foreach(Await.ready(_, Duration.Inf))
  }
}
