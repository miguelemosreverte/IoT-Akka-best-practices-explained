name := "application"
version := "1.0.0"
scalaVersion := "2.13.6"

lazy val `writeside` = project
lazy val `producer` = project

lazy val application = project
  .in(file("."))
  .aggregate(
    writeside,
    producer
  )
