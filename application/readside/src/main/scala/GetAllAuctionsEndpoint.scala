import Main.AuctionWithLots
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import infrastructure.serialization.interpreter.`JSON Serialization`
import io.scalac.auction.auction.Domain.AuctionId
import io.scalac.auction.auction.Protocol.Commands.GetState
import io.scalac.auction.auction.actor.AuctionActor
import io.scalac.auction.auction.actor.AuctionActor.AuctionState
import play.api.libs.json.{Format, Json}

import scala.concurrent.{ExecutionContext, Future}

object GetAllAuctionsEndpoint {

  case class GetAllAuctions(from: Int, to: Int)
  object GetAuction extends `JSON Serialization`[GetAllAuctions] {
    override def example = GetAllAuctions(1, 10)
    override implicit val json: Format[GetAllAuctions] = Json.format

    def route(getAll: GetAllAuctions => Future[Seq[AuctionWithLots]])(implicit ec: ExecutionContext) = {
      println(s"""
      Expecting ${serialize(example)} 
      at auction/1""")
      get {
        path("auctions" / Segment / Segment) { (from, to) =>
          complete {
            getAll(GetAllAuctions(from.toInt, to.toInt)).map { done: Seq[AuctionWithLots] =>
              s"""[
                 |${done
                   .map(auctionWithLots => AuctionWithLots.serialize(auctionWithLots))
                   .mkString(",")}
                 |]""".stripMargin
            }
          }
        }
      }
    }
  }

}
