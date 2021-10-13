val scalaVer = "2.13.6"
// #deps
val AkkaVersion = "2.6.14"
val AkkaHttpVersion = "10.1.12"
val AlpakkaKafkaVersion = "2.0.5"

scalaVersion := scalaVer

organization := "infrastructure"
version := "1.0.0"
name := "http_kafka"

libraryDependencies += ("infrastructure" %% "http" % "1.0.0")
libraryDependencies += ("infrastructure" %% "kafka" % "1.0.0")
