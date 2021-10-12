package infrastructure.actor.crdt

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props, Scheduler}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.cluster.ddata.{LWWRegister, LWWRegisterKey, SelfUniqueAddress}
import akka.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object Register {
  object Integer
      extends Register[NumberFormatException, Int](
        60,
        any =>
          Try { any.toString.toInt } match {
            case Failure(error: NumberFormatException) =>
              Left(error)
            case Success(integer) =>
              Right(integer)
          }
      )

  object String
      extends Register[Nothing, String](
        "",
        any => Right(any.toString)
      )

}
abstract class Register[Error, Register](
    default: Register,
    recover: Any => Either[Error, Register]
) {

  def apply(name: String)(implicit context: ActorContext[_]) =
    new Wrapper()(context.executionContext, context.system.scheduler, context.spawnAnonymous(behavior(name)))

  class Wrapper(
      implicit
      executionContext: ExecutionContext,
      scheduler: Scheduler,
      ref: ActorRef[Command]
  ) {
    import akka.actor.typed.scaladsl.AskPattern._
    import akka.util.Timeout
    private implicit val timeout: Timeout = 20.seconds
    type EitherRegister = Either[Error, Register]
    def get: Future[Register] =
      ref.ask[EitherRegister](ref => Command.GetValue(ref)).flatMap {
        case Left(value) => Future.failed(new Exception(value.toString))
        case Right(value) => Future.successful(value)
      }
    def set(value: Register): Future[Register] =
      ref.ask[EitherRegister](ref => Command.SetValue(value)).flatMap {
        case Left(value) => Future.failed(new Exception(value.toString))
        case Right(value) => Future.successful(value)
      }
  }

  sealed trait Command

  object Command {

    case class SetValue(
        value: Register
    ) extends Command

    final case class GetValue(
        replyTo: ActorRef[Either[Error, Register]]
    ) extends Command

    final case class GetCachedValue(
        replyTo: ActorRef[Either[Error, Register]]
    ) extends Command
    case object Unsubscribe extends Command

  }
  private sealed trait InternalCommand extends Command
  private object InternalCommand {
    case class InternalUpdateResponse(
        rsp: Replicator.UpdateResponse[LWWRegister[Register]]
    ) extends InternalCommand

    case class InternalGetResponse(
        rsp: Replicator.GetResponse[LWWRegister[Register]],
        replyTo: ActorRef[Either[Error, Register]]
    ) extends InternalCommand

    case class InternalSubscribeResponse(
        chg: Replicator.SubscribeResponse[LWWRegister[Register]]
    ) extends InternalCommand
  }
  import Command._
  import InternalCommand._

  def behavior(name: String): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      val key: LWWRegisterKey[Register] = LWWRegisterKey(name)
      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress

      // adapter that turns the response messages from the replicator into our own protocol
      DistributedData.withReplicatorMessageAdapter[Command, LWWRegister[Register]] { replicatorAdapter =>
        // Subscribe to changes of the given `key`.
        replicatorAdapter.subscribe(key, InternalSubscribeResponse.apply)

        def updated(cachedValue: Register): Behavior[Command] = {
          Behaviors.receiveMessage[Command] {
            case SetValue(value) =>
              replicatorAdapter.askUpdate(
                askReplyTo =>
                  Replicator.Update(key, LWWRegister.apply(node, cachedValue), Replicator.WriteLocal, askReplyTo)(_ =>
                    LWWRegister.apply(node, value)
                  ),
                InternalUpdateResponse.apply
              )
              Behaviors.same

            case GetValue(replyTo) =>
              replicatorAdapter.askGet(
                askReplyTo => Replicator.Get(key, Replicator.ReadLocal, askReplyTo),
                value => InternalGetResponse(value, replyTo)
              )
              Behaviors.same

            case GetCachedValue(replyTo) =>
              replyTo ! Right(cachedValue)
              Behaviors.same

            case Unsubscribe =>
              replicatorAdapter.unsubscribe(key)
              Behaviors.same

            case internal: InternalCommand =>
              internal match {
                case InternalUpdateResponse(_) => Behaviors.same // ok

                case InternalGetResponse(rsp @ Replicator.GetSuccess(`key`), replyTo) =>
                  replyTo ! recover(rsp.get(key).value)
                  Behaviors.same

                case InternalGetResponse(Replicator.NotFound(key), replyTo) =>
                  replyTo ! Right(cachedValue)
                  Behaviors.same

                case error @ InternalGetResponse(_, _) =>
                  Behaviors.unhandled

                case InternalSubscribeResponse(chg @ Replicator.Changed(`key`)) =>
                  recover(chg.get(key).value) match {
                    case Left(error) =>
                      Behaviors.unhandled
                    case Right(value) =>
                      updated(value)
                  }

                case InternalSubscribeResponse(Replicator.Deleted(_)) =>
                  Behaviors.unhandled // no deletes

                case InternalSubscribeResponse(_) => // changed but wrong key
                  Behaviors.unhandled

              }
          }
        }

        updated(cachedValue = default)
      }
    }
}
