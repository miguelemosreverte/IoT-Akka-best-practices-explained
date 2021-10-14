import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cohesion.device.Device
import cohesion.device.Device.CreatedDevice
import cohesion.device.protocol.commands
import cohesion.device.protocol.commands.{AddDevice, AddDeviceRecord}
import infrastructure.actor.sharding.ShardedActor
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.actor.crdt.Register

object Main extends App {

  object GuardianActor {
    final case class Start(clientName: String)

    def apply(): Behavior[Start] =
      Behaviors.setup { implicit context =>
        val timeWindow = Register.Integer("test", 1)

        implicit val system = context.system
        implicit val classicSystem = context.system.classicSystem
        implicit val clusterSharding = ClusterSharding.apply(system)
        implicit val ec = classicSystem.dispatcher

        import infrastructure.kafka.KafkaSupport.Implicit._
        implicit val kafkaRequirements = KafkaRequirements(
          KafkaBootstrapServer("0.0.0.0:29092"),
          system.classicSystem,
          println
        )

        infrastructure.http.Server.apply(cohesion.device.TimeWindowEndpoint(timeWindow), "0.0.0.0", 8080)

        val publishAverageDeviceTemperature: domain.DeviceRecord => Unit = deviceRecord =>
          domain.DeviceRecord.kafka.producer.plain
            .apply(kafkaRequirements.kafkaBootstrapServer.url)(topic = "DeviceAverageTemperature", deviceRecord)

        val device = ShardedActor.apply("a", Device.apply(publishAverageDeviceTemperature))

        device.ask("1")(ref => AddDevice(domain.Device.example, ref))
        device.ask("1")(ref => AddDeviceRecord(domain.DeviceRecord.example, ref))

        domain.Device.kafka.consumer.transactional.run("DeviceCreation", "writeside") { newDevice =>
          device.ask(newDevice.deviceId.toString)(ref => commands.AddDevice(newDevice, ref))
        }

        domain.DeviceRecord.kafka.consumer.transactional.run("DeviceRecord", "writeside") { deviceRecord =>
          device.ask(deviceRecord.deviceId.toString)(ref => commands.AddDeviceRecord(deviceRecord, ref))
        }

        Behaviors.receiveMessage { message =>
          Behaviors.same
        }
      }
  }

  ActorSystem(GuardianActor(), "example")

}
