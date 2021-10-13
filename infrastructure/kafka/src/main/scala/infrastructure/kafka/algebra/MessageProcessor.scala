package infrastructure.kafka.algebra

import akka.Done
import akka.stream.UniqueKillSwitch

import scala.concurrent.Future

trait MessageProcessor[Command] {

  def run(topic: String, group: String)(
      callback: Command => Future[Either[String, Unit]]
  ): (Option[UniqueKillSwitch], Future[Done])

}
