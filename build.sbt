
lazy val iep = project.in(file("."))
  .configure(BuildSettings.profile)
  .aggregate(
    `iep-admin`,
    `iep-guice`,
    `iep-launcher`,
    `iep-leader-api`,
    `iep-leader-dynamodb`,
    `iep-module-admin`,
    `iep-module-atlas`,
    `iep-module-atlasaggr`,
    `iep-module-aws2`,
    `iep-module-dynconfig`,
    `iep-module-jmxport`,
    `iep-module-leader`,
    `iep-module-userservice`,
    `iep-nflxenv`,
    `iep-servergroups`,
    `iep-service`,
    `iep-ses`)
  .settings(BuildSettings.noPackaging: _*)

lazy val `iep-admin` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-nflxenv`, `iep-service`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.inject,
    Dependencies.jacksonCore,
    Dependencies.jacksonMapper,
    Dependencies.slf4jApi,
    Dependencies.spectatorIpc
  ))

lazy val `iep-guice` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-service`)
  .settings(libraryDependencies ++= Dependencies.guiceCoreAndMulti ++ Seq(
    Dependencies.slf4jApi,
    Dependencies.jsr250 % "test"
  ))

lazy val `iep-launcher` = project
  .configure(BuildSettings.profile)

lazy val `iep-leader-api` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.inject,
    Dependencies.slf4jApi,
    Dependencies.spectatorApi,
    Dependencies.typesafeConfig,
    Dependencies.assertjcore % "test",
    Dependencies.equalsVerifier % "test"
  ))

lazy val `iep-leader-dynamodb` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-leader-api`, `iep-module-aws2`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.aws2DynamoDB,
      Dependencies.spectatorApi
  ))

lazy val `iep-module-admin` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-admin`)
  .settings(libraryDependencies ++= Dependencies.guiceCoreAndMulti ++ Seq(
    Dependencies.slf4jApi
  ))

lazy val `iep-module-atlas` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-nflxenv`, `iep-service`)
  .settings(libraryDependencies ++= Dependencies.guiceCoreAndMulti ++ Seq(
    Dependencies.slf4jApi,
    Dependencies.spectatorApi,
    Dependencies.spectatorAtlas,
    Dependencies.spectatorGc,
    Dependencies.spectatorJvm,
    Dependencies.typesafeConfig
  ))

lazy val `iep-module-atlasaggr` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-nflxenv`, `iep-service`)
  .settings(libraryDependencies ++= Dependencies.guiceCoreAndMulti ++ Seq(
    Dependencies.slf4jApi,
    Dependencies.spectatorApi,
    Dependencies.spectatorStateless,
    Dependencies.spectatorGc,
    Dependencies.spectatorJvm,
    Dependencies.typesafeConfig
  ))

lazy val `iep-module-aws2` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-nflxenv`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.aws2Core,
    Dependencies.aws2EC2 % "test",
    Dependencies.aws2STS,
    Dependencies.aws2UrlClient % "test",
    Dependencies.guiceCore,
    Dependencies.slf4jApi,
    Dependencies.typesafeConfig
  ))

lazy val `iep-module-dynconfig` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-nflxenv`, `iep-module-admin`)
  .settings(libraryDependencies ++= Dependencies.guiceCoreAndMulti ++ Seq(
      Dependencies.slf4jApi,
      Dependencies.spectatorApi,
      Dependencies.spectatorIpc
  ))

lazy val `iep-module-jmxport` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.guiceCore,
    Dependencies.slf4jApi
  ))

lazy val `iep-module-leader` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-leader-api`, `iep-service`)
  .settings(libraryDependencies ++= Dependencies.guiceCoreAndMulti ++ Seq(
    Dependencies.slf4jApi,
    Dependencies.spectatorApi,
    Dependencies.typesafeConfig,
    Dependencies.assertjcore % "test"
  ))

lazy val `iep-module-userservice` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-module-admin`, `iep-service`)
  .settings(libraryDependencies ++= Dependencies.guiceCoreAndMulti ++ Seq(
    Dependencies.caffeine,
    Dependencies.jacksonMapper,
    Dependencies.slf4jApi,
    Dependencies.spectatorApi,
    Dependencies.spectatorIpc,
    Dependencies.typesafeConfig
  ))

lazy val `iep-nflxenv` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
    Dependencies.slf4jApi,
    Dependencies.typesafeConfig
  ))

lazy val `iep-servergroups` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-service`)
  .settings(libraryDependencies ++= Seq(
    Dependencies.jacksonCore,
    Dependencies.spectatorApi,
    Dependencies.spectatorIpc,
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
    Dependencies.awsSES % "test",
    Dependencies.aws2SES % "test"
  ))
