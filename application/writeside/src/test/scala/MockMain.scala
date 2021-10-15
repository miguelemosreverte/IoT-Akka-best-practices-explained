import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import infrastructure.kafka.KafkaMock.Implicits.TestSession

import scala.concurrent.Future

object MockMain extends App {
  val system = akka.actor.ActorSystem("example")
  implicit val testSession: TestSession = TestSession()
  implicit val typed = system.toTyped
  implicit val ec = system.dispatcher
  val mock = Mock()
  val main = Main(mock)

  mock.publishToKafka[domain.Device]("DeviceCreation", domain.Device.example)(domain.Device)
  system.spawn[Nothing](main.GuardianActor(), "GuardianActor")

}
