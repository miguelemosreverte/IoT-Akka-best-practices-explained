package cohesion.device.protocol

import akka.actor.typed.ActorRef
import domain._
import scala.concurrent.duration.FiniteDuration

sealed trait commands
object commands {

  case class Inform(in: FiniteDuration) extends commands
  case object LikeUsual extends commands
  case class ScheduleNextInform(in: FiniteDuration) extends commands

  case class AddDevice(
      device: Device,
      replyTo: ActorRef[Either[String, Unit]]
  ) extends commands

  case class AddDeviceRecord(
      device: DeviceRecord,
      replyTo: ActorRef[Either[String, Unit]]
  ) extends commands

}
