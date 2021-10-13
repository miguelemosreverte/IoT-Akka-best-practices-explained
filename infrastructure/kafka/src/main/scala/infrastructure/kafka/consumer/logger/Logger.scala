package infrastructure.kafka.consumer.logger

import monitoring.{Monitoring, RED}

trait Logger {
  def log(protocol: Protocol): Unit
}

object Logger {
  class PrometheusLogger()(
      implicit
      monitoring: Monitoring
  ) extends Logger {

    override def log(protocol: Protocol): Unit = protocol match {
      case Protocol.`Failed to deserialize`(topic, msg) =>
        RED recordErrors (s"$topic")

      case Protocol.`Failed to process`(topic, msg) =>
        RED recordErrors (s"$topic")

      case Protocol.`Processed`(topic, msg, timeToProcess) =>
        RED recordDuration (topic, timeToProcess)
    }
  }
}
