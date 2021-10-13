package infrastructure.kafka.algebra

import akka.stream.scaladsl.SourceQueue

trait MessageProducer[Response] {
  def producer(to: String): SourceQueue[Response]
}
