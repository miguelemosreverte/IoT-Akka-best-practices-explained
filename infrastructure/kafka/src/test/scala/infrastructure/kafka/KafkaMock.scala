package infrastructure.kafka

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import infrastructure.kafka.algebra.{MessageProcessor, MessageProducer}
import infrastructure.serialization.algebra._
import org.scalatest.Assertion

import java.util.UUID
import scala.concurrent.Future
import scala.reflect.ClassTag

object KafkaMock {
  case class SubscribeTo(topic: String, receiveMessages: String => Unit)

  object Implicits {

    def withSession(then: TestSession => Assertion) = {
      implicit val testSession = TestSession()
      then(testSession)
    }
    case class TestSession(id: UUID = UUID.randomUUID())
    var onePerSession: collection.mutable.Map[UUID, KafkaMockRequirements] = collection.mutable.Map.empty

    implicit def kafkaMockRequirements(
        implicit
        system: ActorSystem[_],
        onlyOnePerTestSession: TestSession
    ): KafkaMockRequirements = {
      onePerSession.get(onlyOnePerTestSession.id) match {
        case Some(hereYouGo) => hereYouGo
        case None =>
          onePerSession.addOne(onlyOnePerTestSession.id, KafkaMockRequirements.apply())
          kafkaMockRequirements
      }
    }

    /*implicit def toMock[Command: ClassTag, Response: ClassTag](
        implicit
        commandSerialization: Serialization[Command],
        responseSerialization: Serialization[Response],
        kafkaMockRequirements: KafkaMockRequirements,
        system: ActorSystem[_]
    ) = new KafkaMock[Command, Response]()*/

    implicit def toMockProducer[Response](
        implicit
        responseSerialization: Serialization[Response],
        kafkaMockRequirements: KafkaMockRequirements,
        system: ActorSystem[_]
    ): MessageProducer[Response] = new MessageProducer[Response] {

      override def producer(topic: String): SourceQueue[Response] =
        Source
          .queue(bufferSize = 1024, OverflowStrategy.backpressure)
          .to(
            Flow[Response]
              .map(responseSerialization.serialize(_))
              .to(Sink.foreach { e =>
                println(s"TRIGGERING ON MESSAGE of kafkaMockRequirements ${e}")
                kafkaMockRequirements.onMessage(topic)(e)
              })
          )
          .run
    }

    implicit def toMockConsumer[Command](
        implicit
        commandSerialization: Serialization[Command],
        kafkaMockRequirements: KafkaMockRequirements,
        system: ActorSystem[_]
    ): MessageProcessor[Command] = new MessageProcessor[Command] {
      println("at toMockConsumer")
      def run(topic: String, group: String)(
          callback: Command => Future[Either[String, Unit]]
      ): (Option[UniqueKillSwitch], Future[Done]) = {
        println("RUNNING CONSUMER at toMockConsumer")
        kafkaMockRequirements.receiveMessagesFrom(
          KafkaMock.SubscribeTo(
            topic, { message: String =>
              commandSerialization deserialize message match {
                case Left(value) => Future.successful(Left(value.explanation))
                case Right(value) => callback(value)
              }
              ()
            }
          )
        )
        (None, Future.successful(Done))
      }
    }
  }
}
