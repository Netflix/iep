import sbt._
import sbt.librarymanagement.DependencyBuilders.OrganizationArtifactName

object Dependencies {
  object Versions {
    val archaius   = "2.3.16"
    val assertj    = "3.20.2"
    val aws        = "1.12.41"
    val aws2       = "2.17.13"
    val graal      = "21.1.0"
    val guice      = "5.0.1"
    val jackson    = "2.12.4"
    val rxnetty    = "0.4.20"
    val rxscala    = "0.26.5"
    val scala      = "2.12.12"
    val slf4j      = "1.7.32"
    val spectator  = "0.133.0"
  }

  import Versions._

  val archaiusBridge     = "com.netflix.archaius" % "archaius2-archaius1-bridge" % archaius
  val archaiusCore       = "com.netflix.archaius" % "archaius2-core" % archaius
  val archaiusGuice      = "com.netflix.archaius" % "archaius2-guice" % archaius
  val archaiusLegacy     = "com.netflix.archaius" % "archaius-core" % "0.7.7"
  val archaiusPersist    = "com.netflix.archaius" % "archaius2-persisted2" % archaius
  val archaiusTypesafe   = "com.netflix.archaius" % "archaius2-typesafe" % archaius
  val assertjcore        = "org.assertj" % "assertj-core" % assertj
  val awsAutoScaling     = "com.amazonaws" % "aws-java-sdk-autoscaling" % aws
  val awsCache           = "com.amazonaws" % "aws-java-sdk-elasticache" % aws
  val awsCloudWatch      = "com.amazonaws" % "aws-java-sdk-cloudwatch" % aws
  val awsCore            = "com.amazonaws" % "aws-java-sdk-core" % aws
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
  val aws2Core           = "software.amazon.awssdk" % "core" % aws2
  val aws2DynamoDB       = "software.amazon.awssdk" % "dynamodb" % aws2
  val aws2EC2            = "software.amazon.awssdk" % "ec2" % aws2
  val aws2SES            = "software.amazon.awssdk" % "ses" % aws2
  val aws2STS            = "software.amazon.awssdk" % "sts" % aws2
  val aws2UrlClient      = "software.amazon.awssdk" % "url-connection-client" % aws2
  val caffeine           = "com.github.ben-manes.caffeine" % "caffeine" % "2.9.1"
  val equalsVerifier     = "nl.jqno.equalsverifier" % "equalsverifier" % "3.7"
  val graalJs            = "org.graalvm.js" % "js" % graal
  val graalJsEngine      = "org.graalvm.js" % "js-scriptengine" % graal
  val guiceCoreBase      = "com.google.inject" % "guice"
  val guiceMultiBase     = "com.google.inject.extensions" % "guice-multibindings"
  val inject             = "javax.inject" % "javax.inject" % "1"
  val jacksonCore        = "com.fasterxml.jackson.core" % "jackson-core" % jackson
  val jacksonMapper      = "com.fasterxml.jackson.core" % "jackson-databind" % jackson
  val jodaTime           = "joda-time" % "joda-time" % "2.10.10"
  val jsr250             = "javax.annotation" % "jsr250-api" % "1.0"
  val junit              = "junit" % "junit" % "4.12"
  val junitInterface     = "com.novocode" % "junit-interface" % "0.11"
  val jzlib              = "com.jcraft" % "jzlib" % "1.1.3"
  val reactiveStreams    = "org.reactivestreams" % "reactive-streams" % "1.0.3"
  val rxjava             = "io.reactivex" % "rxjava" % "1.3.8"
  val rxjava2            = "io.reactivex.rxjava2" % "rxjava" % "2.2.21"
  val rxnettyCore        = "io.reactivex" % "rxnetty" % rxnetty
  val slf4jApi           = "org.slf4j" % "slf4j-api" % slf4j
  val spectatorApi       = "com.netflix.spectator" % "spectator-api" % spectator
  val spectatorAtlas     = "com.netflix.spectator" % "spectator-reg-atlas" % spectator
  val spectatorAws       = "com.netflix.spectator" % "spectator-ext-aws" % spectator
  val spectatorGc        = "com.netflix.spectator" % "spectator-ext-gc" % spectator
  val spectatorIpc       = "com.netflix.spectator" % "spectator-ext-ipc" % spectator
  val spectatorJvm       = "com.netflix.spectator" % "spectator-ext-jvm" % spectator
  val spectatorStateless = "com.netflix.spectator" % "spectator-reg-stateless" % spectator
  val typesafeConfig     = "com.typesafe" % "config" % "1.4.1"

  def isBeforeJava16: Boolean = {
    System.getProperty("java.specification.version").toDouble < 16
  }

  private def guiceDep(base: OrganizationArtifactName): ModuleID = {
    base % (if (isBeforeJava16) "4.1.0" else guice)
  }

  def guiceCore: ModuleID = guiceDep(guiceCoreBase)

  def guiceCoreAndMulti: Seq[ModuleID] = {
    if (isBeforeJava16)
      Seq(guiceDep(guiceCoreBase), guiceDep(guiceMultiBase))
    else
      Seq(guiceDep(guiceCoreBase))
  }
}
