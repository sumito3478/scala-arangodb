package info.sumito3478

package object arango {
  private[arango] case class Logger(name: String) {
    private[this] val underlying = org.slf4j.LoggerFactory.getLogger(name)

    def error(msg: => String) = if (underlying.isErrorEnabled) underlying.error(msg)
    def error(msg: => String, e: Throwable) = if (underlying.isErrorEnabled) underlying.error(msg, e)
    def warn(msg: => String) = if (underlying.isWarnEnabled) underlying.warn(msg)
    def warn(msg: => String, e: Throwable) = if (underlying.isWarnEnabled) underlying.warn(msg, e)
    def info(msg: => String) = if (underlying.isInfoEnabled) underlying.info(msg)
    def info(msg: => String, e: Throwable) = if (underlying.isInfoEnabled) underlying.info(msg, e)
    def debug(msg: => String) = if (underlying.isDebugEnabled) underlying.debug(msg)
    def debug(msg: => String, e: Throwable) = if (underlying.isDebugEnabled) underlying.debug(msg, e)
    def trace(msg: => String) = if (underlying.isTraceEnabled) underlying.trace(msg)
    def trace(msg: => String, e: Throwable) = if (underlying.isTraceEnabled) underlying.trace(msg, e)
  }
  private[arango] trait Logging {
    lazy val logger = Logger(getClass.getName)
  }
  import java.util.concurrent.atomic._
  trait Disposable extends Logging {
    def disposeInternal: Unit

    private[this] val disposed = new AtomicBoolean(false)

    def dispose = if (disposed.compareAndSet(false, true)) disposeInternal

    override def finalize =
      if (!disposed.get) {
        logger.warn(s"$this - calling dispose from finalizer!")
        dispose
      }
  }
  object Disposable extends Logging {
    def using[A, B](x: A)(f: A => B)(implicit ev: A => Disposable) = try f(x) finally x.dispose
  }

  case class ArangoErrorResponse(error: Boolean, code: Int, errorNum: Int, errorMessage: String)

  case class ArangoException(error: Option[ArangoErrorResponse], response: json.JObject) extends Exception(error.map(_.errorMessage).getOrElse(json.write(response, json.NumericPrecisionOption.Ignored)))

  case class ArangoResult[A](result: A, raw: json.JObject, etag: Option[String]) {
    override def toString = json.write(raw, json.NumericPrecisionOption.Ignored)
  }
}