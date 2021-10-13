package infrastructure.http

import akka.actor.ActorSystem
import akka.actor.typed.{scaladsl, ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.dispatch.MessageDispatcher
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.{Directive, PathMatcher, Route}
import akka.stream.scaladsl.Flow
import com.typesafe.config.{Config, ConfigFactory}

import java.util.concurrent.{Executor, ExecutorService, Executors, ThreadPoolExecutor, TimeUnit}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.duration.{Duration, DurationInt, SECONDS}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}
import akka.http.scaladsl.server.Directives.{get, path, _}
import akka.util.Timeout
import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

object Server {

  def common = {

    implicit def actorSystem: ActorSystem =
      ActorSystem.create("HTTP_to_Kafka", system_config.withFallback(ConfigFactory.load()))

    // https://doc.akka.io/docs/akka-http/current/handling-blocking-operations-in-akka-http-routes.html
    def system_config = ConfigFactory.parseString("""
                                                    |my-blocking-dispatcher {
                                                    |  type = Dispatcher
                                                    |  executor = "thread-pool-executor"
                                                    |  thread-pool-executor {
                                                    |    fixed-pool-size = 16
                                                    |  }
                                                    |  throughput = 1
                                                    |}
                                                    |""".stripMargin)

    //implicit val blockingDispatcher: MessageDispatcher = actorSystem.dispatchers.lookup("my-blocking-dispatcher")

    (actorSystem) //, blockingDispatcher)
  }

  def onComplete(interface: String, port: Int)(server: Try[Http.ServerBinding])(
      implicit
      actorSystem: ActorSystem,
      executionContext: ExecutionContext
  ) = {
    server match {
      case Success(binding) =>
        println(s"Server online at http://${interface}:${port}\n")

      case Failure(ex) =>
        println(s"Failed to start server, shutting down actor system. Exception is: ${ex.getCause}: ${ex.getMessage}")
        actorSystem.terminate()
    }
  }

  def apply(
      route: Flow[HttpRequest, HttpResponse, Any],
      interface: String,
      port: Int
  )(
      implicit
      actorSystem: ActorSystem,
      executionContext: ExecutionContext
  ) = {
    implicit val (actorSystem) = common
    val server: Future[Http.ServerBinding] = Http().bindAndHandle(route, interface, port)
    server onComplete onComplete(interface, port)
    server
  }

  def apply(
      route: Route,
      interface: String,
      port: Int
  ): Future[Http.ServerBinding] = {
    println("STARTING SERVER")
    implicit val (actorSystem) = common
    val server: Future[Http.ServerBinding] = Http().bindAndHandle(route, interface, port)
    import scala.concurrent.ExecutionContext.Implicits.global
    server onComplete onComplete(interface, port)
    server
  }

  case class AkkaHttpServerConfig(
      interface: String,
      port: Int,
      config: Config
  )
  def `with akka http server`(route: Route)(
      implicit
      akkaHttpServerConfig: AkkaHttpServerConfig,
      actorSystem: ActorSystem
  ): Unit =
    Server(route, akkaHttpServerConfig.interface, akkaHttpServerConfig.port)

}

trait Server {
  import Server._
  val interface: String = "0.0.0.0"
  val port: Int = 8080
  val explanation: String
  val preffix: Directive[Unit]
  val preffixString: String
  protected def selfPath = s"$interface:$port/$preffixString"
  def usefulUrls: Seq[String] = Seq(
    s"$selfPath/examples"
  )

  def examples: Seq[Example]

  def routes: Route = {
    preffix {
      get {
        path("health") {
          complete {
            HttpResponse(OK, entity = "OK")
          }
        }
      } ~
      get {
        path("") {
          complete {

            s"""
               |
               | ${explanation}
               |
               | ${usefulUrls.mkString("\n")}
               |
               |""".stripMargin
          }
        }
      } ~
      get {
        path("examples") {
          complete {

            examples map {
              case example: `GET Example` =>
                `GET Example` serialize example
              case example: `POST Example` =>
                `POST Example` serialize example
            } mkString ("\n")

          }
        }
      }
    }

  }

  case class Routes(routes: Route) {
    def addRoute(route: Route) = copy(routes ~ route)
  }
  //var r: Routes
}
