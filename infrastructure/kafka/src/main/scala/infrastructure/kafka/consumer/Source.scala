package infrastructure.kafka.consumer

import infrastructure.kafka.consumer.logger.Protocol
import infrastructure.serialization.algebra.Deserializer

import scala.concurrent.{ExecutionContext, Future}

trait Source {
  def now = System.currentTimeMillis
  def process[Command](callback: Command => Future[Either[String, Unit]])(topic: String, message: String)(
      implicit
      deserializer: Deserializer[Command],
      executionContext: ExecutionContext
  ): Future[Protocol] = {
    for {
      output <- deserializer
        .deserialize(message) match {
        case Left(_) =>
          Future.successful(Protocol.`Failed to deserialize`(topic, message))
        case Right(value) =>
          def now = System.currentTimeMillis
          val before = now
          callback(value) map {
            case Left(_) =>
              Protocol.`Failed to process`(topic, message)
            case Right(_) =>
              Protocol.`Processed`(topic, message, now - before)
          }
      }
    } yield output
  }
}
