import Main.ctx
import akka.http.scaladsl.server.Directives.{get, path}
import infrastructure.microservice.Microservice.MicroserviceName
import infrastructure.microservice.TransactionMicroservice
import infrastructure.transaction.interpreter.free.FreeTransaction
import io.getquill.Query
import io.scalac.auction.auction.actor.AuctionActor.AuctionCreated
import io.scalac.auction.auction.http.BidEndpoint.Bid
import io.scalac.auction.lot.actor.Actor.{InProgress, LotCreated}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Random, Try}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path.Segment
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Flow
import infrastructure.http.Server
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.microservice.Microservice.MicroserviceName
import infrastructure.microservice.TransactionMicroservice
import infrastructure.serialization.interpreter.`JSON Serialization`
import io.scalac.auction.auction.http.BidEndpoint.Bid
import play.api.libs.json.{Format, Json}

import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps
import scala.util.Random

/*import akka.actor.ActorSystem
import domain.Bid
import infrastructure.kafka.consumer.logger.{Logger, Protocol}
import infrastructure.kafka.KafkaSupport.Protocol._
import infrastructure.kafka.KafkaSupport.Implicit._
import infrastructure.serialization.algebra.Deserializer
 */
object Main extends App with TransactionMicroservice {

  override implicit lazy val name = MicroserviceName("SQL")

  val ctx = {
    import io.getquill._
    import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
    val pgDataSource = new org.postgresql.ds.PGSimpleDataSource()
    pgDataSource.setUser("postgres")
    pgDataSource.setPassword("example")
    val config = new HikariConfig()
    config.setDataSource(pgDataSource)
    val ctx = new PostgresJdbcContext(LowerCase, new HikariDataSource(config))
    ctx
  }
  import ctx._

  // mapping `city` table
  case class Auction(
      id: String
  )
  object Auction extends `JSON Serialization`[Auction] {
    override def example = Auction("1")
    override implicit val json: Format[Auction] = Json.format
  }
  case class Lot(
      id: String,
      auctionId: String
  )
  object Lot extends `JSON Serialization`[Lot] {
    override def example = Lot("1", "1")
    override implicit val json: Format[Lot] = Json.format
  }

  val auctionTable = quote { query[Auction] } //B
  val lotTable = quote { query[Lot] } //B

  def save(auction: Auction)(implicit ec: ExecutionContext) = {
    // Generated SQL:
    // INSERT INTO customer (id,name) VALUES (?, ?) RETURNING name
    val q = quote {
      auctionTable
        .insert(lift(auction)) //F
        .returning(_.id) //G
    }
    run(q)
  }
  def saveLot(lot: Lot)(implicit ec: ExecutionContext) = {
    // Generated SQL:
    // INSERT INTO customer (id,name) VALUES (?, ?) RETURNING name
    val q = quote {
      lotTable
        .insert(lift(lot)) //F
        .returning(_.id) //G
    }
    run(q)
  }

  (1 to 100).map(_.toString) foreach { auctionId =>
    Try(save(Auction(auctionId)))
    (1 to 5).map(_.toString) foreach { lotId =>
      Try(saveLot(Lot(lotId, auctionId)))
    }
  }

  val res11 = ctx.run(query[Auction])
  println(res11)

  val res13 = ctx.run(query[Lot])
  println(res13)

  case class AuctionWithLots(auction: Auction, lots: Seq[Lot])
  object AuctionWithLots extends `JSON Serialization`[AuctionWithLots] {
    override def example = AuctionWithLots(Auction("1"), Seq.empty)
    override implicit val json: Format[AuctionWithLots] = Json.format
  }
  def getAllAuctions: Map[Auction, AuctionWithLots] =
    ctx
      .run(quote {
        for {
          auction <- query[Auction]
          lotsPerAuction <- query[Lot].join(a => a.auctionId == auction.id)
        } yield (auction, lotsPerAuction)
      })
      .groupBy(_._1)
      .map { case (k, v) => (k, AuctionWithLots(k, v.map(_._2))) }

  import infrastructure.kafka.KafkaSupport.Implicit._
  {

    /*{
      "auctionId" : {
        "id" : "1"
      },
      "lots" : [ {
        "id" : {
          "id" : "1"
        },
        "description" : "Relic",
        "minimumPrice" : 10,
        "maximumPrice" : 100
      } ]
    }*/
    AuctionCreated.kafka.consumer.transactional.run("CreatedAuction", "SQL") { auctionCreated =>
      Try {
        save(Auction(auctionCreated.auctionId.id))
        auctionCreated.lots.foreach { lot =>
          saveLot(Lot(lot.id.id, auctionCreated.auctionId.id))
        }
      }
      Future.successful(Right())
    }
  }

  println(Bid.serialize(Bid.example, multiline = false))
  println("getAllAuctions")
  println(getAllAuctions)
  val routes = GetAllAuctionsEndpoint.GetAuction.route { (a: GetAllAuctionsEndpoint.GetAllAuctions) =>
    Future.successful(
      getAllAuctions.keys.toSeq.sortBy(_.id.toInt).slice(a.from, a.to).map(key => getAllAuctions(key))
    )
  }

  HttpServer.addRoute("bid websocket endpoint", routes)
  HttpServer.start("0.0.0.0", 8080)

  override lazy val transactions = Set.empty

} /*extends App {
  implicit lazy val actorSystem = ActorSystem("Readside")
  val logger: Logger = {
    case Protocol.`Failed to deserialize`(topic, msg) =>
      println(s"Failed to deserialize $topic $msg")
    case Protocol.`Failed to process`(topic, msg) =>
      println(s"Failed to process $topic $msg")
    case Protocol.`Processed`(topic, msg) =>
      println(s"Processed $topic $msg")
  }
  implicit val kafkaRequirements = KafkaRequirements(
    KafkaBootstrapServer("0.0.0.0:29092"),
    actorSystem,
    logger
  )

  domain.Bid.kafka.consumer.`commit`.run(
    "bid",
    "default"
  ) {
    case Bid(user, lot, 0) =>
      Left("Bid is zero")
    case Bid(user, lot, bid) =>
      Right(())

  }

}
 */
