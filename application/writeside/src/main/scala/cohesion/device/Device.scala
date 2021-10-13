package cohesion.device

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.LWWRegisterKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}
import cohesion.device.protocol.commands
import cohesion.device.protocol.commands.{Inform, LikeUsual, ScheduleNextInform}
import domain.{Device, DeviceRecord}
import infrastructure.actor.crdt.Register

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Device {

  sealed trait DeviceState
  case class Empty(id: String) extends DeviceState
  sealed trait hasDevice extends DeviceState {
    val device: Device
  }
  sealed trait hasRecords extends DeviceState {
    val records: Seq[DeviceRecord]
  }
  case class CreatedDevice(device: Device) extends hasDevice
  case class DeviceRecords(
      device: Device,
      records: Seq[DeviceRecord]
  ) extends hasDevice
      with hasRecords

  def apply(inform: Float => Unit)(id: String): Behavior[commands] = {
    Behaviors.setup { implicit context =>
      val timeWindow: Register.Integer = Register.Integer("test", 1)

      def scheduleNextInform(in: FiniteDuration) =
        context.pipeToSelf(timeWindow.get) {
          case Failure(exception) => ScheduleNextInform(in)
          case Success(n: Int) => ScheduleNextInform(n.seconds)
        }

      context.self ! ScheduleNextInform(1 second)

      DurableStateBehavior[commands, DeviceState](
        persistenceId = PersistenceId.ofUniqueId(s"Device-${id}"),
        emptyState = Empty(id),
        commandHandler = { (state: DeviceState, command: commands) =>
          command match {

            case Inform(in) =>
              state match {
                case state: hasDevice with hasRecords =>
                  val average = state.records.map(_.currentValue).sum / state.records.size
                  inform(average)
                case _ =>
              }
              println(s"Scheduling next inform in ${in}")
              scheduleNextInform(in)
              Effect.none

            case ScheduleNextInform(in) =>
              println(s"Received schedule inform in ${in}")
              context.scheduleOnce(in, context.self, commands.Inform(in))
              Effect.none

            case commands.AddDevice(device, replyTo) =>
              Effect.persist(CreatedDevice(device)).thenRun { state =>
                replyTo ! Right()
              }
            case commands.AddDeviceRecord(record, replyTo) =>
              state match {
                case state: hasDevice with hasRecords =>
                  Effect.persist(DeviceRecords(state.device, state.records :+ record)).thenRun { state =>
                    replyTo ! Right()
                  }
                case _ =>
                  replyTo ! Right()
                  Effect.none
              }
          }

        }
      )
    }
  }

}
