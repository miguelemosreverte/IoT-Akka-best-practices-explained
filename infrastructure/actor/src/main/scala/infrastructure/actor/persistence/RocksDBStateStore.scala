package infrastructure.actor.persistence

import akka.actor.ExtendedActorSystem
import akka.serialization.{SerializationExtension, Serializers}
import akka.util.ByteString
import infrastructure.actor.persistence.RocksDBStateStore.withSerializationMetadata
import infrastructure.serialization.interpreter.`JSON Serialization`
import org.rocksdb.{Options, RocksDB, RocksDBException}
import play.api.libs.json.Json

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object RocksDBStateStore {

  RocksDB.loadLibrary()

  val rocksdbOptions = new Options().setCreateIfMissing(true)
  val rocksdb = RocksDB.open(rocksdbOptions, "./rocksdb")
  private implicit def `string to array of bytes`(string: String): Array[Byte] = string.getBytes
  private implicit def `array of bytes to string`(arrayOfBytes: Array[Byte]): String =
    if (arrayOfBytes == null) ""
    else arrayOfBytes.map(_.toChar).mkString("")

  case class withSerializationMetadata(payload: Array[Byte], serId: Int, serManifiest: String)
  object withSerializationMetadata extends `JSON Serialization`[withSerializationMetadata] {
    override def example = withSerializationMetadata(ByteString.emptyByteString.toArray, 1, "")
    override implicit val json = Json.format
  }

  def put(key: String, value: withSerializationMetadata): Either[RocksDBException, Unit] = {
    Try {
      rocksdb.put(key, withSerializationMetadata.serialize(value))
    }.toEither match {
      case Left(value: RocksDBException) => Left(value)
      case Left(unexpected) => throw unexpected
      case Right(value) => Right(value)
    }
  }

  def get(key: String): Option[withSerializationMetadata] = {
    Try {
      rocksdb.get(key)
    }.toOption.flatMap(serialized =>
      if (serialized == null) None
      else
        (withSerializationMetadata deserialize serialized).toOption
    )
  }

}

import akka.Done
import akka.persistence.state.DurableStateStoreProvider
import akka.persistence.state.javadsl.{DurableStateStore => JavaDurableStateStore}
import akka.persistence.state.scaladsl.{DurableStateStore, DurableStateUpdateStore, GetObjectResult}
import akka.serialization.Serialization

import scala.concurrent.Future

class RocksDBProvider(system: ExtendedActorSystem) extends DurableStateStoreProvider {
  private val serialization = SerializationExtension(system)

  override def scaladslDurableStateStore(): DurableStateStore[Any] =
    new RocksDBStateStore(serialization)

  override def javadslDurableStateStore(): JavaDurableStateStore[AnyRef] =
    null // TODO
}

class RocksDBStateStore[A: ClassTag](
    serialization: Serialization
) extends DurableStateUpdateStore[A] {

  def getObject(persistenceId: String): Future[GetObjectResult[A]] = {
    RocksDBStateStore.get(persistenceId) match {
      case None =>
        Future.successful(GetObjectResult(None, 0))
      case Some(r) =>
        deserialize(r.payload.toArray, r.serId, r.serManifiest) match {
          case Failure(exception) =>
            Future.failed(exception)
          case Success(value) =>
            Future successful GetObjectResult(Some(value), 0)
        }

    }
  }

  def upsertObject(persistenceId: String, seqNr: Long, value: A, tag: String): Future[Done] =
    serialize(value) match {
      case Failure(exception) => Future failed exception
      case Success(serialized) =>
        RocksDBStateStore.put(
          persistenceId,
          withSerializationMetadata(serialized._1.toArray, serialized._2, serialized._3)
        ) match {
          case Left(exception) => Future failed exception
          case Right(value) => Future successful Done
        }
    }

  def deleteObject(persistenceId: String): Future[Done] =
    Future.successful(Done) // TODO

  private def serialize(payload: Any): Try[(ByteString, Int, String)] = {
    val p2 = payload.asInstanceOf[AnyRef]
    val serializer = serialization.findSerializerFor(p2)
    val serManifest = Serializers.manifestFor(serializer, p2)
    val serialized = serialization.serialize(p2)
    serialized.map(payload => (ByteString(payload), serializer.identifier, serManifest))
  }

  private def deserialize(bytes: Array[Byte], serId: Int, serManifest: String): Try[A] = {
    serialization.deserialize(bytes, serId, serManifest).map(_.asInstanceOf[A])
  }
}
