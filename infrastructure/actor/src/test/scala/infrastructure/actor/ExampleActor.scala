package infrastructure.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}

object ExampleActor {

  case class Increment(replyTo: ActorRef[Int])

  object CounterActor {
    def empty = CounterActor(0)
    def apply(state: Int): Behaviors.Receive[Increment] =
      Behaviors.receiveMessage[Increment] {
        case Increment(replyTo) =>
          replyTo ! state
          CounterActor.apply(state + 1)
      }
  }

  object PersistentCounterActor {
    def apply(id: String): Behavior[Increment] = {
      Behaviors.setup { context =>
        DurableStateBehavior[Increment, Int](
          persistenceId = PersistenceId.ofUniqueId(s"PersistentCounterActor-${id}"),
          emptyState = 0,
          commandHandler = { (state: Int, command: Increment) =>
            Effect.persist(state + 1).thenRun { state =>
              command.replyTo ! state
            }
          }
        )
      }
    }
  }

}
