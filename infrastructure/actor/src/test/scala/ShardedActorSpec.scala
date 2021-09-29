import ExampleActor._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.typesafe.config.ConfigFactory
import infrastructure.actor.ShardedActor
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.matchers.should.Matchers

class ShardedActorSpec extends AsyncWordSpec with Matchers {

  import akka.actor.typed.scaladsl.adapter._
  implicit lazy val system = akka.actor
    .ActorSystem(
      "ShardedActorSpec",
      ConfigFactory.load
    )
    .toTyped

  implicit lazy val sharding = ClusterSharding.apply(system)

  "Sharded actors should be segregated by id" in {

    val sharded = ShardedActor[Counter](
      uniqueName = "CounterActor",
      behavior = id => CounterActor.empty
    )

    for {
      a <- sharded.ask("A")(Counter)
      b <- sharded.ask("B")(Counter)
      c <- sharded.ask("C")(Counter)
      d <- sharded.ask("C")(Counter)
      e <- sharded.ask("C")(Counter)
    } yield Seq(a, b, c, d, e) should be(Seq(0, 0, 0, 1, 2))

  }
}
