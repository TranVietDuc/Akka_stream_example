name := "test_akka_family"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.31"

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.0"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % AkkaVersion

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion





