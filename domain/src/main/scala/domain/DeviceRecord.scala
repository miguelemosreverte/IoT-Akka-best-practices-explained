package domain

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json._

import java.util.UUID

case class DeviceRecord(
    deviceId: UUID,
    currentValue: Float,
    unit: String,
    timestamp: Long,
    version: Float
)

object DeviceRecord extends `JSON Serialization`[DeviceRecord] {
  override def example = DeviceRecord(
    deviceId = UUID fromString "81b43716-c91c-4e40-a7c4-1fdd390c6418",
    currentValue = 72.5f,
    unit = "Farenheit",
    timestamp = 1594146343,
    version = 1
  )
  override val json = Json.format
}
