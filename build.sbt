name := "hello-kafka-salesforce"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  "org.cometd.java" % "cometd-java-client" % "3.1.2",
  "com.force.api" % "force-partner-api" % "41.0.0",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.17",
  "com.heroku.sdk" % "env-keystore" % "1.0.0",
  "org.slf4j" % "slf4j-simple" % "1.7.25"
)

cancelable in Global := true

enablePlugins(JavaAppPackaging)
