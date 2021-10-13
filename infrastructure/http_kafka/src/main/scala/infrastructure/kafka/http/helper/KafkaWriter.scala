package infrastructure.kafka.http.helper

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

private[http] object KafkaWriter {

  def apply(
      kafkaBootstrapServers: String
  )(
      topic: String,
      item: String
  )(
      implicit
      actorSystem: ActorSystem,
      executionContext: ExecutionContext
  ): Future[Done] = {
    implicit val materializer: Materializer = Materializer.apply(actorSystem)

    val kafkaProducerSettings = ProducerSettings
      .create(actorSystem, new StringSerializer, new StringSerializer)
      .withBootstrapServers(kafkaBootstrapServers)
    Source
      .single(new ProducerRecord[String, String](topic, item))
      .runWith(Producer.plainSink(kafkaProducerSettings))
  }
}
