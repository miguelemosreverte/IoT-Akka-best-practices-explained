package infrastructure.serialization.algebra

import infrastructure.serialization.algebra.Deserializer.`failed to deserialize`

trait Deserializer[A] extends Deserializer.DeserializationValidator {
  def deserialize(in: String): Either[`failed to deserialize`, A]
  final override def validate: String => Either[`failed to deserialize`, Unit] =
    deserialize _ andThen simplify
}

object Deserializer {
  case class `failed to deserialize`(explanation: String)
  trait DeserializationValidator {
    def simplify[A]: Either[`failed to deserialize`, A] => Either[`failed to deserialize`, Unit] = {
      case Left(explanation) => Left(explanation)
      case Right(value) => Right()
    }

    def validate: String => Either[`failed to deserialize`, Unit]
  }
}
