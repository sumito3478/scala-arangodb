package info.sumito3478.arango

package object db {
  import connection._
  import collection._
  import scala.concurrent._
  import scala.language.dynamics
  import com.ning.http.client._

  case class DatabaseInfo(name: String, id: String, path: String, isSystem: Boolean)
  private[this] case class DatabaseInfoResponse(result: DatabaseInfo)

  case class User(username: String, passwd: Option[String], active: Boolean, extra: json.JObject)

  private[this] case class DatabaseCreation(name: String, users: Seq[User])

  sealed trait DatabaseLike extends Dynamic {
    self =>
    protected[arango] val _connection: Connection
    protected[this] def _api: String
    private[arango] def _dispatcher[A: Manifest] = _connection.Dispatcher[A](url = _api)
    def selectDynamic(name: String): Collection = Collection(name, this)
    /**
     * Retrieves information about the current database
     */
    def _info(implicit ec: ExecutionContext): Future[ArangoResult[DatabaseInfo]] = {
      for (res <- _dispatcher[DatabaseInfoResponse].GET / "database/current" dispatch ())
        yield res.copy(result = res.result.result)
    }
    def _collections: Seq[Collection] = {
      //      val req = new RequestBuilder().setUrl(s"$_api/collection").build

      ???
    }
    def _create(name: String) = ???
    def _drop(name: String) = ???
    def _query(query: String) = ???
    def _database(name: String): DatabaseLike = ???
    def _createDatabase(name: String) = ???
    def _listDatabases = ???
  }
  case class Database(_name: String, _connection: Connection) extends DatabaseLike {
    def _api = s"${_connection._baseUrl}/_db/${_name}/_api"
  }
  case class DefaultDatabase(_connection: Connection) extends DatabaseLike {
    def _api = s"${_connection._api}"
  }
  case class SystemDatabase(_connection: Connection) extends DatabaseLike {
    def _api = s"${_connection._baseUrl}/_db/_system/_api"
    /**
     * Retrieves the list of all existing databases
     */
    def _database(implicit ec: ExecutionContext) =
      for (names <- _dispatcher[Names].GET / "database" dispatch ())
        yield names.copy(result = names.result.result)
    /**
     * Creates a new database
     */
    def _createDatabase(name: String, users: Seq[User])(implicit ec: ExecutionContext): Future[ArangoResult[Unit]] =
      for (result <- _dispatcher[json.JObject].POST / "database" dispatch ())
        yield result.copy(result = ())
    /**
     * Deletes the database along with all data stored in it
     */
    def _deleteDatabase(name: String)(implicit ec: ExecutionContext): Future[ArangoResult[Unit]] =
      for (result <- _dispatcher[json.JObject].DELETE / s"database/$name" dispatch ())
        yield result.copy(result = ())
  }
  trait CollectionSelector {
    def selectDynamic(name: String): Collection
  }
}