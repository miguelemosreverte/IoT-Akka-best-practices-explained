import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import mongodb.schema.DeviceTemperature
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.InsertOneOptions
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.language.postfixOps

object Main extends App {

  val mongoClient: MongoClient = MongoClient("mongodb://0.0.0.0:27017")

  val codecRegistry = fromRegistries(
    fromProviders(
      classOf[DeviceTemperature]
    ),
    DEFAULT_CODEC_REGISTRY
  )

  val database: MongoDatabase = mongoClient.getDatabase("readside").withCodecRegistry(codecRegistry)

  val devices: MongoCollection[DeviceTemperature] = database.getCollection("devices")

  import infrastructure.kafka.KafkaSupport.Implicit._
  implicit val system: ActorSystem = ActorSystem("Example")
  implicit val ec = system.dispatcher

  implicit val kafkaRequirements = KafkaRequirements(
    KafkaBootstrapServer("0.0.0.0:29092"),
    system,
    println
  )
  domain.DeviceRecord.kafka.consumer.transactional.run(
    topic = "DeviceAverageTemperature",
    group = "MongoDB"
  ) { deviceRecord =>
    devices
      .insertOne(DeviceTemperature(deviceRecord.deviceId.toString, deviceRecord.currentValue.toInt),
                 InsertOneOptions.apply().bypassDocumentValidation(true))
      .toFuture
      .map(_ => Right())
  }

  def getDevices(page: Int) = {
    val pageSize = 5
    devices.find().skip(page * pageSize).limit(pageSize).sort(ascending("id")).toFuture
  }

  // let's generate some data in MongoDB
  (0 to 100)
    .map { i =>
      devices
        .insertOne(
          DeviceTemperature.example.copy(
            id = DeviceTemperature.example.id + s"-$i",
            averageTemperature = 10
          )
        )
    }
    .foreach(_.subscribe(done => println(done)))

  def getDeviceTemperature(id: String) = {
    devices
      .find(equal("id", id))
      .first()
      .toFuture
  }

  val routes = Seq(
    GetDeviceTemperature.route(getDeviceTemperature),
    GetDevices.route(getDevices)
  ).reduce(_ ~ _)

  infrastructure.http.Server.apply(routes, "0.0.0.0", 8080)

}
