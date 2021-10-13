package infrastructure.kafka.consumer.settings

import akka.kafka.{ConsumerSettings, ProducerSettings}
import infrastructure.kafka.KafkaSupport.Protocol.KafkaRequirements
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import java.time.Duration

object Consumer {
  def apply(implicit requirements: KafkaRequirements) = {
    println(s"requirements.kafkaBootstrapServer.url ${requirements.kafkaBootstrapServer.url}")
    ConsumerSettings
      .create(requirements.actorSystem, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(requirements.kafkaBootstrapServer.url)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  }

}
