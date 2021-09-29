package infrastructure.actor.persistence

import akka.Done
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.state.DurableStateStoreProvider
import akka.persistence.state.scaladsl.{DurableStateStore, DurableStateUpdateStore, GetObjectResult}
import com.typesafe.config.{Config, ConfigFactory}
import infrastructure.actor.ActorTestSuite
import infrastructure.actor.ExampleActor._
import infrastructure.actor.sharding.ShardedActor
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
/*
object PersistentActorSpec {
  class MockPersistenceProvider extends DurableStateStoreProvider {
    override def scaladslDurableStateStore(): DurableStateStore[Any] = new InmemDurableStateStore
    override def javadslDurableStateStore() = ???
  }

  class MockPersistence[A] extends DurableStateStore[A] {
    val store = new TrieMap[String, A]()
    def getObject(persistenceId: String): Future[GetObjectResult[A]] = {
      Future.successful(GetObjectResult(store.get(persistenceId), 0))
    }
  }
}*/
class PersistentActorSpec extends ActorTestSuite {

  "Persistent actors should be recover state after complete system shutdown" in {

    {
      implicit val system = ActorSystem.start
      implicit lazy val sharding = ClusterSharding.apply(system)
      val sharded = ShardedActor[Increment](
        uniqueName = "PersistentCounterActor",
        behavior = PersistentCounterActor.apply
      )
      for {
        a <- sharded.ask("A")(Increment)
        b <- sharded.ask("A")(Increment)
        c <- sharded.ask("A")(Increment)
        done <- ActorSystem.stop(system)
      } yield Seq(a, b, c) should be(Seq(1, 2, 3))
    }

    Thread.sleep(5000)

    {
      implicit val system = ActorSystem.start
      implicit lazy val sharding = ClusterSharding.apply(system)
      val sharded = ShardedActor[Increment](
        uniqueName = "PersistentCounterActor",
        behavior = PersistentCounterActor.apply
      )
      for {
        a <- sharded.ask("A")(Increment)
        b <- sharded.ask("A")(Increment)
        c <- sharded.ask("A")(Increment)
      } yield Seq(a, b, c) should be(Seq(4, 5, 6))
    }

  }
}
