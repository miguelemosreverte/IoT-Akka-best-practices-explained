package infrastructure.kafka.producer

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, Sink, Source}
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.serialization.algebra.{Deserializer, Serializer}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{ExecutionContext, Future}

class TransactionalProducer[A]()(implicit requirements: KafkaRequirements, serializer: Serializer[A]) {

  def apply(
      kafkaBootstrapServers: String
  )(
      topic: String,
      item: A
  )(
      implicit
      actorSystem: ActorSystem,
      executionContext: ExecutionContext
  ): Future[Done] = {

    val kafkaProducerSettings = ProducerSettings
      .create(actorSystem, new StringSerializer, new StringSerializer)
      .withBootstrapServers(kafkaBootstrapServers)
    Source
      .single(new ProducerRecord[String, String](topic, serializer.serialize(item)))
      .runWith(Producer.plainSink(kafkaProducerSettings))
  }

  def sink[T](topic: String)(
      implicit
      serializer: Serializer[T]
  ): Sink[T, NotUsed] =
    Flow[T]
      .map(message => new ProducerRecord[String, String](topic, serializer serialize message))
      .to(
        Producer
          .plainSink(
            infrastructure.kafka.producer.settings.Producer.apply
          )
      )
}
