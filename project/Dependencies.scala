import sbt._

object Dependencies {
  object Versions {
    val assertj    = "3.27.7"
    val aws2       = "2.42.32"
    val jackson    = "3.1.1"
    val scala      = "2.12.20"
    val slf4j      = "2.0.17"
    val spectator  = "1.9.6"
    val spring     = "7.0.6"
    val springBoot = "4.0.5"
  }

  import Versions._

  val assertjcore        = "org.assertj" % "assertj-core" % assertj
  val aws2Core           = "software.amazon.awssdk" % "core" % aws2
  val aws2DynamoDB       = "software.amazon.awssdk" % "dynamodb" % aws2
  val aws2EC2            = "software.amazon.awssdk" % "ec2" % aws2
  val aws2SES            = "software.amazon.awssdk" % "ses" % aws2
  val aws2STS            = "software.amazon.awssdk" % "sts" % aws2
  val aws2UrlClient      = "software.amazon.awssdk" % "url-connection-client" % aws2
  val caffeine           = "com.github.ben-manes.caffeine" % "caffeine" % "3.2.3"
  val equalsVerifier     = "nl.jqno.equalsverifier" % "equalsverifier" % "4.3.1"
  val jacksonCore        = "tools.jackson.core" % "jackson-core" % jackson
  val jacksonMapper      = "tools.jackson.core" % "jackson-databind" % jackson
  val jakartaAnno        = "jakarta.annotation" % "jakarta.annotation-api" % "3.0.0"
  val jakartaInject      = "jakarta.inject" % "jakarta.inject-api" % "2.0.1"
  val jedis              = "redis.clients" % "jedis" % "7.2.1"
  val junitInterface     = "com.novocode" % "junit-interface" % "0.11"
  val mockitoCore        = "org.mockito" % "mockito-core" % "5.21.0"
  val slf4jApi           = "org.slf4j" % "slf4j-api" % slf4j
  val spectatorApi       = "com.netflix.spectator" % "spectator-api" % spectator
  val spectatorAtlas     = "com.netflix.spectator" % "spectator-reg-atlas" % spectator
  val spectatorGc        = "com.netflix.spectator" % "spectator-ext-gc" % spectator
  val spectatorIpc       = "com.netflix.spectator" % "spectator-ext-ipc" % spectator
  val spectatorJvm       = "com.netflix.spectator" % "spectator-ext-jvm" % spectator
  val spectatorSidecar   = "com.netflix.spectator" % "spectator-reg-sidecar" % spectator
  val spectatorTagging   = "com.netflix.spectator" % "spectator-nflx-tagging" % spectator
  val springBootHealth   = "org.springframework.boot" % "spring-boot-health" % springBoot
  val springContext      = "org.springframework" % "spring-context" % spring
  val typesafeConfig     = "com.typesafe" % "config" % "1.4.6"
}
