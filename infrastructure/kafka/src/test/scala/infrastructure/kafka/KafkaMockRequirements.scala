package infrastructure.kafka

import akka.actor.typed.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueue}
import infrastructure.kafka.KafkaMock.SubscribeTo

case class KafkaMockRequirements(
    onMessage: String => String => Unit,
    receiveMessagesFrom: SubscribeTo => Unit,
    topics: () => Map[String, Seq[String => Unit]] = () => Map.empty,
    eventLog: () => Seq[String],
    messageQueue: SourceQueue[(String, String)]
) {

  def publishToKafka(topic: String, message: String): Unit = {
    messageQueue.offer((topic, message))
    ()
  }

}
object KafkaMockRequirements {

  def apply()(
      implicit
      actorSystem: ActorSystem[_]
  ): KafkaMockRequirements = {
    var topics: Map[String, Seq[String => Unit]] = Map.empty
    var eventLog: Seq[String] = Seq.empty
    val messagesPerTopic: collection.mutable.Map[String, Seq[String]] = collection.mutable.Map.empty

    def messageQueue: SourceQueue[(String, String)] =
      Source
        .queue(bufferSize = 1024, OverflowStrategy.backpressure)
        .to(Flow[(String, String)].to(Sink.foreach {
          case (topic, message) =>
            topics.get(topic) match {
              case Some(subscriptions) =>
                messagesPerTopic(topic) = messagesPerTopic(topic) :+ message
                subscriptions.foreach(_(message))
              case None =>
                topics = topics.+((topic, Seq.empty))
            }
        }))
        .run

    val onMessage: String => String => Unit = { topic => message =>
      println("RECEIVED MESSAGE")
      topics
        .filter(_._1 == topic)
        .foreach(_._2 foreach (_(message)))
    }
    def receiveMessagesFrom: SubscribeTo => Unit = { (s: SubscribeTo) =>
      topics = topics.+(
        (
          s.topic,
          Seq({ message: String =>
            println(
              s"""
                 |${Console.YELLOW} [MessageProducer] ${Console.RESET}
                 |Sending message to: ${(Console.YELLOW + s.topic + Console.RESET)}
                 |${Console.CYAN} $message ${Console.RESET}
                 |""".stripMargin
            )
            eventLog = eventLog :+ message
            s.receiveMessages(message)
          })
        )
      )
      println(s"messagesPerTopic: ${messagesPerTopic}")
      messagesPerTopic.get(s.topic).map(_.foreach(s.receiveMessages))
    }
    KafkaMockRequirements(onMessage, receiveMessagesFrom, () => topics, () => eventLog, messageQueue)
  }
}
