import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors

object ExampleActor {

  case class Counter(replyTo: ActorRef[Int])
  object CounterActor {
    def empty = CounterActor(0)
    def apply(acc: Int): Behaviors.Receive[Counter] =
      Behaviors.receiveMessage[Counter] {
        case Counter(replyTo) =>
          replyTo ! acc
          CounterActor.apply(acc + 1)
      }
  }

}
