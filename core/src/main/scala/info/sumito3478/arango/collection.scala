package info.sumito3478.arango

package object collection {
  import db._
  import doc._
  import scala.concurrent._
  import scalaz._, Scalaz._
  import argonaut._, Argonaut._
  import macros.casecodec
  import scala.language.implicitConversions
  implicit def mapToJson(map: Map[String, Any]) = JsonObject(InsertionMap[String, Json](map.mapValues(v => jString(v.toString)).toSeq: _*))
  case class Document(_id: String, _rev: String, _key: String)
  implicit def DocumentCodecJson = casecodec[Document]
  case class EnsuringIndex(`type`: String, unique: Boolean, fields: List[String])
  implicit def EnsuringIndexCodecJson = casecodec[EnsuringIndex]
  //  implicit def EnsuringIndexCodecJson = casecodec3(EnsuringIndex.apply, EnsuringIndex.unapply)("type", "unique", "fields")
  case class EnsuringIndexResult(isNewlyCreated: Boolean, id: String, fields: List[String], `type`: String, unique: Boolean)
  implicit def EnsuringIndexResultCodecJson = casecodec[EnsuringIndexResult]
  case class QueryByExample(collection: String, example: Json, skip: Option[Int], limit: Option[Int])
  implicit def QueryByExampleCodecJson = casecodec[QueryByExample]
  case class QueryResult(hasMore: Boolean, count: Int, result: List[Json])
  implicit def QueryResultCodecJson = casecodec[QueryResult]
  //  implicit def QueryResultCodecJson =
  case class Collection(name: String, parent: DatabaseLike) {
    def _connection = parent._connection
    def _api: String = _connection._api
    def _dispatcher[A: DecodeJson] = parent._dispatcher[A]
    def document[A](id: String)(implicit codec: CodecJson[A], ec: ExecutionContext): Future[ArangoResult[A]] =
      _dispatcher[A].GET / s"document/$name/$id" dispatch ()
    def document[A](id: String, etag: String)(implicit codec: CodecJson[A], ec: ExecutionContext): Future[Option[ArangoResult[A]]] =
      _dispatcher[A].GET / s"document/$name/$id" <:< Map("If-None-Match" -> "etag") dispatchOption ()
    def save[A](doc: A, createCollection: Boolean = false, waitForSync: Boolean = false)(implicit decoder: CodecJson[A], ec: ExecutionContext): Future[ArangoResult[Document]] =
      _dispatcher[Document].POST.copy(body = Some(decoder.encode(doc).spaces2)) / s"document" <<? Seq("collection" -> name, "createCollection" -> createCollection, "waitForSync" -> waitForSync) dispatch ()
    def replace[A](id: String, doc: A, overwirte: Boolean = false, waitForSync: Boolean = false)(implicit decoder: DecodeJson[A], ec: ExecutionContext) = ???
    def ensureHashIndex(fields: String*)(implicit ec: ExecutionContext) =
      _dispatcher[EnsuringIndexResult].POST.copy[EnsuringIndexResult](body = Some(json.write(EnsuringIndex(`type` = "hash", unique = false, fields = fields.toList)))) / s"index" <<? Seq("collection" -> name) dispatch ()
    def byExample[A](example: Map[String, Any], skip: Option[Int], limit: Option[Int])(implicit decoder: DecodeJson[A], ec: ExecutionContext) =
      _dispatcher[QueryResult].PUT.copy[QueryResult](body = Some(json.write(QueryByExample(collection = name, example = jObject(example), skip = skip, limit = limit)))) / s"simple/by-example" dispatch ()
  }
}