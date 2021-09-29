import sbt.Keys._

organization := "infrastructure"
name := "actor"
version := "1.0.0"

val scalaVer = "2.13.3"
// #deps
val AkkaVersion = "2.6.16"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer
libraryDependencies ++= List(
  // menu
  "com.github.scopt" %% "scopt" % "4.0.1",
  // #deps
  "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
  "com.typesafe.akka" %% "akka-infrastructure.actor.persistence-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % AlpakkaKafkaVersion,
  // test
  "org.scalatest" %% "scalatest" % "3.2.8" % Test,
  // Logging
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
libraryDependencies += "org.rocksdb" % "rocksdbjni" % "6.5.2"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-infrastructure.actor.sharding-typed" % AkkaVersion
libraryDependencies += "org.reflections" % "reflections" % "0.9.10"

fork / run := true
connectInput / run := true
