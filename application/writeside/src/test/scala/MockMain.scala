import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import cohesion.Writeside
import infrastructure.kafka.KafkaMock.Implicits.TestSession
import infrastructure.microservice.{withHttpServer, KafkaMock}

import scala.concurrent.Future

object MockMain extends App {
  implicit val system = akka.actor.ActorSystem("example")
  implicit val testSession: TestSession = TestSession()
  implicit val typed = system.toTyped
  implicit val ec = system.dispatcher
  implicit val kafka = KafkaMock()
  implicit val http = withHttpServer.Production()
  val writeside = Writeside()
  kafka.publishToKafka[domain.Device]("DeviceCreation", domain.Device.example)(domain.Device)
  system.spawn[Nothing](writeside.GuardianActor(), "GuardianActor")

}
