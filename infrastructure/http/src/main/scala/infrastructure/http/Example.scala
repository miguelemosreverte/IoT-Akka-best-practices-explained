package infrastructure.http

import infrastructure.serialization.algebra.{Deserializer, Serialization}
import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

sealed trait Example
case class `GET Example`(path: String, exampleOutput: String) extends Example
object `GET Example` extends Serialization[`GET Example`] {
  val example = `GET Example`("/", "OK")
  val json: Format[`GET Example`] = Json.format

  override def deserialize(in: String): Either[Deserializer.`failed to deserialize`, `GET Example`] =
    in match {
      case s"""
           |
           |HTTP GET ${path}
           |${exampleOutput}
           |
           |""" => Right(`GET Example`(path, exampleOutput))
      case other => Left(Deserializer.`failed to deserialize`(other))
    }

  override def serialize(item: `GET Example`, multiline: Boolean): String =
    s"""
       |
       |HTTP GET ${item.path}       
       |${item.exampleOutput}
       |
       |""".stripMargin
}
case class `POST Example`(path: String, exampleInput: String, exampleOutput: String) extends Example
object `POST Example` extends Serialization[`POST Example`] {
  val example = `POST Example`("/", "OK", "OK")
  val json: Format[`POST Example`] = Json.format

  override def deserialize(in: String): Either[Deserializer.`failed to deserialize`, `POST Example`] =
    in match {
      case s"""
              |
              |HTTP POST ${path}
              |${exampleInput}
              |${exampleOutput}
              |
              |""" => Right(`POST Example`(path, exampleInput, exampleOutput))
      case other => Left(Deserializer.`failed to deserialize`(other))
    }

  override def serialize(item: `POST Example`, multiline: Boolean): String =
    s"""
       |
       |HTTP POST ${item.path}
       |${item.exampleInput}
       |${item.exampleOutput}
       |
       |""".stripMargin
}
