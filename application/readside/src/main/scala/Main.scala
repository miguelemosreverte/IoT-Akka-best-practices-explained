import akka.http.scaladsl.server.Directives.{get, path}
import infrastructure.microservice.Microservice.MicroserviceName
import infrastructure.microservice.TransactionMicroservice
import infrastructure.transaction.interpreter.free.FreeTransaction
import io.getquill.Query
import io.scalac.auction.auction.actor.AuctionActor.AuctionCreated

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Random, Try}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path.Segment
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Flow
import domain.auction.AuctionId
import domain.lot.LotId
import domain.user.UserId
import infrastructure.http.Server
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.microservice.Microservice.MicroserviceName
import infrastructure.microservice.TransactionMicroservice
import infrastructure.serialization.interpreter.`JSON Serialization`
import mongodb.schema._
import org.mongodb.scala.model.Filters.{equal, regex}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.InsertOneOptions
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}
import play.api.libs.json.{Format, Json}

import scala.language.postfixOps
import scala.util.Random

/*import akka.actor.ActorSystem
import domain.Bid
import infrastructure.kafka.consumer.logger.{Logger, Protocol}
import infrastructure.kafka.KafkaSupport.Protocol._
import infrastructure.kafka.KafkaSupport.Implicit._
import infrastructure.serialization.algebra.Deserializer
 */

object Main extends App {

  val mongoClient: MongoClient = MongoClient("mongodb://0.0.0.0:27017")

  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}

  val codecRegistry = fromRegistries(fromProviders(
                                       classOf[Bid],
                                       classOf[Lot],
                                       classOf[Auction]
                                     ),
                                     DEFAULT_CODEC_REGISTRY)

  val database: MongoDatabase = mongoClient.getDatabase("readside").withCodecRegistry(codecRegistry)

  val devices: MongoCollection[DeviceTemperature] = database.getCollection("devices")

  override implicit lazy val name = MicroserviceName("SQL")

  import infrastructure.kafka.KafkaSupport.Implicit._
  domain.DeviceRecord.Auction.kafka.consumer.transactional.run(
    topic = "DeviceAverageTemperature",
    group = "MongoDB"
  ) { deviceRecord =>
    devices
      .insertOne(DeviceTemperature(deviceRecord.deviceId.toString, deviceRecord.currentValue.toInt), InsertOneOptions.apply().bypassDocumentValidation(true))
      .toFuture
      .map(_ => Right())
  }

  def getDevices(page: Int) = {
    val pageSize = 5
    devices.find().skip(page * pageSize).limit(pageSize).sort(ascending("id")).toFuture
  }
  def getDeviceTemperature(id: String) = {
    devices
      .find(equal("id", id))
      .first()
      .toFuture
  }

  // let's generate some data in MongoDB
  (0 to 100) map { i =>
    devices.insertOne(DeviceTemperature.example.copy(
      id = DeviceTemperature.example.id + s"-$i",
      averageTemperature = 10
    ))
  }

  HttpServer.addRoute("GetDeviceTemperature", GetDeviceTemperature.route(getAution))
  HttpServer.addRoute("GetDevices", GetDevices.route(getAutions))
  HttpServer.start("0.0.0.0", 8080)

}
