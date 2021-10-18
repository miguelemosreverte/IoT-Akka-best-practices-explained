package infrastructure.microservice

import akka.http.scaladsl.server.Route
import infrastructure.serialization.algebra.Serialization

import scala.concurrent.Future

sealed trait MicroserviceRequirements

object MicroserviceRequirements {

  trait withHttpServer extends MicroserviceRequirements {
    def serve(routes: Seq[Route], interface: String, port: Int): Future[akka.Done]
    def serve(route: Route, interface: String, port: Int): Future[akka.Done]

  }

  trait withKafka extends MicroserviceRequirements {
    def publishToKafka[A](topic: String, message: A)(implicit f: Serialization[A])
    def subscribeFromKafka[A](topic: String, group: String)(process: A => Future[Either[String, Unit]])(
        implicit f: Serialization[A]
    )
  }
}
