
lazy val iep = project.in(file("."))
  .configure(BuildSettings.profile)
  .aggregate(
    `iep-admin`,
    `iep-dynconfig`,
    `iep-launcher`,
    `iep-leader-api`,
    `iep-servergroups`,
    `iep-service`,
    `iep-ses`,
    `iep-spring`,
    `iep-spring-admin`,
    `iep-spring-atlas`,
    `iep-spring-aws2`,
    `iep-spring-dynconfig`,
    `iep-spring-leader`,
    `iep-spring-leader-dynamodb`,
    `iep-spring-spectatord`,
    `iep-spring-userservice`)
  .settings(BuildSettings.noPackaging: _*)

lazy val `iep-admin` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-dynconfig`, `iep-service`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.jacksonCore,
      Dependencies.jacksonMapper,
      Dependencies.jakartaInject,
      Dependencies.slf4jApi,
      Dependencies.spectatorIpc
  ))

lazy val `iep-dynconfig` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
      Dependencies.slf4jApi,
      Dependencies.typesafeConfig
  ))

lazy val `iep-launcher` = project
  .configure(BuildSettings.profile)

lazy val `iep-leader-api` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
      Dependencies.slf4jApi,
      Dependencies.spectatorApi,
      Dependencies.typesafeConfig,
      Dependencies.assertjcore % "test",
      Dependencies.equalsVerifier % "test"
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
      Dependencies.jakartaAnno,
      Dependencies.slf4jApi
  ))

lazy val `iep-ses` = project
  .configure(BuildSettings.profile)
  .settings(libraryDependencies ++= Seq(
      Dependencies.aws2SES % "test"
  ))

lazy val `iep-spring` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-service`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.slf4jApi,
      Dependencies.springContext,
      Dependencies.jakartaAnno % "test",
      Dependencies.jakartaInject % "test"
  ))

lazy val `iep-spring-admin` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-admin`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.slf4jApi,
      Dependencies.springContext
  ))

lazy val `iep-spring-atlas` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-service`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.slf4jApi,
      Dependencies.spectatorApi,
      Dependencies.spectatorAtlas,
      Dependencies.spectatorGc,
      Dependencies.spectatorJvm,
      Dependencies.spectatorTagging,
      Dependencies.springContext,
      Dependencies.typesafeConfig
  ))

lazy val `iep-spring-aws2` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-dynconfig`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.aws2Core,
      Dependencies.aws2EC2 % "test",
      Dependencies.aws2STS,
      Dependencies.aws2UrlClient % "test",
      Dependencies.slf4jApi,
      Dependencies.springContext,
      Dependencies.typesafeConfig
  ))

lazy val `iep-spring-dynconfig` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-dynconfig`, `iep-spring-admin`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.slf4jApi,
      Dependencies.spectatorApi,
      Dependencies.spectatorIpc,
      Dependencies.springContext
  ))

lazy val `iep-spring-leader` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-leader-api`, `iep-service`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.slf4jApi,
      Dependencies.spectatorApi,
      Dependencies.springContext,
      Dependencies.typesafeConfig,
      Dependencies.assertjcore % "test"
  ))

lazy val `iep-spring-leader-dynamodb` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-leader-api`, `iep-spring-aws2`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.aws2DynamoDB,
      Dependencies.spectatorApi
  ))

lazy val `iep-spring-spectatord` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-service`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.slf4jApi,
      Dependencies.spectatorApi,
      Dependencies.spectatorSidecar,
      Dependencies.spectatorGc,
      Dependencies.spectatorJvm,
      Dependencies.springContext,
      Dependencies.typesafeConfig
  ))

lazy val `iep-spring-userservice` = project
  .configure(BuildSettings.profile)
  .dependsOn(`iep-spring-admin`, `iep-service`)
  .settings(libraryDependencies ++= Seq(
      Dependencies.caffeine,
      Dependencies.jacksonMapper,
      Dependencies.slf4jApi,
      Dependencies.spectatorApi,
      Dependencies.spectatorIpc,
      Dependencies.springContext,
      Dependencies.typesafeConfig
  ))
