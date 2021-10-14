import akka.http.scaladsl.server.Directives._
import mongodb.schema.DeviceTemperature

import scala.concurrent.{ExecutionContext, Future}

object GetDeviceTemperature {

  def route(getTemperature: String => Future[DeviceTemperature])(implicit ec: ExecutionContext) = {

    get {
      path("device" / Segment) { id: String =>
        complete {
          getTemperature(id).map { done: mongodb.schema.DeviceTemperature =>
            DeviceTemperature serialize done
          }
        }
      }
    }
  }
}
