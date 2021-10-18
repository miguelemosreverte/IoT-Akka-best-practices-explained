import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import cohesion.Writeside
import infrastructure.microservice._

object Main extends App {
  implicit val system = akka.actor.ActorSystem("example")
  implicit val kafka = withKafka.Production()
  implicit val http = withHttpServer.Production()
  system.spawn[Nothing](Writeside().GuardianActor(), "GuardianActor")
}
