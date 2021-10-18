package cohesion

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cohesion.device.Device
import cohesion.device.protocol.commands
import cohesion.device.protocol.commands.{AddDevice, AddDeviceRecord}
import infrastructure.actor.crdt.Register
import infrastructure.actor.sharding.ShardedActor
import infrastructure.microservice.MicroserviceRequirements.{withHttpServer, withKafka}

case class Writeside()(implicit kafka: withKafka, http: withHttpServer) {
  import http._
  import kafka._

  object GuardianActor {
    def apply(): Behavior[Nothing] =
      Behaviors.setup[Nothing] { implicit context =>
        val timeWindow = Register.Integer("test", 1)

        implicit val system = context.system
        implicit val classicSystem = context.system.classicSystem
        implicit val clusterSharding = ClusterSharding.apply(system)
        implicit val ec = classicSystem.dispatcher

        serve(cohesion.device.TimeWindowEndpoint(timeWindow), "0.0.0.0", 8080)

        val publishAverageDeviceTemperature: domain.DeviceRecord => Unit = deviceRecord => {
          println(s"Publishing DeviceRecord (average) to DeviceAverageTemperature -- ${deviceRecord}")
          publishToKafka[domain.DeviceRecord](
            topic = "DeviceAverageTemperature",
            deviceRecord
          )(domain.DeviceRecord)
        }

        val device = ShardedActor.apply("DeviceActor", Device.apply(publishAverageDeviceTemperature))

        subscribeFromKafka[domain.Device](
          topic = "DeviceCreation",
          group = "writeside"
        ) { newDevice =>
          device.ask(newDevice.deviceId.toString)(ref => commands.AddDevice(newDevice, ref))
        }(domain.Device)

        subscribeFromKafka[domain.DeviceRecord](
          topic = "DeviceRecord",
          group = "writeside"
        ) { deviceRecord =>
          println("Received DeviceRecord at writeside")
          device.ask(deviceRecord.deviceId.toString)(ref => commands.AddDeviceRecord(deviceRecord, ref))
        }(domain.DeviceRecord)

        Behaviors.ignore
      }
  }

}
