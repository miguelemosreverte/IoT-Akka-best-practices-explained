package cohesion.device

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import infrastructure.serialization.interpreter.`JSON Serialization`
import play.api.libs.json.{Format, Json, Reads, Writes}
import infrastructure.actor.crdt.Register

import scala.concurrent.{ExecutionContext, Future}

object TimeWindowEndpoint {
  def apply(timeWindow: Register.Integer.Wrapper)(implicit ec: ExecutionContext) = {
    get {
      path("time_window" / IntNumber) { window: Int =>
        complete {
          timeWindow.set(window).map(_.toString)
        }
      }
    }
  }
}
