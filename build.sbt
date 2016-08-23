name := "hello-kafka-salesforce"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.cometd.java" % "cometd-java-client" % "3.0.9",
  "com.force.api" % "force-partner-api" % "36.0.0",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.11-RC1",
  "com.github.jkutner" % "env-keystore" % "0.1.2",
  "org.slf4j" % "slf4j-simple" % "1.7.21"
)

cancelable in Global := true

enablePlugins(JavaAppPackaging)
