package infrastructure.microservice

import akka.http.scaladsl.server.Route
import infrastructure.microservice.MicroserviceRequirements.withHttpServer

import scala.concurrent.Future

object withHttpServer {

  object Production {

    import akka.http.scaladsl.server.Directives._

    def apply()(implicit actorSystem: akka.actor.ActorSystem): withHttpServer = new withHttpServer {
      def serve(route: Route, interface: String, port: Int): Future[akka.Done] =
        infrastructure.http.Server
          .apply(route, interface, port)
          .map(_ => akka.Done)(actorSystem.dispatcher)

      def serve(routes: Seq[Route], interface: String, port: Int): Future[akka.Done] =
        serve(routes.reduce(_ ~ _), interface, port)
    }
  }

}
