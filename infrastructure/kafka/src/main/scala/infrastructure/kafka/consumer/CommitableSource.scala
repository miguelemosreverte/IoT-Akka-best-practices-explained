package infrastructure.kafka.consumer

import akka.Done
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.ProducerMessage.Envelope
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerMessage, ProducerMessage, Subscriptions}
import akka.kafka.scaladsl.{Committer, Consumer, Producer}
import akka.stream.scaladsl.Keep
import akka.stream.{KillSwitches, UniqueKillSwitch}
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.kafka.consumer.logger.Protocol
import infrastructure.kafka.consumer.logger.Protocol._
import infrastructure.serialization.algebra.Deserializer
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.Future

class CommitableSource[A]()(implicit requirements: KafkaRequirements, deserializer: Deserializer[A])
    extends infrastructure.kafka.consumer.Source {
  private implicit val actorSystem = requirements.actorSystem
  private implicit val executionContext = actorSystem.dispatcher

  def run(topic: String,
          group: String)(callback: A => Future[Either[String, Unit]]): (UniqueKillSwitch, Future[Done]) = {

    val `topic to commit in case of success` = s"${topic}_transactional_success"

    def log: Protocol => Unit = requirements.logger.log
    def commit(
        msg: ConsumerMessage.CommittableMessage[String, String]
    )(output: Protocol): Envelope[String, String, CommittableOffset] =
      output match {
        case _: `Failed to deserialize` =>
          ProducerMessage.passThrough(msg.committableOffset)

        case _: `Failed to process` =>
          ProducerMessage.passThrough(msg.committableOffset)

        case _: `Processed` =>
          ProducerMessage.single(
            new ProducerRecord(
              `topic to commit in case of success`,
              msg.record.key,
              msg.record.value
            ),
            msg.committableOffset
          )
      }

    Consumer
      .committableSource(settings.Consumer.apply.withGroupId(group), Subscriptions.topics(topic))
      .viaMat(KillSwitches.single)(Keep.right)
      .mapAsync(1)((msg: ConsumerMessage.CommittableMessage[String, String]) => {
        for {
          output <- process(callback)(topic, msg.record.value)
        } yield {
          log(output)
          commit(msg)(output)
        }
      })
      .via(Producer.flexiFlow(infrastructure.kafka.producer.settings.Producer.apply))
      .map(_.passThrough)
      .toMat(Committer.sink(akka.kafka.CommitterSettings.apply(actorSystem)))(Keep.both)
      .run()
  }
}
