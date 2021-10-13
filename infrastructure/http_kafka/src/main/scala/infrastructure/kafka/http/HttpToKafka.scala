package infrastructure.kafka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{as, complete, decodeRequest, entity, path, post}
import akka.http.scaladsl.server.Route
import infrastructure.kafka.http.helper.KafkaWriter
import infrastructure.serialization.algebra.Deserializer.{`failed to deserialize`, DeserializationValidator}

import scala.concurrent.{ExecutionContext, Future}

object HttpToKafka {

  case class KafkaEndpoint(url: String)

  case class TopicToWrite(
      endpointName: String,
      topic: String,
      deserializationValidator: DeserializationValidator,
      example: String
  )

  object Responses {
    def `FAILED TO DECODE`: `failed to deserialize` => HttpResponse =
      e => HttpResponse(StatusCodes.BadRequest, entity = s"Failed to decode: ${e.explanation}")

    val `FAILED TO PUBLISH TO KAFKA` =
      HttpResponse(StatusCodes.BadRequest, entity = "Failed to publish to Kafka")
    val `PUBLISHED` =
      HttpResponse(StatusCodes.OK, entity = "Done")
  }

  import Responses._

  def apply(topicToWrite: TopicToWrite)(
      implicit
      system: ActorSystem,
      executionContext: ExecutionContext,
      kafkaEndpoint: KafkaEndpoint
  ): Route =
    path(topicToWrite.endpointName) {
      post {
        decodeRequest {
          entity(as[String]) { order =>
            complete {

              topicToWrite.deserializationValidator.validate(order) match {
                case Left(error) =>
                  Responses `FAILED TO DECODE` error
                case Right(_) =>
                  KafkaWriter(kafkaEndpoint.url)(
                    topicToWrite.topic,
                    order
                  ).recoverWith {
                      case not_published =>
                        Future.successful(
                          `FAILED TO PUBLISH TO KAFKA`
                        )
                    }
                    .map { published =>
                      `PUBLISHED`
                    }

              }
            }
          }
        }
      }
    }

}
