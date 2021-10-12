import sbt.Keys._

organization := "application"
version := "1.0.0"
name := "writeside"
scalaVersion := "2.13.6"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.8" % Test
libraryDependencies += ("infrastructure" %% "actor" % "1.0.0" % "compile->compile;test->test")
libraryDependencies += ("domain" %% "domain" % "1.0.0")

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

fork / run := true
connectInput / run := true
