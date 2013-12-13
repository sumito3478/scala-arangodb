package info.sumito3478.arango

package object collection {
  import db._
  import doc._
  import scala.concurrent._
  case class Document(_id: String, _rev: String, _key: String)
  case class EnsuringIndex(`type`: String, unique: Boolean, fields: Seq[String])
  case class EnsuringIndexResult(isNewlyCreated: Boolean, id: String, fields: List[String], `type`: String, unique: Boolean)
  case class QueryByExample(collection: String, example: Map[String, Any], skip: Option[Int], limit: Option[Int])
  case class QueryResult[A](hasMore: Boolean, count: Int, result: Seq[A])
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
    def ensureHashIndex(fields: String*)(implicit ec: ExecutionContext) =
      _dispatcher[EnsuringIndexResult].POST.copy[EnsuringIndexResult](body = Some(json.write(EnsuringIndex(`type` = "hash", unique = false, fields = fields)))) / s"index" <<? Seq("collection" -> name) dispatch ()
    def byExample[A](example: Map[String, Any], skip: Option[Int], limit: Option[Int])(implicit manifest: Manifest[A], ec: ExecutionContext) =
      _dispatcher[QueryResult[A]].PUT.copy[QueryResult[A]](body = Some(json.write(QueryByExample(collection = name, example = example, skip = skip, limit = limit)))) / s"simple/by-example" dispatch ()
  }
}