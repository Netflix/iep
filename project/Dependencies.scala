import sbt._

object Dependencies {
  object Versions {
    val archaius   = "2.1.9"
    val aws        = "1.11.37"
    val eureka     = "1.5.5"
    val guice      = "4.1.0"
    val jackson    = "2.8.2"
    val rxnetty    = "0.4.19"
    val scala      = "2.11.8"
    val slf4j      = "1.7.21"
    val spectator  = "0.40.0"
  }

  import Versions._

  val archaiusBridge   = "com.netflix.archaius" % "archaius2-archaius1-bridge" % archaius
  val archaiusCore     = "com.netflix.archaius" % "archaius2-core" % archaius
  val archaiusGuice    = "com.netflix.archaius" % "archaius2-guice" % archaius
  val archaiusLegacy   = "com.netflix.archaius" % "archaius-core" % "0.7.4"
  val archaiusPersist  = "com.netflix.archaius" % "archaius2-persisted2" % archaius
  val archaiusTypesafe = "com.netflix.archaius" % "archaius2-typesafe" % archaius
  val awsCore          = "com.amazonaws" % "aws-java-sdk-core" % aws
  val awsEC2           = "com.amazonaws" % "aws-java-sdk-ec2" % aws
  val awsSES           = "com.amazonaws" % "aws-java-sdk-ses" % aws
  val awsSTS           = "com.amazonaws" % "aws-java-sdk-sts" % aws
  val equalsVerifier   = "nl.jqno.equalsverifier" % "equalsverifier" % "2.1.5"
  val eurekaClient     = "com.netflix.eureka" % "eureka-client" % eureka
  val guiceAssist      = "com.google.inject.extensions" % "guice-assistedinject" % guice
  val guiceCore        = "com.google.inject" % "guice" % guice
  val guiceGrapher     = "com.google.inject.extensions" % "guice-grapher" % guice
  val guiceMulti       = "com.google.inject.extensions" % "guice-multibindings" % guice
  val guiceServlet     = "com.google.inject.extensions" % "guice-servlet" % guice
  val inject           = "javax.inject" % "javax.inject" % "1"
  val jacksonCore      = "com.fasterxml.jackson.core" % "jackson-core" % jackson
  val jacksonMapper    = "com.fasterxml.jackson.core" % "jackson-databind" % jackson
  val jodaTime         = "joda-time" % "joda-time" % "2.9.4"
  val junit            = "junit" % "junit" % "4.12"
  val junitInterface   = "com.novocode" % "junit-interface" % "0.11"
  val jzlib            = "com.jcraft" % "jzlib" % "1.1.3"
  val rxjava           = "io.reactivex" % "rxjava" % "1.2.0"
  val rxnettyCore      = "io.reactivex" % "rxnetty" % rxnetty
  val rxnettyCtxts     = "io.reactivex" % "rxnetty-contexts" % rxnetty
  val rxnettySpectator = "io.reactivex" % "rxnetty-spectator" % rxnetty
  val scalaLibrary     = "org.scala-lang" % "scala-library" % scala
  val scalaLibraryAll  = "org.scala-lang" % "scala-library-all" % scala
  val scalaReflect     = "org.scala-lang" % "scala-reflect" % scala
  val scalatest        = "org.scalatest" % "scalatest_2.11" % "2.2.6"
  val slf4jApi         = "org.slf4j" % "slf4j-api" % slf4j
  val spectatorApi     = "com.netflix.spectator" % "spectator-api" % spectator
  val spectatorAws     = "com.netflix.spectator" % "spectator-ext-aws" % spectator
  val spectatorSandbox = "com.netflix.spectator" % "spectator-ext-sandbox" % spectator
  val typesafeConfig   = "com.typesafe" % "config" % "1.3.1"
}
