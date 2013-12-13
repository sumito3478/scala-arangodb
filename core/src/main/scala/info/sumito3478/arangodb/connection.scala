package info.sumito3478.arangodb

package object connection {
  import db._
  import scala.language.dynamics
  import com.ning.http.client._
  import http._
  import scala.concurrent._
  import scala.util.control.Exception._

  private[arangodb] case class Names(result: Seq[String])

  case class Connection(executor: Executor, host: String = "127.0.0.1", port: Int = 8529, ssl: Boolean = false, user: Option[String] = None, password: Option[String] = None) {
    def db = DefaultDatabase(this)
    def _system = SystemDatabase(this)
    def apply(name: String) = Database(name, this)
    private[arangodb] def _baseUrl = {
      val scheme = if (ssl) "https" else "http"
      s"$scheme://$host:$port"
    }
    private[arangodb] def _api = s"${_baseUrl}/_api"
    /**
     * Retrieves the list of all databases the current user can access without specifying a different username or password
     */
    def _user(implicit ec: ExecutionContext): Future[ArangoResult[Seq[String]]] =
      for (names <- Dispatcher[Names]().GET / "database/user" dispatch ())
        yield names.copy(result = names.result.result)
    private[arangodb] case class Dispatcher[A](url: String = _api, method: String = "GET", body: Option[String] = None, headers: Map[String, String] = Map(), queries: Map[String, String] = Map()) {
      private[this] def dispatchRaw[A](f: Response => A)(implicit manifest: Manifest[A], ec: ExecutionContext): Future[A] = {
        val req = new RequestBuilder().setUrl(url).setMethod(method)
        for (body <- body)
          req.setBody(body)
        for ((k, v) <- headers)
          req.addHeader(k, v)
        for ((k, v) <- queries)
          req.addQueryParameter(k, v)
        executor(req.build, new AsyncCompletionHandler[A] {
          def onCompleted(res: Response) = f(res)
        })
      }
      private[this] def checkError(js: json.JObject): Unit =
        if (js.value.get("error") == Some(json.JBoolean(true))) {
          throw new ArangoException(error = allCatch[ArangoErrorResponse].opt(json.convert[ArangoErrorResponse](js)), response = js)
        }
      def dispatchOption()(implicit manifest: Manifest[A], ec: ExecutionContext): Future[Option[ArangoResult[A]]] =
        dispatchRaw {
          res =>
            if (res.getStatusCode == 304) None
            else {
              val body = res.getResponseBody("UTF-8")
              val js = json.read[json.JObject](body)
              checkError(js)
              Some(ArangoResult(result = json.convert[A](js), raw = js, etag = Option(res.getHeader("etag"))))
            }
        }
      def dispatch()(implicit manifest: Manifest[A], ec: ExecutionContext): Future[ArangoResult[A]] =
        dispatchRaw {
          res =>
            val body = res.getResponseBody("UTF-8")
            val js = json.read[json.JObject](body)
            checkError(js)
            ArangoResult(result = json.convert[A](js), raw = js, etag = Option(res.getHeader("etag")))
        }
      def GET = copy[A](method = "GET")
      def POST = copy[A](method = "POST")
      def PUT = copy[A](method = "PUT")
      def PATCH = copy[A](method = "PATCH")
      def HEAD = copy[A](method = "HEAD")
      def DELETE = copy[A](method = "DELETE")
      def <:<(hs: Traversable[(String, Any)]) = copy[A](headers = headers ++ hs.map { case (k, v) => k -> v.toString })
      def <<?(params: Traversable[(String, Any)]) = copy[A](queries = queries ++ params.map { case (k, v) => k -> v.toString })
      def /(segment: String) = copy[A](url = s"$url/$segment")
    }
}