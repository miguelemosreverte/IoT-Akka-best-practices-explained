package cohesion

import akka.http.scaladsl.server.Directives.{complete, get, path}
import cohesion.mongodb.schema.DeviceTemperature
import play.api.libs.json.{Json, Writes}

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.Directives._

object GetDevices {
  implicit val json: Writes[Seq[DeviceTemperature]] = Writes.seq(DeviceTemperature.json)

  def route(getDevices: Int => Future[Seq[DeviceTemperature]])(implicit ec: ExecutionContext) = {

    get {
      path("devices" / IntNumber) { pageIndex: Int =>
        complete {
          getDevices(pageIndex).map { done: Seq[DeviceTemperature] =>
            Json prettyPrint json.writes(done)
          }
        }
      }
    }
  }
}
