package infrastructure.actor

import akka.cluster.sharding.typed.ShardingEnvelope
import scala.concurrent.duration.DurationInt
import akka.cluster.sharding.typed.scaladsl._
import scala.language.postfixOps
import scala.reflect.ClassTag
import akka.util.Timeout
import akka.actor.typed._

case class ShardedActor[Command](
    uniqueName: String,
    behavior: String => Behavior[Command]
)(
    implicit
    sharding: ClusterSharding,
    system: ActorSystem[Nothing],
    timeout: Timeout = Timeout(20 seconds)
) {

  private val shardActor: ActorRef[ShardingEnvelope[Command]] = {
    val entityTypeKey: EntityTypeKey[Command] =
      EntityTypeKey apply uniqueName
    sharding.init(Entity(entityTypeKey)(createBehavior = { context =>
      behavior(context.entityId)
    }))
  }

  import akka.actor.typed.scaladsl.AskPattern._
  def ask[Res](id: String)(replyTo: ActorRef[Res] => Command) =
    shardActor.ask[Res](replyTo.andThen(command => ShardingEnvelope(id, command)))

}
