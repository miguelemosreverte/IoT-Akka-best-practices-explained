version := "1.0.0"
name := "e2e"
scalaVersion := "2.13.6"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.8" % Test
libraryDependencies += ("infrastructure" %% "actor" % "1.0.0" % "compile->compile;test->test")
libraryDependencies += ("infrastructure" %% "kafka" % "1.0.0" % "compile->compile;test->test")
libraryDependencies += ("infrastructure" %% "http_kafka" % "1.0.0" % "compile->compile;test->test")
libraryDependencies += ("infrastructure" %% "http_kafka" % "1.0.0" % "compile->compile;test->test")
libraryDependencies += ("domain" %% "domain" % "1.0.0")
libraryDependencies += ("application" %% "writeside" % "1.0.0" % "compile->compile;test->test")
libraryDependencies += ("application" %% "readside" % "1.0.0" % "compile->compile;test->test")
