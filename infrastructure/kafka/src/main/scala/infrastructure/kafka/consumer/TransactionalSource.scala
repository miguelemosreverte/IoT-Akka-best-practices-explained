package infrastructure.kafka.consumer

import akka.Done
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerMessage, ProducerMessage, Subscriptions}
import akka.kafka.scaladsl.Transactional
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.kafka.consumer.logger.Protocol
import infrastructure.kafka.consumer.logger.Protocol._
import infrastructure.serialization.algebra.{Deserializer, Serializer}
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.{ExecutionContext, Future}

class TransactionalSource[Command]()(implicit requirements: KafkaRequirements, deserializer: Deserializer[Command])
    extends infrastructure.kafka.consumer.Source {
  private implicit val actorSystem = requirements.actorSystem
  private implicit val ec: ExecutionContext = actorSystem.dispatcher

  def run(topic: String,
          group: String)(callback: Command => Future[Either[String, Unit]]): (UniqueKillSwitch, Future[Done]) = {

    val `topic to commit in case of errors` = s"${topic}_transactional_error"
    val `topic to commit in case of deserialization error` = s"${topic}_transactional_deserialization_error"
    val `topic to commit in case of success` = s"${topic}_transactional_success"

    val source = Transactional
      .source(settings.Consumer.apply.withGroupId(group), Subscriptions.topics(topic))

    def log: Protocol => Unit =
      requirements.logger.log

    def commit(
        msg: ConsumerMessage.TransactionalMessage[String, String]
    )(output: Protocol): ProducerMessage.Envelope[String, String, ConsumerMessage.PartitionOffset] = {
      ProducerMessage.single(
        new ProducerRecord(
          output match {
            case _: `Failed to deserialize` => `topic to commit in case of deserialization error`
            case _: `Failed to process` => `topic to commit in case of errors`
            case _: `Processed` => `topic to commit in case of success`
          },
          msg.record.key,
          msg.record.value
        ),
        passThrough = msg.partitionOffset
      )
    }

    source
      .mapAsync(1) { (msg: ConsumerMessage.TransactionalMessage[String, String]) =>
        for {
          output <- process(callback)(topic, msg.record.value)
        } yield {
          log(output)
          commit(msg)(output)
        } // TODO add Future recover that does not commit
      }
      .via(Transactional.flow(infrastructure.kafka.producer.settings.Producer.apply, "transactionalId"))
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(Sink.ignore)(Keep.both)
      .run()

  }
}
