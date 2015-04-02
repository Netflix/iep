import sbt._

object Dependencies {
  object Versions {
    val archaius   = "2.0.0-SNAPSHOT"
    val guice      = "4.0-beta5"
    val ribbon     = "2.0.0"
    val rxnetty    = "0.4.8"
    val scala      = "2.11.6"
    val slf4j      = "1.7.10"
    val spectator  = "0.20.0"
  }

  import Versions._

  val archaiusCore    = "com.netflix.archaius" % "archaius-core" % archaius
  val archaiusGuice   = "com.netflix.archaius" % "archaius-guice" % archaius
  val archaiusLegacy  = "com.netflix.archaius" % "archaius-legacy" % archaius
  val archaiusPersist = "com.netflix.archaius" % "archaius-persisted2" % archaius
  val archaiusTypesafe= "com.netflix.archaius" % "archaius-typesafe" % archaius
  val equalsVerifier  = "nl.jqno.equalsverifier" % "equalsverifier" % "1.5.1"
  val eureka          = "com.netflix.eureka" % "eureka-client" % "1.1.150"
  val governator      = "com.netflix.governator" % "governator" % "1.3.3"
  val guiceAssist     = "com.google.inject.extensions" % "guice-assistedinject" % guice
  val guiceCore       = "com.google.inject" % "guice" % guice
  val guiceGrapher    = "com.google.inject.extensions" % "guice-grapher" % guice
  val guiceMulti      = "com.google.inject.extensions" % "guice-multibindings" % guice
  val guiceServlet    = "com.google.inject.extensions" % "guice-servlet" % guice
  val inject          = "javax.inject" % "javax.inject" % "1"
  val jodaTime        = "joda-time" % "joda-time" % "2.5"
  val karyonAdmin     = "com.netflix.karyon2" % "karyon-admin-web" % "2.2.00-ALPHA7"
  val karyonCore      = "com.netflix.karyon2" % "karyon-core" % "2.2.00-ALPHA7"
  val junit           = "junit" % "junit" % "4.10"
  val junitInterface  = "com.novocode" % "junit-interface" % "0.11"
  val jzlib           = "com.jcraft" % "jzlib" % "1.1.3"
  val ribbonCore      = "com.netflix.ribbon" % "ribbon-core" % ribbon
  val ribbonEureka    = "com.netflix.ribbon" % "ribbon-eureka" % ribbon
  val ribbonHttp      = "com.netflix.ribbon" % "ribbon-httpclient" % ribbon
  val ribbonLb        = "com.netflix.ribbon" % "ribbon-loadbalancer" % ribbon
  val rxjava          = "io.reactivex" % "rxjava" % "1.0.8"
  val rxnettyCore     = "io.reactivex" % "rxnetty" % "0.4.8"
  val rxnettyCtxts    = "io.reactivex" % "rxnetty-contexts" % "0.4.8"
  val rxnettySpectator= "io.reactivex" % "rxnetty-spectator" % "0.4.8"
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
