import sbt.Keys._

organization := "infrastructure"
name := "microservice"
version := "1.0.0"

scalaVersion := "2.13.3"

// https://mvnrepository.com/artifact/com.typesafe.play/play-json
libraryDependencies += "infrastructure" %% "kafka" % "1.0.0" % "compile->compile;test->test"
libraryDependencies += "infrastructure" %% "http" % "1.0.0"

val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % Test

publishArtifact in GlobalScope in Test := true

// enable publishing the jar produced by `test:package`
publishArtifact in (Test, packageBin) := true

// enable publishing the test API jar
publishArtifact in (Test, packageDoc) := true

// enable publishing the test sources jar
publishArtifact in (Test, packageSrc) := true
