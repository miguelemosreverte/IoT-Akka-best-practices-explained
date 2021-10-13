import sbt.Keys._

organization := "infrastructure"
name := "monitoring"
version := "1.0.0"

lazy val KamonVersion = "2.2.2"

libraryDependencies ++= Seq(
  "io.kamon" %% "kamon-bundle" % KamonVersion,
  "io.kamon" %% "kamon-core" % KamonVersion,
  "io.kamon" %% "kamon-status-page" % KamonVersion,
  "io.kamon" %% "kamon-prometheus" % KamonVersion
)

scalaVersion := "2.13.6"
