name := "banking"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  // akka
  "com.typesafe.akka" %% "akka-actor" % "2.5.11",
  "com.typesafe.akka" %% "akka-stream" % "2.5.11",
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  // joda
  "org.joda" % "joda-money" % "0.12",
  "joda-time" % "joda-time" % "2.9.9",
  // log
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.11",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  // stm
  "org.scala-stm" %% "scala-stm" % "0.8",
  // config
  "com.typesafe" % "config" % "1.2.1",
  // test
  "org.scalatest" %% "scalatest" % "3.0.5",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1",
)