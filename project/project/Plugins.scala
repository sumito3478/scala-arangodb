import sbt._
import Keys._

object Plugins extends Build {
  override lazy val projects = Seq(plugins)

  lazy val plugins = Project("plugins", file(".")).settings(
    logLevel := Level.Warn,
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.1"),
    addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.1"),
    addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.2"),
    addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.2"))
}

