package info.sumito3478.arangodb

package object http {
  import scala.concurrent._
  import com.ning.http.client._

  val defaultUserAgent = "Mozilla/5.0 (compatible; scala-arangodb/1.4; +https://github.com/sumito3478/)"
  trait Executor {
    def client: AsyncHttpClient

    protected implicit def ctx: ExecutionContext

    def apply[A](request: Request, handler: AsyncHandler[A]): Future[A] = {
      val jfuture = client.executeRequest(request, handler)
      val p = Promise[A]()
      jfuture.addListener(new Runnable {
        def run = p.complete(scala.util.Try(jfuture.get))
      }, new java.util.concurrent.Executor {
        def execute(runnable: Runnable) = {
          ctx.execute(runnable)
        }
      })
      p.future
    }
  }
  object Executor {
    def apply(client: AsyncHttpClient)(implicit executionContext: ExecutionContext): Executor = {
      val c = client
      new Executor {
        def client = c

        def ctx = executionContext
      }
    }
    def apply(userAgent: String = defaultUserAgent)(implicit executionContext: ExecutionContext): Executor with Disposable = {
      val builder = new AsyncHttpClientConfig.Builder()
        .setUserAgent(userAgent)
        .setRequestTimeoutInMs(-1)
      new Executor with Disposable {
        def client = new AsyncHttpClient(builder.build)

        def ctx = executionContext

        def disposeInternal = client.close
      }
    }
  }
}