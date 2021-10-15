import akka.actor
import akka.actor.typed.javadsl.ActorContext
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cohesion.device.Device
import cohesion.device.Device.CreatedDevice
import cohesion.device.protocol.commands
import cohesion.device.protocol.commands.{AddDevice, AddDeviceRecord}
import com.typesafe.config.ConfigFactory
import infrastructure.actor.sharding.ShardedActor
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.actor.crdt.Register
import infrastructure.serialization.algebra.{Deserializer, Serialization, Serializer}

import scala.concurrent.Future
import scala.util.Try

trait Microservice {
  def publishToKafka[A](topic: String, message: A)(implicit f: Serialization[A])
  def subscribeFromKafka[A](topic: String, group: String)(process: A => Future[Either[String, Unit]])(
      implicit f: Serialization[A]
  )
}
case class Production(classicSystem: akka.actor.ActorSystem) extends Microservice {

  implicit val config = ConfigFactory.load()
  implicit val executionContext = classicSystem.dispatcher
  val kafkaBootstrapServer = KafkaBootstrapServer(Try(config.getString("kafka.url")) getOrElse "0.0.0.0:29092")
  implicit val kafkaRequirements = KafkaRequirements(
    kafkaBootstrapServer,
    classicSystem,
    println
  )

  override def subscribeFromKafka[A](topic: String, group: String)(
      process: A => Future[Either[String, Unit]]
  )(implicit f: Serialization[A]): Unit =
    infrastructure.kafka.KafkaSupport.Implicit
      .fromDeserializer(f)
      .kafka
      .consumer
      .transactional
      .run(topic, group)(process)
  override def publishToKafka[A](topic: String, message: A)(implicit f: Serialization[A]): Unit =
    infrastructure.kafka.KafkaSupport.Implicit
      .fromDeserializer(f)
      .kafka
      .producer
      .plain
      .apply(kafkaBootstrapServer.url)(topic, message)(classicSystem, classicSystem.dispatcher)
}

object MainApp extends App {
  val system = akka.actor.ActorSystem("example")
  system.spawn[Nothing](Main(Production(system)).GuardianActor(), "GuardianActor")
}

case class Main(m: Microservice) {

  import m._

  object GuardianActor {
    def apply(): Behavior[Nothing] =
      Behaviors.setup[Nothing] { implicit context =>
        val timeWindow = Register.Integer("test", 1)

        implicit val system = context.system
        implicit val classicSystem = context.system.classicSystem
        implicit val clusterSharding = ClusterSharding.apply(system)
        implicit val ec = classicSystem.dispatcher

        infrastructure.http.Server.apply(cohesion.device.TimeWindowEndpoint(timeWindow), "0.0.0.0", 8080)

        val publishAverageDeviceTemperature: domain.DeviceRecord => Unit = deviceRecord =>
          publishToKafka[domain.DeviceRecord](
            topic = "DeviceAverageTemperature",
            deviceRecord
          )(domain.DeviceRecord)

        val device = ShardedActor.apply("a", Device.apply(publishAverageDeviceTemperature))

        device.ask("1")(ref => AddDevice(domain.Device.example, ref))
        device.ask("1")(ref => AddDeviceRecord(domain.DeviceRecord.example, ref))

        subscribeFromKafka[domain.Device](
          topic = "DeviceCreation",
          group = "writeside"
        ) { newDevice =>
          println("RECEIVED DEVICECREATION HERE! MIGUEL! HERE!")
          device.ask(newDevice.deviceId.toString)(ref => commands.AddDevice(newDevice, ref))
        }(domain.Device)

        subscribeFromKafka[domain.DeviceRecord](
          topic = "DeviceRecord",
          group = "writeside"
        ) { deviceRecord =>
          device.ask(deviceRecord.deviceId.toString)(ref => commands.AddDeviceRecord(deviceRecord, ref))
        }(domain.DeviceRecord)

        Behaviors.ignore
      }
  }

}
