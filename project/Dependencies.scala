import sbt._

object Dependencies {
  object Versions {
    val archaius   = "2.0.0-rc.21"
    val guice      = "4.0"
    val karyon     = "2.7.1"
    val netty      = "4.1.0.Beta6-SNAPSHOT"
    val rxnetty    = "0.5.0-SNAPSHOT"
    val scala      = "2.11.6"
    val slf4j      = "1.7.12"
    val spectator  = "0.26.0"
  }

  import Versions._

  val archaiusBridge  = "com.netflix.archaius" % "archaius2-archaius1-bridge" % archaius
  val archaiusCore    = "com.netflix.archaius" % "archaius2-core" % archaius
  val archaiusGuice   = "com.netflix.archaius" % "archaius2-guice" % archaius
  val archaiusLegacy  = "com.netflix.archaius" % "archaius-core" % "0.6.6"
  val archaiusPersist = "com.netflix.archaius" % "archaius2-persisted2" % archaius
  val archaiusTypesafe= "com.netflix.archaius" % "archaius2-typesafe" % archaius
  val equalsVerifier  = "nl.jqno.equalsverifier" % "equalsverifier" % "1.7.2"
  val eureka          = "com.netflix.eureka" % "eureka-client" % "1.1.155"
  val governator      = "com.netflix.governator" % "governator" % "1.7.2"
  val guiceAssist     = "com.google.inject.extensions" % "guice-assistedinject" % guice
  val guiceCore       = "com.google.inject" % "guice" % guice
  val guiceGrapher    = "com.google.inject.extensions" % "guice-grapher" % guice
  val guiceMulti      = "com.google.inject.extensions" % "guice-multibindings" % guice
  val guiceServlet    = "com.google.inject.extensions" % "guice-servlet" % guice
  val inject          = "javax.inject" % "javax.inject" % "1"
  val jodaTime        = "joda-time" % "joda-time" % "2.7"
  val karyonAdmin     = "com.netflix.karyon" % "karyon2-admin-web" % karyon
  val karyonCore      = "com.netflix.karyon" % "karyon2-core" % karyon
  val log4jSlf4jImpl  = "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.1"
  val log4jCore       = "org.apache.logging.log4j" % "log4j-core" % "2.1"
  val log4jJul        = "org.apache.logging.log4j" % "log4j-jul" % "2.1"
  val log4jJcl        = "org.apache.logging.log4j" % "log4j-jcl" % "2.1"
  val junit           = "junit" % "junit" % "4.12"
  val junitInterface  = "com.novocode" % "junit-interface" % "0.11"
  val jzlib           = "com.jcraft" % "jzlib" % "1.1.3"
  val nettyCodec      = "io.netty" % "netty-codec-http" % netty
  val nettyHandler    = "io.netty" % "netty-handler" % netty
  val nettyTransport  = "io.netty" % "netty-transport-native-epoll" % netty
  val rxjava          = "io.reactivex" % "rxjava" % "1.0.9"
  val rxnettyCore     = "io.reactivex" % "rxnetty" % rxnetty
  //val rxnettyCtxts    = "io.reactivex" % "rxnetty-contexts" % rxnetty
  val rxnettySpectator= "io.reactivex" % "rxnetty-spectator" % rxnetty
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
