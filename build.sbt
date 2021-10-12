lazy val root = project
  .in(file("."))
  .settings(
    name := "best-practices",
    version := "0.1.0",
    scalaVersion := "2.13.6"
  )
  .aggregate(domain, infrastructure)

lazy val `infrastructure` = project

lazy val `application` = project
  .dependsOn(domain)
  .dependsOn(infrastructure % "compile->compile;test->test")

lazy val `serialization` = (project in file(".") / "infrastructure" / "serialization")

lazy val `domain` = project
  .dependsOn(serialization)
