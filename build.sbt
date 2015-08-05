name := """lancat.scala"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
  "com.typesafe.akka" %% "akka-remote" % "2.3.11",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "javax.jmdns" % "jmdns" % "3.4.1")
