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

  implicit val system = ActorSystem("example").toTyped
  implicit val clusterSharding = ClusterSharding.apply(system)

  val timeWindow: Register.Integer = Register.Integer("test", 1)
  val device = ShardedActor.apply("a", Device.apply(println, timeWindow))

  device.ask("1")(ref => commands.AddDevice(domain.Device.example, ref))
  device.ask("1")(ref => commands.AddDeviceRecord(domain.DeviceRecord.example, ref))

  import infrastructure.kafka.KafkaSupport.Implicit._
  implicit val kafkaRequirements = KafkaRequirements(
    KafkaBootstrapServer("0.0.0.0:29092"),
    system.classicSystem,
    println
  )

  domain.Device.kafka.consumer.transactional.run("DeviceCreation", "writeside") { newDevice =>
    device.ask(newDevice.deviceId.toString)(ref => commands.AddDevice(newDevice, ref))
  }

  domain.DeviceRecord.kafka.consumer.transactional.run("DeviceRecord", "writeside") { deviceRecord =>
    device.ask(deviceRecord.deviceId.toString)(ref => commands.AddDeviceRecord(deviceRecord, ref))
  }

  infrastructure.http.Server.apply(TimeWindowEndpoint(timeWindow), "0.0.0.0", 8080)

}
