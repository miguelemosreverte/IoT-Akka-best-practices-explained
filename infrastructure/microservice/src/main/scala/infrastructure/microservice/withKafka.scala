package infrastructure.microservice

import com.typesafe.config.ConfigFactory
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.microservice.MicroserviceRequirements.withKafka
import infrastructure.serialization.algebra.Serialization

import scala.concurrent.Future
import scala.util.Try

object withKafka {

  object Production {
    def apply()(implicit classicSystem: akka.actor.ActorSystem): withKafka = {
      implicit val config = ConfigFactory.load()
      implicit val executionContext = classicSystem.dispatcher
      val kafkaBootstrapServer = KafkaBootstrapServer(Try(config.getString("kafka.url")) getOrElse "0.0.0.0:29092")
      implicit val kafkaRequirements = KafkaRequirements(
        kafkaBootstrapServer,
        classicSystem,
        println
      )

      new withKafka {
        override def subscribeFromKafka[A](topic: String, group: String)(
            process: A => Future[Either[String, Unit]]
        )(implicit f: Serialization[A]): Unit =
          infrastructure.kafka.KafkaSupport.Implicit
            .fromDeserializer(f)
            .kafka
            .consumer
            .transactional
            .run(topic, group)(process)
        override def publishToKafka[A](topic: String, message: A)(implicit f: Serialization[A]): Unit =
          infrastructure.kafka.KafkaSupport.Implicit
            .fromDeserializer(f)
            .kafka
            .producer
            .plain
            .apply(kafkaBootstrapServer.url)(topic, message)(classicSystem, classicSystem.dispatcher)
      }
    }
  }
}
