package infrastructure.serialization.interpreter

import infrastructure.serialization.algebra.Deserializer.`failed to deserialize`
import infrastructure.serialization.algebra.{Deserializer, Serialization, Serializer}
import play.api.libs.json.{Format, JsValue, Json}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

abstract class `JSON Serialization`[A] extends Serializer[A] with Deserializer[A] with Serialization[A] {

  implicit val json: Format[A]

  def example: A

  override def serialize(item: A, multiline: Boolean = true): String =
    multiline match {
      case true => Json.prettyPrint(json.writes(item))
      case false => Json.asciiStringify(json.writes(item))
    }

  override def deserialize(text: String): Either[`failed to deserialize`, A] = {
    Try {
      Json.parse(text).asOpt[A]
    } match {
      case Failure(invalidJson) =>
        Left(
          `failed to deserialize`(
            s"""
                 | Failed to decode ${this.getClass.getName}
                 | because of:
                 | ${invalidJson.getMessage}
                 | message that failed was: 
                 | $text
                 | """.stripMargin
          )
        )
      case Success(None) =>
        Left(
          `failed to deserialize`(
            s"""
                 | Failed to decode ${this.getClass.getName}
                 | because of:
                 | ${Json.parse(text).validate}
                 | message that failed was: 
                 | $text
                 | """.stripMargin
          )
        )
      case Success(Some(done)) => Right(done)
    }
  }

}
