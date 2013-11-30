import sbt._
import Keys._

object Dependencies {
  val logback = "ch.qos.logback" % "logback-classic" % "1.0.13"

  val scalatest = "org.scalatest" %% "scalatest" % "2.0"

  val async_http_client = "com.ning" % "async-http-client" % "1.7.21"

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.5"

  object json4s {
    object constants {
      val version = "3.2.5"
      val name = "json4s"
      val group = "org.json4s"
    }
    import constants._
    val Seq(core) = Seq("core").map(a => group %% s"$name-$a" % version)
  }
  object dispatch {
    object constants {
      val version = "0.11.0"
      val name = "dispatch"
      val group = "net.databinder.dispatch"
    }
    import constants._
    val Seq(core) = Seq("core").map(a => group %% s"$name-$a" % version)
  }
  object play {
    object constants {
      val version = "2.2.1"
      val name = "play"
      val group = "com.typesafe.play"
    }
    import constants._
    val Seq(json) = Seq("json").map(a => group %% s"$name-$a" % version)
  }
  object jackson {
    object constants {
      val version = "2.3.0"
      val name = "jackson"
      object group {
        val prefix = s"com.fasterxml.$name"
        val core = s"$prefix.core"
        val module = s"$prefix.module"
      }
      val module = s"$name-module"
    }
    import constants._
    val Seq(core, databind) = Seq("core", "databind").map(a => group.core % s"$name-$a" % version)
    val Seq(afterburner) = Seq("afterburner").map(a => group.module % s"$module-$a" % version)
    val scala = group.module %% s"$module-scala" % version
  }
  object libraries {
    object constants {
      val test = "test"
    }
    import constants._
    private[this] def d = Dependencies
    val core = Seq(async_http_client, jackson.core, jackson.databind, jackson.afterburner, jackson.scala, slf4j, logback % test)
    val dispatch = core ++ Seq(d.dispatch.core)
    val play = core ++ Seq(d.play.json)
    val json4s = core ++ Seq(d.json4s.core)
  }
}
