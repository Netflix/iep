import sbt._

object Dependencies {
  object Versions {
    val archaius   = "2.0.0-rc.28"
    val guice      = "4.0"
    val karyon     = "2.7.1"
    val karyon3    = "3.0.1-rc.6"
    val netty      = "4.1.0.Beta7"
    val rxnetty    = "0.5.0-SNAPSHOT"
    val scala      = "2.11.7"
    val slf4j      = "1.7.12"
    val spectator  = "0.29.0"
  }

  import Versions._

  val archaiusBridge  = "com.netflix.archaius" % "archaius2-archaius1-bridge" % archaius
  val archaiusCore    = "com.netflix.archaius" % "archaius2-core" % archaius
  val archaiusGuice   = "com.netflix.archaius" % "archaius2-guice" % archaius
  val archaiusLegacy  = "com.netflix.archaius" % "archaius-core" % "0.7.2"
  val archaiusPersist = "com.netflix.archaius" % "archaius2-persisted2" % archaius
  val archaiusTypesafe= "com.netflix.archaius" % "archaius2-typesafe" % archaius
  val equalsVerifier  = "nl.jqno.equalsverifier" % "equalsverifier" % "1.7.2"
  val eureka          = "com.netflix.eureka" % "eureka-client" % "1.2.0"
  val governator      = "com.netflix.governator" % "governator-core" % "1.9.3"
  val guiceAssist     = "com.google.inject.extensions" % "guice-assistedinject" % guice
  val guiceCore       = "com.google.inject" % "guice" % guice
  val guiceGrapher    = "com.google.inject.extensions" % "guice-grapher" % guice
  val guiceMulti      = "com.google.inject.extensions" % "guice-multibindings" % guice
  val guiceServlet    = "com.google.inject.extensions" % "guice-servlet" % guice
  val inject          = "javax.inject" % "javax.inject" % "1"
  val jodaTime        = "joda-time" % "joda-time" % "2.7"
  val karyonAdmin     = "com.netflix.karyon" % "karyon2-admin-web" % karyon
  val karyonCore      = "com.netflix.karyon" % "karyon2-core" % karyon
  val karyon3Admin    = "com.netflix.karyon" % "karyon3-admin-simple" % karyon3
  val karyon3Archaius = "com.netflix.karyon" % "karyon3-archaius2" % karyon3
  val junit           = "junit" % "junit" % "4.12"
  val junitInterface  = "com.novocode" % "junit-interface" % "0.11"
  val jzlib           = "com.jcraft" % "jzlib" % "1.1.3"
  val nettyCodec      = "io.netty" % "netty-codec-http" % netty
  val nettyHandler    = "io.netty" % "netty-handler" % netty
  val nettyTransport  = "io.netty" % "netty-transport-native-epoll" % netty
  val rxjava          = "io.reactivex" % "rxjava" % "1.0.14"
  val rxnettyCommon   = "io.reactivex" % "rxnetty-common" % rxnetty
  val rxnettyHttp     = "io.reactivex" % "rxnetty-http" % rxnetty
  val rxnettySpectator= "io.reactivex" % "rxnetty-spectator-http" % rxnetty
  val scalaLibrary    = "org.scala-lang" % "scala-library" % scala
  val scalaLibraryAll = "org.scala-lang" % "scala-library-all" % scala
  val scalaLogging    = "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.1.0"
  val scalaParsec     = "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.2"
  val scalaReflect    = "org.scala-lang" % "scala-reflect" % scala
  val scalatest       = "org.scalatest" % "scalatest_2.11" % "2.2.1"
  val slf4jApi        = "org.slf4j" % "slf4j-api" % slf4j
  val slf4jSimple     = "org.slf4j" % "slf4j-simple" % slf4j
  val spectatorApi    = "com.netflix.spectator" % "spectator-api" % spectator
  val spectatorSandbox= "com.netflix.spectator" % "spectator-ext-sandbox" % spectator
}
