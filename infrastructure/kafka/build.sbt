import sbt.Keys._

organization := "infrastructure"
name := "kafka"
version := "1.0.0"

val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.1.0"

scalaVersion := scalaVer

libraryDependencies += ("infrastructure" %% "monitoring" % "1.0.0")
libraryDependencies += ("com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion)
libraryDependencies += ("infrastructure" %% "actor" % "1.0.0")
libraryDependencies += ("infrastructure" %% "serialization" % "1.0.0")
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.8" % Test
libraryDependencies += ("ch.qos.logback" % "logback-classic" % "1.2.3")
libraryDependencies += ("com.typesafe.akka" %% "akka-stream-kafka" % AlpakkaKafkaVersion)
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding-typed" % "2.6.16"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

publishArtifact in GlobalScope in Test := true

// enable publishing the jar produced by `test:package`
publishArtifact in (Test, packageBin) := true

// enable publishing the test API jar
publishArtifact in (Test, packageDoc) := true

// enable publishing the test sources jar
publishArtifact in (Test, packageSrc) := true

fork / run := true
connectInput / run := true
