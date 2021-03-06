package info.sumito3478.arango

import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig

package object http {
  import scala.concurrent._
  import com.ning.http.client._
  import org.jboss.netty.util._
  import org.jboss.netty.channel.socket.nio._

  val defaultUserAgent = "Mozilla/5.0 (compatible; scala-arango/1.4; +https://github.com/sumito3478/)"
  trait Executor {
    def client: AsyncHttpClient

    def apply[A](request: Request, handler: AsyncHandler[A])(implicit ec: ExecutionContext): Future[A] = {
      val jfuture = client.executeRequest(request, handler)
      val p = Promise[A]()
      jfuture.addListener(new Runnable {
        def run = p.complete(scala.util.Try(jfuture.get))
      }, new java.util.concurrent.Executor {
        def execute(runnable: Runnable) = {
          ec.execute(runnable)
        }
      })
      p.future
    }
  }
  object Executor {
    def apply(client: AsyncHttpClient): Executor = {
      val c = client
      new Executor {
        def client = c
      }
    }
    def apply(userAgent: String = defaultUserAgent): Executor with Disposable = {
      val builder = (if ((for (group <- Option(Thread.currentThread.getThreadGroup)) yield group.getName == "trap.exit") == Some(true)) {
        // sbt-interactive session
        class ThreadFactory(shutdown: () => Unit) extends java.util.concurrent.ThreadFactory {
          def newThread(runnable: Runnable) = new Thread(runnable) {
            setDaemon(true)
            override def interrupt = {
              shutdown()
              super.interrupt
            }
          }
        }
        lazy val shuttingDown = new java.util.concurrent.atomic.AtomicBoolean(false)
        lazy val threadFactory: ThreadFactory = new ThreadFactory(() => if (shuttingDown.compareAndSet(false, true)) channelFactory.releaseExternalResources)
        lazy val channelFactory: NioClientSocketChannelFactory = new NioClientSocketChannelFactory(java.util.concurrent.Executors.newCachedThreadPool(threadFactory), 1,
          new NioWorkerPool(java.util.concurrent.Executors.newCachedThreadPool(threadFactory), Runtime.getRuntime.availableProcessors * 2), new HashedWheelTimer(new ThreadFactory(() => ())))
        new AsyncHttpClientConfig.Builder()
          .setAsyncHttpClientProviderConfig(new NettyAsyncHttpProviderConfig().addProperty(NettyAsyncHttpProviderConfig.SOCKET_CHANNEL_FACTORY, channelFactory))
      } else {
        new AsyncHttpClientConfig.Builder()
      }).setUserAgent(userAgent)
        .setRequestTimeoutInMs(-1)
      new Executor with Disposable {
        def client = new AsyncHttpClient(builder.build)

        def disposeInternal = client.close
      }
    }
  }
}