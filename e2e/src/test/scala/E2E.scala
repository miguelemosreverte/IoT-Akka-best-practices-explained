import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import cohesion.Writeside
import cohesion.Readside
import infrastructure.kafka.KafkaMock.Implicits.TestSession
import infrastructure.microservice.{withHttpServer, KafkaMock}

import scala.concurrent.Future

object E2E extends App {
  implicit val system = akka.actor.ActorSystem("example")
  implicit val testSession: TestSession = TestSession()
  implicit val typed = system.toTyped
  implicit val ec = system.dispatcher
  implicit val kafka = KafkaMock()
  implicit val http = withHttpServer.Production()
  val writeside = Writeside()
  val readside = Readside()
  kafka.publishToKafka[domain.Device]("DeviceCreation", domain.Device.example)(domain.Device)
  println("Publishing DeviceRecord")
  kafka.publishToKafka[domain.DeviceRecord]("DeviceRecord", domain.DeviceRecord.example)(domain.DeviceRecord)
  system.spawn[Nothing](writeside.GuardianActor(), "GuardianActor")
  system.spawn[Nothing](readside.GuardianActor(), "GuardianActorReadside")

}
