import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import infrastructure.kafka.http.HttpToKafka.{KafkaEndpoint, TopicToWrite}

object Producer extends App {

  implicit val classicSystem = ActorSystem("example")
  implicit val ec = classicSystem.dispatcher
  implicit val kafkaEndpoint = KafkaEndpoint("0.0.0.0:29092")

  val routes: Route = Seq(
    infrastructure.kafka.http.HttpToKafka(
      TopicToWrite(
        endpointName = "device",
        topic = "CreateDevice",
        domain.Device,
        domain.Device serialize domain.Device.example
      )
    ),
    infrastructure.kafka.http.HttpToKafka(
      TopicToWrite(
        endpointName = "device_record",
        topic = "DeviceRecord",
        domain.Device,
        domain.Device serialize domain.Device.example
      )
    )
  ).reduce(_ ~ _)

  infrastructure.http.Server.apply(routes, "0.0.0.0", 8080)
}
