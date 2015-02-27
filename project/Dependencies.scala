import sbt._

object Dependencies {
  object Versions {
    val scala      = "2.11.5"
    val slf4j      = "1.7.10"
    val spectator  = "0.20.0"
  }

  import Versions._

  val archaiusCore    = "com.netflix.archaius" % "archaius-core" % "0.6.5"
  val equalsVerifier  = "nl.jqno.equalsverifier" % "equalsverifier" % "1.5.1"
  val eureka          = "com.netflix.eureka" % "eureka-client" % "1.1.147"
  val governator      = "com.netflix.governator" % "governator" % "1.3.3"
  val guice           = "com.google.inject" % "guice" % "3.0"
  val jodaTime        = "joda-time" % "joda-time" % "2.5"
  val karyonAdmin     = "com.netflix.karyon2" % "karyon-admin-web" % "2.2.00-ALPHA7"
  val karyonCore      = "com.netflix.karyon2" % "karyon-core" % "2.2.00-ALPHA7"
  val junit           = "junit" % "junit" % "4.10"
  val junitInterface  = "com.novocode" % "junit-interface" % "0.11"
  val jzlib           = "com.jcraft" % "jzlib" % "1.1.3"
  val rxjava          = "com.netflix.iep-shadow" % "iep-rxjava" % "1.0.6"
  val rxnetty         = "com.netflix.iep-shadow" % "iep-rxnetty" % "0.4.6"
  val rxnettyCtxts    = "com.netflix.iep-shadow" % "iep-rxnetty-contexts" % "0.4.6"
  val rxnettySpectator= "com.netflix.iep-shadow" % "iep-rxnetty-spectator" % "0.4.6"
  val scalaLibrary    = "org.scala-lang" % "scala-library" % scala
  val scalaLibraryAll = "org.scala-lang" % "scala-library-all" % scala
  val scalaLogging    = "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.1.0"
  val scalaParsec     = "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.2"
  val scalaReflect    = "org.scala-lang" % "scala-reflect" % scala
  val scalatest       = "org.scalatest" % "scalatest_2.11" % "2.2.1"
  val slf4jApi        = "org.slf4j" % "slf4j-api" % slf4j
  val spectatorApi    = "com.netflix.spectator" % "spectator-api" % spectator
  val spectatorSandbox= "com.netflix.spectator" % "spectator-ext-sandbox" % spectator
}
