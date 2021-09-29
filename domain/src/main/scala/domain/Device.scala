package domain

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.Json

case class Device(
    deviceId: String,
    name: String,
    createdAt: Long
)
object Device extends `JSON Serialization`[Device] {
  override def example = Device(
    deviceId = "81b43716-c91c-4e40-a7c4-1fdd390c6418",
    name = "Device A",
    createdAt = 1594146343
  )
  override val json = Json.format
}
