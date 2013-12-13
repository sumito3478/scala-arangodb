import sbt._
import Keys._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform

object Build extends Build {
  lazy val commonSettings = Seq(
    version := "0.0.1-SNAPSHOT",
    javaOptions := Seq("-Xms1024m"),
    organization := "info.sumito3478",
    scalaVersion := "2.10.3",
    crossScalaVersions := Seq("2.10.3"),
    fork := true,
    scalacOptions ++= Seq(
      "-encoding", "utf-8",
      "-target:jvm-1.7",
      "-deprecation",
      "-feature",
      "-unchecked")) ++ scalariformSettings

  lazy val scalariformSettings = SbtScalariform.scalariformSettings ++ Seq(
    SbtScalariform.ScalariformKeys.preferences := FormattingPreferences()
      .setPreference(DoubleIndentClassDeclaration, true))

  import Dependencies.libraries

  implicit class ProjectW(val self: Project) extends AnyVal {
    def arangodb(libs: Seq[ModuleID]) = self.configure(project => project.copy(id = s"scala-arangodb-${project.id}").settings(commonSettings: _*).settings(libraryDependencies ++= libs))
  }

  lazy val core = project.arangodb(libraries.core)

  lazy val dispatch = project.arangodb(libraries.dispatch).dependsOn(core)

  lazy val play = project.arangodb(libraries.play).dependsOn(core)

  lazy val json4s = project.arangodb(libraries.json4s).dependsOn(core)

  lazy val root = project.in(file(".")).arangodb(Seq()).configure(p => p.copy(id = "scala-arangodb")).aggregate(core, dispatch, play, json4s).settings(publishArtifact := false)
}
