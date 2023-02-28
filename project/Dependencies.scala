import sbt._

object Dependencies {
  object Versions {
    val assertj    = "3.24.2"
    val aws2       = "2.20.13"
    val graal      = "21.1.0"
    val jackson    = "2.14.2"
    val scala      = "2.12.15"
    val slf4j      = "1.7.36"
    val spectator  = "1.5.4"
    val spring     = "5.3.25"
  }

  import Versions._

  val assertjcore        = "org.assertj" % "assertj-core" % assertj
  val aws2Core           = "software.amazon.awssdk" % "core" % aws2
  val aws2DynamoDB       = "software.amazon.awssdk" % "dynamodb" % aws2
  val aws2EC2            = "software.amazon.awssdk" % "ec2" % aws2
  val aws2SES            = "software.amazon.awssdk" % "ses" % aws2
  val aws2STS            = "software.amazon.awssdk" % "sts" % aws2
  val aws2UrlClient      = "software.amazon.awssdk" % "url-connection-client" % aws2
  val caffeine           = "com.github.ben-manes.caffeine" % "caffeine" % "2.9.3"
  val equalsVerifier     = "nl.jqno.equalsverifier" % "equalsverifier" % "3.14"
  val graalJs            = "org.graalvm.js" % "js" % graal
  val graalJsEngine      = "org.graalvm.js" % "js-scriptengine" % graal
  val inject             = "javax.inject" % "javax.inject" % "1"
  val jacksonCore        = "com.fasterxml.jackson.core" % "jackson-core" % jackson
  val jacksonMapper      = "com.fasterxml.jackson.core" % "jackson-databind" % jackson
  val jedis              = "redis.clients" % "jedis" % "4.3.1"
  val jodaTime           = "joda-time" % "joda-time" % "2.10.10"
  val jsr250             = "javax.annotation" % "jsr250-api" % "1.0"
  val junit              = "junit" % "junit" % "4.12"
  val junitInterface     = "com.novocode" % "junit-interface" % "0.11"
  val jzlib              = "com.jcraft" % "jzlib" % "1.1.3"
  val mockitoCore        = "org.mockito" % "mockito-core" % "4.11.0"
  val slf4jApi           = "org.slf4j" % "slf4j-api" % slf4j
  val spectatorApi       = "com.netflix.spectator" % "spectator-api" % spectator
  val spectatorAtlas     = "com.netflix.spectator" % "spectator-reg-atlas" % spectator
  val spectatorAws       = "com.netflix.spectator" % "spectator-ext-aws" % spectator
  val spectatorGc        = "com.netflix.spectator" % "spectator-ext-gc" % spectator
  val spectatorIpc       = "com.netflix.spectator" % "spectator-ext-ipc" % spectator
  val spectatorJvm       = "com.netflix.spectator" % "spectator-ext-jvm" % spectator
  val spectatorSidecar   = "com.netflix.spectator" % "spectator-reg-sidecar" % spectator
  val spectatorTagging   = "com.netflix.spectator" % "spectator-nflx-tagging" % spectator
  val springContext      = "org.springframework" % "spring-context" % spring
  val typesafeConfig     = "com.typesafe" % "config" % "1.4.2"
}
