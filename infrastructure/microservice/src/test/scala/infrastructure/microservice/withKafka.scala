package infrastructure.microservice

import akka.actor.typed.ActorSystem
import infrastructure.kafka.KafkaMock.Implicits.TestSession
import infrastructure.kafka.algebra.MessageProcessor
import infrastructure.microservice.MicroserviceRequirements.withKafka
import infrastructure.serialization.algebra.Serialization

import scala.concurrent.{ExecutionContext, Future}

object withKafka {

  case class Mock()(implicit system: ActorSystem[_], testSession: TestSession, ec: ExecutionContext) extends withKafka {

    import infrastructure.kafka.KafkaMock.Implicits._

    override def publishToKafka[A](topic: String, message: A)(implicit f: Serialization[A]): Unit =
      toMockProducer(f, kafkaMockRequirements, system).producer(topic).offer(message).foreach(println)

    override def subscribeFromKafka[A](topic: String, group: String)(
        process: A => Future[Either[String, Unit]]
    )(implicit f: Serialization[A]): Unit =
      toMockConsumer(f, kafkaMockRequirements, system).run(topic, group)(process)
  }

}
