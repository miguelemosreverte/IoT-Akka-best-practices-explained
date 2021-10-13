import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import infrastructure.serialization.interpreter.`JSON Serialization`
import io.scalac.auction.auction.actor.AuctionActor
import io.scalac.auction.auction.actor.AuctionActor.AuctionState
import mongodb.schema.Auction
import play.api.libs.json.{Format, Json, Reads, Writes}
import mongodb.schema.DeviceTemperature
import scala.concurrent.{ExecutionContext, Future}

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