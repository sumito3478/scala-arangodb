package info.sumito3478.arango

package object connection {
  import db._
  import scala.language.dynamics
  import com.ning.http.client._
  import http._
  import scala.concurrent._
  import scala.util.control.Exception._
  import scalaz._, Scalaz._
  import argonaut._, Argonaut._
  import macros._

  private[arango] case class Names(result: List[String])

  private[arango] implicit def NamesCodecJson = casecodec[Names]

  case class Connection(executor: Executor, host: String = "127.0.0.1", port: Int = 8529, ssl: Boolean = false, user: Option[String] = None, password: Option[String] = None) {
    def db = DefaultDatabase(this)
    def _system = SystemDatabase(this)
    def apply(name: String) = Database(name, this)
    private[arango] def _baseUrl = {
      val scheme = if (ssl) "https" else "http"
      s"$scheme://$host:$port"
    }
    private[arango] def _api = s"${_baseUrl}/_api"
    /**
     * Retrieves the list of all databases the current user can access without specifying a different username or password
     */
    def _user(implicit ec: ExecutionContext): Future[ArangoResult[Seq[String]]] =
      for (names <- Dispatcher[Names]().GET / "database/user" dispatch ())
        yield names.copy(result = names.result.result)
    private[arango] case class Dispatcher[A](url: String = _api, method: String = "GET", body: Option[String] = None, headers: Map[String, String] = Map(), queries: Map[String, String] = Map()) {
      private[this] def dispatchRaw[A](f: Response => A)(implicit ec: ExecutionContext): Future[A] = {
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
      private[this] def checkError(json: Json): Unit = {
        val errorPL = jObjectPL >=> jsonObjectPL("error") >=> jBoolPL
        if (errorPL.get(json) == Some(true))
          throw new ArangoException(error = json.as[ArangoErrorResponse], response = json)
      }
      def dispatchOption()(implicit codec: CodecJson[A], ec: ExecutionContext): Future[Option[ArangoResult[A]]] =
        dispatchRaw {
          res =>
            if (res.getStatusCode == 304) None
            else {
              val body = res.getResponseBody("UTF-8")
              val js = Parse.parse(body).getOrElse(throw new ArangoDriverException(s"could not parse response: $body"))
              checkError(js)
              val result = codec.decodeJson(js).result match {
                case -\/((str, history)) => throw new ArangoDriverException(s"could not convert response: ${js.spaces2}\n$str\n$history")
                case \/-(result) => result
              }
              Some(ArangoResult(result = result, raw = js, etag = Option(res.getHeader("etag"))))
            }
        }
      def dispatch()(implicit codec: CodecJson[A], ec: ExecutionContext): Future[ArangoResult[A]] =
        dispatchRaw {
          res =>
            val body = res.getResponseBody("UTF-8")
            val js = Parse.parse(body).getOrElse(throw new ArangoDriverException(s"could not parse response: $body"))
            checkError(js)
            val result = codec.decodeJson(js).result match {
              case -\/((str, history)) => throw new ArangoDriverException(s"could not convert response: ${js.spaces2}\n$str\n$history")
              case \/-(result) => result
            }
            ArangoResult(result = result, raw = js, etag = Option(res.getHeader("etag")))
        }
      def dispatchUnit()(implicit ec: ExecutionContext): Future[ArangoResult[Unit]] =
        dispatchRaw {
          res =>
            val body = res.getResponseBody("UTF-8")
            val js = Parse.parse(body).getOrElse(throw new ArangoDriverException(s"could not parse response: $body"))
            checkError(js)
            ArangoResult(result = (), raw = js, etag = Option(res.getHeader("etag")))
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
}