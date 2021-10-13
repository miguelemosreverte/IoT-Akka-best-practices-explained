import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cohesion.device.Device
import cohesion.device.protocol.commands
import infrastructure.actor.sharding.ShardedActor
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.actor.crdt.Register
import java.util.UUID

object Main extends App {

  implicit val classicSystem = ActorSystem("example")
  implicit val system = classicSystem.toTyped
  implicit val clusterSharding = ClusterSharding.apply(system)
  implicit val ec = classicSystem.dispatcher

  val timeWindow: Register.Integer = Register.Integer("test", 1)

  import infrastructure.kafka.KafkaSupport.Implicit._
  implicit val kafkaRequirements = KafkaRequirements(
    KafkaBootstrapServer("0.0.0.0:29092"),
    system.classicSystem,
    println
  )

  val publishAverageDeviceTemperature: domain.DeviceRecord => Unit = deviceRecord =>
    domain.DeviceRecord.kafka.producer.plain
      .apply(kafkaRequirements.kafkaBootstrapServer.url)(topic = "DeviceAverageTemperature", deviceRecord)

  val device = ShardedActor.apply("a", Device.apply(publishAverageDeviceTemperature, timeWindow))

  device.ask("1")(ref => commands.AddDevice(domain.Device.example, ref))
  device.ask("1")(ref => commands.AddDeviceRecord(domain.DeviceRecord.example, ref))

  domain.Device.kafka.consumer.transactional.run("DeviceCreation", "writeside") { newDevice =>
    device.ask(newDevice.deviceId.toString)(ref => commands.AddDevice(newDevice, ref))
  }

  domain.DeviceRecord.kafka.consumer.transactional.run("DeviceRecord", "writeside") { deviceRecord =>
    device.ask(deviceRecord.deviceId.toString)(ref => commands.AddDeviceRecord(deviceRecord, ref))
  }

  infrastructure.http.Server.apply(TimeWindowEndpoint(timeWindow), "0.0.0.0", 8080)

}
