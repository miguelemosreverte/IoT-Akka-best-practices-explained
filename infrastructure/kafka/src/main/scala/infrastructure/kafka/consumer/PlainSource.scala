package infrastructure.kafka.consumer

import akka.Done
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.kafka.consumer.logger.Protocol
import infrastructure.serialization.algebra.Deserializer

import scala.concurrent.{ExecutionContext, Future}

class PlainSource[A]()(implicit requirements: KafkaRequirements, deserializer: Deserializer[A])
    extends infrastructure.kafka.consumer.Source {
  private implicit val actorSystem = requirements.actorSystem
  private implicit val ec: ExecutionContext = actorSystem.dispatcher

  def run(topic: String,
          group: String)(callback: A => Future[Either[String, Unit]]): (UniqueKillSwitch, Future[Done]) = {
    val source = Consumer
      .plainSource(settings.Consumer.apply.withGroupId(group), Subscriptions.topics(topic))
      .viaMat(KillSwitches.single)(Keep.right)
      .map(_.value)

    def log: Protocol => Unit = requirements.logger.log

    source
      .mapAsync(1) { msg =>
        for {
          output <- process(callback)(topic, msg)
        } yield {
          log(output)
          output
        }
      }
      .toMat(Sink.ignore)(Keep.both)
      .run()
  }

  def source(topic: String, group: String): Source[A, UniqueKillSwitch] = {
    val source: Source[String, UniqueKillSwitch] = Consumer
      .plainSource(infrastructure.kafka.consumer.settings.Consumer.apply.withGroupId(group),
                   Subscriptions.topics(topic))
      .viaMat(KillSwitches.single)(Keep.right)
      .map(_.value)

    source
      .map { msg =>
        deserializer.deserialize(msg)
      }
      .collect {
        case Right(value) => value
      }
  }
}
