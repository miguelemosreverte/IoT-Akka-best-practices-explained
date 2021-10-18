version := "1.0.0"
name := "infrastructure"
scalaVersion := "2.13.6"

lazy val infrastructure = project
  .in(file("."))
  .aggregate(
    actor,
    kafka,
    serialization,
    monitoring,
    http,
    http_kafka,
    microservice
  )

lazy val http_kafka = project.dependsOn(kafka, http, monitoring)
lazy val http = project.dependsOn(serialization, actor, monitoring)
lazy val actor = project.dependsOn(serialization)
lazy val kafka = project.dependsOn(serialization)
lazy val serialization = project
lazy val monitoring = project
lazy val microservice = project
