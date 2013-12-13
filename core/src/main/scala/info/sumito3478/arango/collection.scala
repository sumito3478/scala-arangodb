package info.sumito3478.arango

package object collection {
  import db._
  import doc._
  import scala.concurrent._
  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  case class Document(_id: String, _rev: String, _key: String)
  case class Collection(name: String, parent: DatabaseLike) {
    def _connection = parent._connection
    def _api: String = _connection._api
    def _dispatcher[A: Manifest] = parent._dispatcher[A]
    def document[A](id: String)(implicit manifest: Manifest[A], ec: ExecutionContext): Future[ArangoResult[A]] =
      _dispatcher[A].GET / s"document/$name/$id" dispatch ()
    def document[A](id: String, etag: String)(implicit manifest: Manifest[A], ec: ExecutionContext): Future[Option[ArangoResult[A]]] =
      _dispatcher[A].GET / s"document/$name/$id" <:< Map("If-None-Match" -> "etag") dispatchOption ()
    def save[A](doc: A, createCollection: Boolean = false, waitForSync: Boolean = false)(implicit manifest: Manifest[A], ec: ExecutionContext): Future[ArangoResult[Document]] =
      _dispatcher[Document].POST.copy(body = Some(json.write(doc))) / s"document" <<? Seq("collection" -> name, "createCollection" -> createCollection, "waitForSync" -> waitForSync) dispatch ()
    def replace[A](id: String, doc: A, overwirte: Boolean = false, waitForSync: Boolean = false)(implicit manifest: Manifest[A], ec: ExecutionContext) = ???
  }
}