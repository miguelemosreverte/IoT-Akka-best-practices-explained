version := "1.0.0"
name := "infrastructure"
scalaVersion := "2.13.6"

lazy val infrastructure = project
  .in(file("."))
  .aggregate(
    actor,
    kafka,
    serialization,
    monitoring
  )

lazy val actor = project.dependsOn(serialization)
lazy val kafka = project.dependsOn(serialization)
lazy val serialization = project
lazy val monitoring = project
