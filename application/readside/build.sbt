import sbt.Keys._

organization := "application"
version := "1.0.0"
name := "readside"

val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
libraryDependencies += ("ch.qos.logback" % "logback-classic" % "1.2.3")
libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "42.2.8",
  "io.getquill" %% "quill-jdbc" % "3.10.0"
)
libraryDependencies += ("infrastructure" %% "microservice" % "1.0.0")
libraryDependencies += ("application" %% "writeside" % "1.0.0")
fork / run := true
connectInput / run := true