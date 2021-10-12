package domain

import infrastructure.serialization.interpreter.`JSON Serialization`

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._
import scala.concurrent.duration._
import scala.language.postfixOps

case class TimeWindow(
    timeWindow: FiniteDuration
)

object TimeWindow extends `JSON Serialization`[TimeWindow] {

  def fromSeconds(n: Int): TimeWindow = TimeWindow(n seconds)

  override def example = TimeWindow(
    timeWindow = 1 minute
  )

  implicit object FiniteDurationFormat extends Format[FiniteDuration] {
    def reads(json: JsValue): JsResult[FiniteDuration] = LongReads.reads(json).map(_.seconds)
    def writes(o: FiniteDuration): JsValue = LongWrites.writes(o.toSeconds)
  }

  override val json = Json.format
}
