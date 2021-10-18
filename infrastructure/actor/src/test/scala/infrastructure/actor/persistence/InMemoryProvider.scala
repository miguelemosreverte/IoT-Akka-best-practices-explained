package infrastructure.actor.persistence

import akka.Done
import akka.persistence.state.DurableStateStoreProvider
import akka.persistence.state.javadsl.{DurableStateStore => JavaDurableStateStore}
import akka.persistence.state.scaladsl.{DurableStateStore, DurableStateUpdateStore, GetObjectResult}

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

class InMemoryProvider extends DurableStateStoreProvider {
  override def scaladslDurableStateStore(): DurableStateStore[Any] = new InmemDurableStateStore

  override def javadslDurableStateStore(): JavaDurableStateStore[AnyRef] =
    null // TODO
}

class InmemDurableStateStore[A] extends DurableStateUpdateStore[A] {

  val store = new TrieMap[String, A]()

  def getObject(persistenceId: String): Future[GetObjectResult[A]] =
    Future.successful(GetObjectResult(store.get(persistenceId), 0))

  def upsertObject(persistenceId: String, seqNr: Long, value: A, tag: String): Future[Done] =
    Future.successful(store.put(persistenceId, value) match {
      case _ => Done
    })

  def deleteObject(persistenceId: String): Future[Done] =
    Future.successful(store.remove(persistenceId) match {
      case _ => Done
    })
}
