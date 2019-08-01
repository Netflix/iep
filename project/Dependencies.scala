import sbt._

object Dependencies {
  object Versions {
    val archaius   = "2.3.14"
    val assertj    = "3.13.1"
    val aws        = "1.11.579"
    val aws2       = "2.6.4"
    val eureka     = "1.9.12"
    val guice      = "4.1.0"
    val jackson    = "2.9.9"
    val rxnetty    = "0.4.20"
    val rxscala    = "0.26.5"
    val scala      = "2.12.8"
    val slf4j      = "1.7.26"
    val spectator  = "0.92.0"
  }

  import Versions._

  val archaiusBridge     = "com.netflix.archaius" % "archaius2-archaius1-bridge" % archaius
  val archaiusCore       = "com.netflix.archaius" % "archaius2-core" % archaius
  val archaiusGuice      = "com.netflix.archaius" % "archaius2-guice" % archaius
  val archaiusLegacy     = "com.netflix.archaius" % "archaius-core" % "0.7.6"
  val archaiusPersist    = "com.netflix.archaius" % "archaius2-persisted2" % archaius
  val archaiusTypesafe   = "com.netflix.archaius" % "archaius2-typesafe" % archaius
  val assertjcore        = "org.assertj" % "assertj-core" % assertj
  val awsAutoScaling     = "com.amazonaws" % "aws-java-sdk-autoscaling" % aws
  val awsCache           = "com.amazonaws" % "aws-java-sdk-elasticache" % aws
  val awsCore            = "com.amazonaws" % "aws-java-sdk-core" % aws
  val awsCloudWatch      = "com.amazonaws" % "aws-java-sdk-cloudwatch" % aws
  val awsDynamoDB        = "com.amazonaws" % "aws-java-sdk-dynamodb" % aws
  val awsEC2             = "com.amazonaws" % "aws-java-sdk-ec2" % aws
  val awsELB             = "com.amazonaws" % "aws-java-sdk-elasticloadbalancing" % aws
  val awsELBv2           = "com.amazonaws" % "aws-java-sdk-elasticloadbalancingv2" % aws
  val awsEMR             = "com.amazonaws" % "aws-java-sdk-emr" % aws
  val awsLambda          = "com.amazonaws" % "aws-java-sdk-lambda" % aws
  val awsRDS             = "com.amazonaws" % "aws-java-sdk-rds" % aws
  val awsRoute53         = "com.amazonaws" % "aws-java-sdk-route53" % aws
  val awsSES             = "com.amazonaws" % "aws-java-sdk-ses" % aws
  val awsSTS             = "com.amazonaws" % "aws-java-sdk-sts" % aws
  val aws2AutoScaling    = "software.amazon.awssdk" % "autoscaling" % aws2
  val aws2Core           = "software.amazon.awssdk" % "core" % aws2
  val aws2CloudWatch     = "software.amazon.awssdk" % "cloudwatch" % aws2
  val aws2DynamoDB       = "software.amazon.awssdk" % "dynamodb" % aws2
  val aws2EC2            = "software.amazon.awssdk" % "ec2" % aws2
  val aws2ELB            = "software.amazon.awssdk" % "elasticloadbalancing" % aws2
  val aws2ELBv2          = "software.amazon.awssdk" % "elasticloadbalancingv2" % aws2
  val aws2EMR            = "software.amazon.awssdk" % "emr" % aws2
  val aws2Route53        = "software.amazon.awssdk" % "route53" % aws2
  val aws2SES            = "software.amazon.awssdk" % "ses" % aws2
  val aws2STS            = "software.amazon.awssdk" % "sts" % aws2
  val equalsVerifier     = "nl.jqno.equalsverifier" % "equalsverifier" % "3.1.9"
  val eurekaClient       = "com.netflix.eureka" % "eureka-client" % eureka
  val guiceAssist        = "com.google.inject.extensions" % "guice-assistedinject" % guice
  val guiceCore          = "com.google.inject" % "guice" % guice
  val guiceGrapher       = "com.google.inject.extensions" % "guice-grapher" % guice
  val guiceMulti         = "com.google.inject.extensions" % "guice-multibindings" % guice
  val guiceServlet       = "com.google.inject.extensions" % "guice-servlet" % guice
  val inject             = "javax.inject" % "javax.inject" % "1"
  val jacksonCore        = "com.fasterxml.jackson.core" % "jackson-core" % jackson
  val jacksonCbor        = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jackson
  val jacksonJoda        = "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jackson
  val jacksonJr          = "com.fasterxml.jackson.jr" % "jackson-jr-objects" % jackson
  val jacksonMapper      = "com.fasterxml.jackson.core" % "jackson-databind" % jackson
  val jacksonScala       = "com.fasterxml.jackson.module" %% "jackson-module-scala" % jackson
  val jacksonSmile       = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-smile" % jackson
  val jodaConvert        = "org.joda" % "joda-convert" % "1.8.1"
  val jodaTime           = "joda-time" % "joda-time" % "2.10.2"
  val jsr250             = "javax.annotation" % "jsr250-api" % "1.0"
  val junit              = "junit" % "junit" % "4.12"
  val junitInterface     = "com.novocode" % "junit-interface" % "0.11"
  val jzlib              = "com.jcraft" % "jzlib" % "1.1.3"
  val reactiveStreams    = "org.reactivestreams" % "reactive-streams" % "1.0.2"
  val rxjava             = "io.reactivex" % "rxjava" % "1.3.8"
  val rxjava2            = "io.reactivex.rxjava2" % "rxjava" % "2.2.10"
  val rxScala            = "io.reactivex" %% "rxscala" % rxscala
  val rxnettyCore        = "io.reactivex" % "rxnetty" % rxnetty
  val rxnettySpectator   = "io.reactivex" % "rxnetty-spectator" % rxnetty
  val scalaLibrary       = "org.scala-lang" % "scala-library" % scala
  val scalaLibraryAll    = "org.scala-lang" % "scala-library-all" % scala
  val scalaReflect       = "org.scala-lang" % "scala-reflect" % scala
  val scalaj             = "org.scalaj" %% "scalaj-http" % "2.3.0"
  val scalaLogging       = "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  val scalatest          = "org.scalatest" % "scalatest_2.11" % "2.2.6"
  val slf4jApi           = "org.slf4j" % "slf4j-api" % slf4j
  val spectatorApi       = "com.netflix.spectator" % "spectator-api" % spectator
  val spectatorAtlas     = "com.netflix.spectator" % "spectator-reg-atlas" % spectator
  val spectatorAws       = "com.netflix.spectator" % "spectator-ext-aws" % spectator
  val spectatorGc        = "com.netflix.spectator" % "spectator-ext-gc" % spectator
  val spectatorIpc       = "com.netflix.spectator" % "spectator-ext-ipc" % spectator
  val spectatorJvm       = "com.netflix.spectator" % "spectator-ext-jvm" % spectator
  val spectatorStateless = "com.netflix.spectator" % "spectator-reg-stateless" % spectator
  val typesafeConfig     = "com.typesafe" % "config" % "1.3.4"
}
