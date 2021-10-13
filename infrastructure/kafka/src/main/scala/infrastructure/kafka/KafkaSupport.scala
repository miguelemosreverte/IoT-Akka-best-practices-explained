package infrastructure.kafka

import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import infrastructure.kafka.consumer.{CommitableSource, PlainSource, TransactionalSource}
import infrastructure.kafka.consumer.logger.Logger
import infrastructure.kafka.producer.TransactionalProducer
import infrastructure.serialization.algebra.{Deserializer, Serialization, Serializer}

trait KafkaSupport[A] {
  implicit val deserializer: Deserializer[A]
  implicit val serializer: Serializer[A]
  object kafka {
    object consumer {
      def `plain`(implicit requirements: KafkaRequirements) = new PlainSource
      def `commit`(implicit requirements: KafkaRequirements) = new CommitableSource
      def `transactional`(implicit requirements: KafkaRequirements) = new TransactionalSource
    }
    object producer {
      def `plain`(implicit requirements: KafkaRequirements) = new TransactionalProducer
    }
  }

}

object KafkaSupport {

  object Protocol {
    import akka.actor.ActorSystem

    case class KafkaRequirements(
        kafkaBootstrapServer: KafkaBootstrapServer,
        actorSystem: ActorSystem,
        logger: Logger,
        startFromZero: Boolean = true
    )
    case class KafkaBootstrapServer(url: String)

  }
  import scala.language.implicitConversions
  object Implicit {
    implicit def fromDeserializer[A](s: Serialization[A]): KafkaSupport[A] =
      new KafkaSupport[A] {
        val deserializer = s.deserialize
        val serializer = s.serialize
      }
  }

}
