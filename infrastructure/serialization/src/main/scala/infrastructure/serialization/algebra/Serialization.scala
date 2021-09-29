package infrastructure.serialization.algebra

trait Serialization[A] extends Deserializer[A] with Serializer[A]
