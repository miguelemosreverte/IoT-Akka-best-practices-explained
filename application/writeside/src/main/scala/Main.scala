import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cohesion.device.Device
import cohesion.device.protocol.commands
import infrastructure.actor.sharding.ShardedActor

import java.util.UUID

object Main extends App {

  implicit val system = ActorSystem("example").toTyped
  implicit val clusterSharding = ClusterSharding.apply(system)
  val device = ShardedActor.apply("a", Device.apply(println))

  device.ask("1")(ref => commands.AddDevice(domain.Device.example, ref))
  device.ask("1")(ref => commands.AddDeviceRecord(domain.DeviceRecord.example, ref))
}
