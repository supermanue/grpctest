name := "akka-grpc-quickstart-scala"

version := "1.0"

scalaVersion := "2.13.4"

lazy val akkaVersion = "2.8.0"
lazy val akkaHttpVersion = "10.5.1"
lazy val akkaGrpcVersion = "2.3.1"
lazy val scalaUUIDVersion = "0.3.1"
lazy val scalaCacheVersion = "1.0.0-M6"
lazy val scalaCacheGuavaVersion = "0.28.0"

enablePlugins(AkkaGrpcPlugin)

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
  "com.typesafe.akka" %% "akka-pki" % akkaVersion,

  // The Akka HTTP overwrites are required because Akka-gRPC depends on 10.1.x
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,

  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.1" % Test,

  //UUID generator
  "io.jvm.uuid" %% "scala-uuid" % scalaUUIDVersion,

  //caching
  "com.github.cb372" %% "scalacache-core" % scalaCacheVersion,
  "com.github.cb372" %% "scalacache-guava" % scalaCacheGuavaVersion

)
