package infrastructure.kafka.consumer.logger

sealed trait Protocol

object Protocol {
  case class `Failed to deserialize`(topic: String, msg: String) extends Protocol
  case class `Failed to process`(topic: String, msg: String) extends Protocol
  case class `Processed`(topic: String, msg: String, timeToProcess: Long) extends Protocol
}
