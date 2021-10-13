package infrastructure.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{
  ContentType,
  ContentTypes,
  HttpHeader,
  HttpRequest,
  HttpResponse,
  StatusCode,
  StatusCodes
}
import akka.util.ByteString
import cats.data.EitherT
import infrastructure.http.Client.{DeserializationRequestError, HTTP_Request, RequestError, RequestFailed}
import infrastructure.serialization.algebra.Deserializer.`failed to deserialize`
import infrastructure.serialization.algebra.{Deserializer, Serialization, Serializer}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Either

object Client {
  sealed trait RequestError
  case class DeserializationRequestError(error: `failed to deserialize`) extends RequestError
  case class RequestFailed(error: StatusCode, response: String) extends RequestError

  sealed trait HTTP_Request {
    val url: String
    val header: Option[HttpHeader]
    val contentType: ContentType.WithFixedCharset = ContentTypes.`application/json`
  }
  case class GET(url: String, header: Option[HttpHeader] = None) extends HTTP_Request
  case class POST[Body](url: String, body: Body, header: Option[HttpHeader] = None) extends HTTP_Request
  case class PUT[Body](url: String, body: Body, header: Option[HttpHeader] = None) extends HTTP_Request
  case class DELETE(url: String, header: Option[HttpHeader] = None) extends HTTP_Request

}
class Client()(
    implicit
    system: ActorSystem,
    executionContext: ExecutionContext,
    basicHttpCredentials: Option[BasicHttpCredentials] = None
) {

  final def `GET bytes`(
      url: String,
      headers: Option[HttpHeader]
  ): EitherT[Future, RequestError, ByteString] =
    EitherT(http(Client.GET(url, headers), identity))

  final def GET[Response](
      url: String,
      headers: Option[HttpHeader]
  )(
      implicit
      outS: Deserializer[Response]
  ): EitherT[Future, RequestError, Response] =
    deserialize[Response](http(Client.GET(url, headers), identity))

  final def POST[Body, Response](
      url: String,
      body: Body,
      headers: Option[HttpHeader]
  )(
      implicit
      inS: Serialization[Body],
      outS: Serialization[Response]
  ): EitherT[Future, RequestError, Response] = {
    val request = Client.POST(url, body, headers)
    deserialize[Response](http(request, {
      _.withEntity(request.contentType, {
        inS serialize request.body
      })
    }))
  }

  final def PUT[Body, Response](
      url: String,
      body: Body,
      headers: Option[HttpHeader]
  )(
      implicit
      inS: Serialization[Body],
      outS: Serialization[Response]
  ): EitherT[Future, RequestError, Response] = {
    val request = Client.PUT(url, body, headers)
    deserialize[Response](http(request, {
      _.withEntity(request.contentType, {
        inS serialize request.body
      })
    }))
  }

  final def DELETE[Response](
      url: String,
      headers: Option[HttpHeader]
  )(
      implicit
      outS: Deserializer[Response]
  ): EitherT[Future, RequestError, Response] =
    deserialize[Response](http(Client.DELETE(url, headers), identity))

  private final def http(
      hTTP_Request: HTTP_Request,
      addBody: HttpRequest => HttpRequest
  ): Future[Either[RequestFailed, ByteString]] =
    for {
      response: HttpResponse <- Http().singleRequest {
        val requestBuilder: HttpRequest = (hTTP_Request match {
          case _: infrastructure.http.Client.GET =>
            akka.http.scaladsl.client.RequestBuilding.Get
          case _: infrastructure.http.Client.POST[_] =>
            akka.http.scaladsl.client.RequestBuilding.Post
          case _: infrastructure.http.Client.PUT[_] =>
            akka.http.scaladsl.client.RequestBuilding.Put
          case _: infrastructure.http.Client.DELETE =>
            akka.http.scaladsl.client.RequestBuilding.Delete
        }).apply(hTTP_Request.url)
        hTTP_Request.header match {
          case Some(header) =>
            addBody(requestBuilder).withHeaders(header)
          case None =>
            addBody(requestBuilder)
        }

      }
      code = response.status
      _ = println(response.status)
      entity: ByteString <- response.entity.dataBytes
        .runFold(ByteString(""))(_ ++ _)
      _ = println(entity.utf8String)
    } yield {
      code match {
        case akka.http.scaladsl.model.StatusCodes.OK =>
          Right(entity)
        case error: akka.http.scaladsl.model.StatusCode =>
          Left(RequestFailed(error, entity.utf8String))
      }
    }

  private final def deserialize[Response](done: Future[Either[RequestFailed, ByteString]])(
      implicit
      outS: Deserializer[Response]
  ): EitherT[Future, RequestError, Response] =
    EitherT(
      done
        .map {
          case Right(success) =>
            (outS deserialize success.utf8String) match {
              case Left(deserializationError) => Left(DeserializationRequestError(deserializationError))
              case Right(response) => Right(response)
            }
          case Left(requestFailed) => Left(requestFailed)
        }
    )
}
