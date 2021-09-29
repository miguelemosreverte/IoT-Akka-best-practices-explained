package infrastructure.serialization.algebra

import infrastructure.serialization.algebra.Deserializer.`failed to deserialize`

trait Deserializer[A] {
  def deserialize(in: String): Either[`failed to deserialize`, A]
}

object Deserializer {
  case class `failed to deserialize`(explanation: String)
}
