package cohesion.device

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.LWWRegisterKey
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}
import cohesion.device.protocol.commands
import cohesion.device.protocol.commands.{Inform, LikeUsual, ScheduleNextInform}
import domain.DeviceRecord
import infrastructure.actor.crdt.Register

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Device {
  case class DeviceState(
      lastRecord: Option[DeviceRecord],
      average: Float
  )
}
class Device {
  def apply(id: String): Behavior[commands] = {
    Behaviors.setup { implicit context =>
      val timeWindow = Register.Integer("test")

      def scheduleNextInform(in: FiniteDuration) =
        context.pipeToSelf(timeWindow.get) {
          case Failure(exception) => ScheduleNextInform(in)
          case Success(n: Int) => ScheduleNextInform(n.seconds)
        }

      context.self ! ScheduleNextInform(1 second)

      DurableStateBehavior[commands, Int](
        persistenceId = PersistenceId.ofUniqueId(s"Device-${id}"),
        emptyState = 0,
        commandHandler = { (state: Int, command: commands) =>
          command match {

            case Inform(in) =>
              //inform()
              scheduleNextInform(in)
              Effect.none

            case ScheduleNextInform(in) =>
              context.scheduleOnce(in, context.self, commands.Inform(in))
              Effect.none

            case commands.AddDevice(device, replyTo) =>
              Effect.persist(state + 1).thenRun { state =>
                replyTo ! akka.Done
              }
            case commands.AddDeviceRecord(device, replyTo) =>
              Effect.persist(state + 1).thenRun { state =>
                replyTo ! akka.Done
              }
          }

        }
      )
    }
  }

}
