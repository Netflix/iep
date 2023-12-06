import sbt._

object Dependencies {
  object Versions {
    val assertj    = "3.24.2"
    val aws2       = "2.21.38"
    val jackson    = "2.16.0"
    val scala      = "2.12.15"
    val slf4j      = "1.7.36"
    val spectator  = "1.7.2"
    val spring     = "6.0.13"
  }

  import Versions._

  val assertjcore        = "org.assertj" % "assertj-core" % assertj
  val aws2Core           = "software.amazon.awssdk" % "core" % aws2
  val aws2DynamoDB       = "software.amazon.awssdk" % "dynamodb" % aws2
  val aws2EC2            = "software.amazon.awssdk" % "ec2" % aws2
  val aws2SES            = "software.amazon.awssdk" % "ses" % aws2
  val aws2STS            = "software.amazon.awssdk" % "sts" % aws2
  val aws2UrlClient      = "software.amazon.awssdk" % "url-connection-client" % aws2
  val caffeine           = "com.github.ben-manes.caffeine" % "caffeine" % "3.1.8"
  val equalsVerifier     = "nl.jqno.equalsverifier" % "equalsverifier" % "3.15.2"
  val jacksonCore        = "com.fasterxml.jackson.core" % "jackson-core" % jackson
  val jacksonMapper      = "com.fasterxml.jackson.core" % "jackson-databind" % jackson
  val jakartaAnno        = "jakarta.annotation" % "jakarta.annotation-api" % "2.1.1"
  val jakartaInject      = "jakarta.inject" % "jakarta.inject-api" % "2.0.1"
  val jodaTime           = "joda-time" % "joda-time" % "2.10.10"
  val jedis              = "redis.clients" % "jedis" % "5.0.2"
  val junit              = "junit" % "junit" % "4.12"
  val junitInterface     = "com.novocode" % "junit-interface" % "0.11"
  val jzlib              = "com.jcraft" % "jzlib" % "1.1.3"
  val mockitoCore        = "org.mockito" % "mockito-core" % "5.7.0"
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
  val typesafeConfig     = "com.typesafe" % "config" % "1.4.3"
}
