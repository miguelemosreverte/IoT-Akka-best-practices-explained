package cohesion

import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Directives._
import infrastructure.kafka.KafkaSupport.Protocol.{KafkaBootstrapServer, KafkaRequirements}
import infrastructure.microservice.MicroserviceRequirements.{withHttpServer, withKafka}
import mongodb.schema.DeviceTemperature
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.InsertOneOptions
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.language.postfixOps

case class Readside()(implicit kafka: withKafka, http: withHttpServer) {

  object GuardianActor {
    def apply(): Behavior[Nothing] =
      Behaviors.setup[Nothing] { implicit context =>
        val mongoClient: MongoClient = MongoClient("mongodb://0.0.0.0:27017")

        val codecRegistry = fromRegistries(
          fromProviders(
            classOf[DeviceTemperature]
          ),
          DEFAULT_CODEC_REGISTRY
        )

        val database: MongoDatabase = mongoClient.getDatabase("readside").withCodecRegistry(codecRegistry)

        val devices: MongoCollection[DeviceTemperature] = database.getCollection("devices")

        implicit val system = context.system.classicSystem
        implicit val executionContext = system.dispatcher

        kafka.subscribeFromKafka[domain.DeviceRecord](
          topic = "DeviceAverageTemperature",
          group = "MongoDB"
        ) { deviceRecord =>
          devices
            .insertOne(DeviceTemperature(deviceRecord.deviceId.toString, deviceRecord.currentValue.toInt),
                       InsertOneOptions.apply().bypassDocumentValidation(true))
            .toFuture
            .map(_ => Right())
        }(domain.DeviceRecord)

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

        val routes = Seq(
          GetDeviceTemperature.route(getDeviceTemperature),
          GetDevices.route(getDevices)
        ).reduce(_ ~ _)

        infrastructure.http.Server.apply(routes, "0.0.0.0", 8081)

        Behaviors.ignore
      }
  }
}
