package cohesion.mongodb.schema

import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json}

case class DeviceTemperature(_id: String, averageTemperature: Int)

object DeviceTemperature extends `JSON Serialization`[DeviceTemperature] {
  val example = DeviceTemperature("device-1", 10)
  val json: Format[DeviceTemperature] = Json.format
}
