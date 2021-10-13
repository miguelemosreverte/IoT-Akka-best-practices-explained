package infrastructure.kafka

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

import scala.concurrent.Future

object Main extends App {
  case class Bob(id: String = "1")
  implicit object Bob extends `JSON Serialization`[Bob] {
    val example = Bob()
    val json: Format[Bob] = Json.format
  }
  import infrastructure.kafka.KafkaSupport.Implicit._

  implicit val system: ActorSystem = ActorSystem("Example")
  implicit val kafkaRequirements = KafkaRequirements(
    KafkaBootstrapServer("0.0.0.0:29092"),
    system,
    println
  )

  Source
    .fromIterator(() => (1 to 1000).iterator)
    .map { i =>
      Bob.example.copy(id = i.toString)
    }
    .to(
      Bob.kafka.producer.`plain`.sink[Bob]("bob")
    )
    .run()

  Bob.kafka.consumer.`transactional`.run("CreateAuction", "consumer_group2") { bob =>
    println(s"Consumed Bob with Transactional Source: ${bob}")
    Future.successful(Right())
  }
}
