package infrastructure.http

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._
import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class ServerBuilder(
    implicit
    actorSystem: ActorSystem[Nothing],
    clusterSingleton: ClusterSingleton
) {

  import ServerBuilder.Protocol.`Building a Server`.`Step by Step`.{AddRoute, StartServer}
  import akka.actor.typed.scaladsl.AskPattern._
  implicit val timeout = Timeout(2.seconds)
  implicit val classicSystem = actorSystem.classicSystem

  private def actor = clusterSingleton.init(
    SingletonActor.apply(ServerBuilder.apply(), "ServerBuilder")
  )
  def addRoute(description: String, route: Route) =
    actor.ask[akka.Done](ref => AddRoute(description, route, ref))
  def start(interface: String = "0.0.0.0", port: Int = 8080) =
    actor.ask[Either[String, ServerBinding]](ref => StartServer(interface, port, ref))
}

object ServerBuilder {

  protected object Protocol {

    sealed trait `Building a Server`

    object `Building a Server` {

      sealed trait `Step by Step` extends `Building a Server`

      object `Step by Step` {
        case class AddRoute(
            description: String,
            route: Route,
            ref: ActorRef[akka.Done]
        ) extends `Building a Server`
            with `Step by Step`

        case class StartServer(
            interface: String = "0.0.0.0",
            port: Int = 8080,
            ref: ActorRef[Either[String, ServerBinding]]
        ) extends `Building a Server`
            with `Step by Step`
      }

    }

    object InternalEvents {

      sealed trait InternalEvents extends `Building a Server`

      case class Failed(
          error: String,
          ref: ActorRef[Either[String, ServerBinding]]
      ) extends InternalEvents

      case class Started(
          binding: ServerBinding,
          ref: ActorRef[Either[String, ServerBinding]]
      ) extends InternalEvents

    }

  }
  import Protocol._
  import Protocol.InternalEvents._
  import Protocol.`Building a Server`.`Step by Step`._

  def apply(
      routes: Seq[Route] = Seq.empty,
      started: Option[ServerBinding] = None,
      starting: Boolean = false
  )(implicit actorSystem: akka.actor.ActorSystem): Behavior[`Building a Server`] =
    Behaviors.receive { (context, message) =>
      message match {
        case AddRoute(description: String, route, ref) =>
          ref ! akka.Done
          ServerBuilder.apply(routes :+ route)
        case s @ StartServer(interface, port, ref) =>
          if (!starting) {
            context.scheduleOnce(FiniteDuration(2L, TimeUnit.SECONDS), context.self, s)

            apply(routes, started, starting = true)
          } else {
            started match {
              case Some(binding) =>
                Behaviors.same
              case None =>
                if (routes.isEmpty) {
                  context.self ! Failed("No routes to serve", ref)
                } else {
                  val route = routes.reduce(_ ~ _)
                  implicit val executionContext: ExecutionContextExecutor = context.executionContext
                  context.scheduleOnce(FiniteDuration(5L, TimeUnit.SECONDS), context.self, s)
                  context.pipeToSelf(Server(route, interface, port)) {
                    case Failure(exception) =>
                      Failed(exception.getMessage, ref)
                    case Success(binding) =>
                      Started(binding, ref)
                  }
                }
                Behaviors.same
            }
          }

        case Failed(error, ref) =>
          ref ! Left(error)
          Behaviors.same
        case Started(binding, ref) =>
          ref ! Right(binding)
          ServerBuilder.apply(Seq.empty, Some(binding))
      }
    }
}
