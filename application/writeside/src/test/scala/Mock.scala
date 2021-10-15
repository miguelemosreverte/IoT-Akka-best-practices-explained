import akka.actor.typed.ActorSystem
import infrastructure.kafka.KafkaMock.Implicits.TestSession
import infrastructure.kafka.algebra.MessageProcessor
import infrastructure.serialization.algebra.Serialization

import scala.concurrent.{ExecutionContext, Future}

case class Mock()(implicit system: ActorSystem[_], testSession: TestSession, ec: ExecutionContext)
    extends Microservice {
  import infrastructure.kafka.KafkaMock.Implicits._
  override def publishToKafka[A](topic: String, message: A)(implicit f: Serialization[A]): Unit = {
    println("OFFERING to KAFKA")
    toMockProducer(f, kafkaMockRequirements, system).producer(topic).offer(message).foreach(println)
  }

  override def subscribeFromKafka[A](topic: String, group: String)(
      process: A => Future[Either[String, Unit]]
  )(implicit f: Serialization[A]): Unit = {
    println("subscribeFromKafka")
    toMockConsumer(f, kafkaMockRequirements, system).run(topic, group)(process)
  }
}
