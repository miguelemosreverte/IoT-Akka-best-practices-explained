import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import cohesion.Readside
import infrastructure.microservice.{withHttpServer, withKafka}

object Main extends App {
  implicit val system = akka.actor.ActorSystem("example")
  implicit val kafka = withKafka.Production()
  implicit val http = withHttpServer.Production()
  system.spawn[Nothing](Readside().GuardianActor(), "GuardianActor")
}
