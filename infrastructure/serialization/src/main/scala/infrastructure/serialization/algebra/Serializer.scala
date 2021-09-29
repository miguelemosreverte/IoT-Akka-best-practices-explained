package infrastructure.serialization.algebra

trait Serializer[A] {
  def serialize(item: A, multiline: Boolean = true): String
}
