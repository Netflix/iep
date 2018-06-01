
lazy val root = project.in(file("."))
  .configure(BuildSettings.profile)
  .aggregate(
    `iep-admin`,
    `iep-config`,
    `iep-eureka-testconfig`,
    `iep-guice`,
    `iep-launcher`,
    `iep-module-admin`,
    `iep-module-archaius1`,
    `iep-module-archaius2`,
    `iep-module-atlas`,
    `iep-module-aws`,
    `iep-module-aws2`,
    `iep-module-awsmetrics`,
    `iep-module-eureka`,
    `iep-module-jmxport`,
    `iep-module-rxnetty`,
    `iep-module-userservice`,
    `iep-nflxenv`,
    `iep-platformservice`,
    `iep-rxhttp`,
    `iep-service`,
    `iep-ses`)
  .settings(BuildSettings.noPackaging: _*)

lazy val `iep-admin` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-service`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.inject,
    Dependencies.jacksonCore,
    Dependencies.jacksonMapper,
    Dependencies.slf4jApi,
    Dependencies.spectatorSandbox
  ))

lazy val `iep-config` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-platformservice`, `iep-nflxenv`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.archaiusBridge,
    Dependencies.archaiusCore,
    Dependencies.guiceCore,
    Dependencies.jodaTime,
    Dependencies.equalsVerifier % "test"
  ))

lazy val `iep-eureka-testconfig` = project
  .configure(BuildSettings.profile)

lazy val `iep-guice` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-service`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.guiceCore,
    Dependencies.guiceMulti,
    Dependencies.slf4jApi,
    Dependencies.jsr250 % "test"
  ))

lazy val `iep-launcher` = project
  .configure(BuildSettings.profile)

lazy val `iep-module-admin` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-admin`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.guiceCore,
    Dependencies.guiceMulti,
    Dependencies.slf4jApi
  ))

lazy val `iep-module-archaius1` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.archaiusBridge,
    Dependencies.archaiusCore,
    Dependencies.archaiusGuice,
    Dependencies.archaiusLegacy,
    Dependencies.guiceCore,
    Dependencies.guiceMulti,
    Dependencies.slf4jApi
  ))

lazy val `iep-module-archaius2` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-platformservice`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.archaiusCore,
    Dependencies.archaiusGuice,
    Dependencies.archaiusPersist,
    Dependencies.archaiusTypesafe,
    Dependencies.guiceCore,
    Dependencies.slf4jApi
  ))

lazy val `iep-module-atlas` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-nflxenv`, `iep-service`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.guiceCore,
    Dependencies.guiceMulti,
    Dependencies.slf4jApi,
    Dependencies.spectatorApi,
    Dependencies.spectatorAtlas,
    Dependencies.spectatorGc,
    Dependencies.spectatorJvm,
    Dependencies.typesafeConfig
  ))

lazy val `iep-module-aws` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-nflxenv`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.awsCore,
    Dependencies.awsAutoScaling % "test",
    Dependencies.awsCloudWatch % "test",
    Dependencies.awsDynamoDB % "test",
    Dependencies.awsEC2 % "test",
    Dependencies.awsELB % "test",
    Dependencies.awsELBv2 % "test",
    Dependencies.awsEMR % "test",
    Dependencies.awsRoute53 % "test",
    Dependencies.awsSTS,
    Dependencies.guiceCore,
    Dependencies.reactiveStreams,
    Dependencies.rxjava2,
    Dependencies.slf4jApi,
    Dependencies.typesafeConfig
  ))

lazy val `iep-module-aws2` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-nflxenv`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.aws2Core,
    Dependencies.aws2AutoScaling % "test",
    Dependencies.aws2CloudWatch % "test",
    Dependencies.aws2DynamoDB % "test",
    Dependencies.aws2EC2 % "test",
    Dependencies.aws2ELB % "test",
    Dependencies.aws2ELBv2 % "test",
    Dependencies.aws2EMR % "test",
    Dependencies.aws2Route53 % "test",
    Dependencies.aws2STS,
    Dependencies.guiceCore,
    Dependencies.jacksonCbor,    // Jackson deps are not used directly, here to force 2.9.1
    Dependencies.jacksonMapper,  // https://bugs.openjdk.java.net/browse/JDK-8186334
    Dependencies.jacksonJr,
    Dependencies.reactiveStreams,
    Dependencies.rxjava2,
    Dependencies.slf4jApi,
    Dependencies.typesafeConfig
  ))

lazy val `iep-module-awsmetrics` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.spectatorApi,
    Dependencies.spectatorAws,
    Dependencies.guiceCore,
    Dependencies.slf4jApi
  ))

lazy val `iep-module-eureka` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-service`, `iep-module-admin`, `iep-eureka-testconfig` % "test")
  .settings(libraryDependencies ++= Seq(
    Dependencies.eurekaClient,
    Dependencies.guiceCore,
    Dependencies.guiceMulti,
    Dependencies.slf4jApi
  ))

lazy val `iep-module-jmxport` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.guiceCore,
    Dependencies.slf4jApi
  ))

lazy val `iep-module-rxnetty` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-rxhttp`, `iep-module-eureka` % "test", `iep-eureka-testconfig` % "test")
  .settings(libraryDependencies ++= Seq(
    Dependencies.guiceCore,
    Dependencies.rxnettyCore,
    Dependencies.slf4jApi
  ))

lazy val `iep-module-userservice` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-module-admin`, `iep-service`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.guiceCore,
    Dependencies.guiceMulti,
    Dependencies.jacksonMapper,
    Dependencies.slf4jApi,
    Dependencies.spectatorApi,
    Dependencies.spectatorSandbox,
    Dependencies.typesafeConfig
  ))

lazy val `iep-nflxenv` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.slf4jApi,
    Dependencies.typesafeConfig
  ))

lazy val `iep-platformservice` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-nflxenv`, `iep-module-admin`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.archaiusCore,
    Dependencies.archaiusGuice,
    Dependencies.archaiusPersist,
    Dependencies.archaiusTypesafe,
    Dependencies.guiceCore,
    Dependencies.guiceMulti,
    Dependencies.slf4jApi,
    Dependencies.spectatorApi,
    Dependencies.spectatorSandbox
  ))

lazy val `iep-rxhttp` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.archaiusCore,
    Dependencies.eurekaClient,
    Dependencies.jsr250,
    Dependencies.jzlib,
    Dependencies.rxjava,
    Dependencies.rxnettyCore,
    Dependencies.spectatorApi,
    Dependencies.spectatorSandbox,
    Dependencies.slf4jApi,
    Dependencies.equalsVerifier % "test"
  ))

lazy val `iep-service` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.inject,
    Dependencies.jsr250,
    Dependencies.slf4jApi
  ))

lazy val `iep-ses` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.awsCore,
    Dependencies.awsSES
  ))
