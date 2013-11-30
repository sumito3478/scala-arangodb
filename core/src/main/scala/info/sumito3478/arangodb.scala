package info.sumito3478

package object arangodb {
  private[arangodb] case class Logger(name: String) {
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
  private[arangodb] trait Logging {
    lazy val logger = Logger(getClass.getName)
  }
}