import sbt.Keys._

organization := "infrastructure"
name := "serialization"
version := "1.0.0"

scalaVersion := "2.13.3"

// https://mvnrepository.com/artifact/com.typesafe.play/play-json
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.0-RC2"
