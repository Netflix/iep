import sbt._
import sbt.Keys._

object MainBuild extends Build {

  lazy val baseSettings =
    sbtrelease.ReleasePlugin.releaseSettings ++
    GitVersion.settings ++
    Bintray.settings ++
    net.virtualvoid.sbt.graph.Plugin.graphSettings ++
    scoverage.ScoverageSbtPlugin.projectSettings

  lazy val buildSettings = baseSettings ++ Seq(
            organization := BuildSettings.organization,
            scalaVersion := Dependencies.Versions.scala,
           scalacOptions ++= BuildSettings.compilerFlags,
            javacOptions ++= BuildSettings.javaCompilerFlags,
     javacOptions in doc := BuildSettings.javadocFlags,
             testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),
              crossPaths := false,
           sourcesInBase := false,
            fork in Test := true,
        autoScalaLibrary := false,
       externalResolvers := BuildSettings.resolvers,
     checkLicenseHeaders := License.checkLicenseHeaders(streams.value.log, sourceDirectory.value),
    formatLicenseHeaders := License.formatLicenseHeaders(streams.value.log, sourceDirectory.value)
  )

  lazy val root = project.in(file("."))
    .aggregate(
      `iep-admin`,
      `iep-config`,
      `iep-eureka-testconfig`,
      `iep-guice`,
      `iep-launcher`,
      `iep-module-admin`,
      `iep-module-archaius1`,
      `iep-module-archaius2`,
      `iep-module-awsmetrics`,
      `iep-module-eureka`,
      `iep-module-jmxport`,
      `iep-module-rxnetty`,
      `iep-nflxenv`,
      `iep-platformservice`,
      `iep-rxhttp`,
      `iep-service`)
    .settings(buildSettings: _*)
    .settings(BuildSettings.noPackaging: _*)

  lazy val `iep-admin` = project
    .dependsOn(`iep-service`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.inject,
      Dependencies.jacksonCore,
      Dependencies.jacksonMapper,
      Dependencies.slf4jApi,
      Dependencies.spectatorSandbox
    ))

  lazy val `iep-config` = project
    .dependsOn(`iep-platformservice`, `iep-nflxenv`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.archaiusBridge,
      Dependencies.archaiusCore,
      Dependencies.guiceCore,
      Dependencies.jodaTime,
      Dependencies.equalsVerifier % "test"
    ))

  lazy val `iep-eureka-testconfig` = project
    .settings(buildSettings: _*)

  lazy val `iep-guice` = project
    .dependsOn(`iep-service`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.guiceCore,
      Dependencies.slf4jApi
    ))

  lazy val `iep-launcher` = project
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)

  lazy val `iep-module-admin` = project
    .dependsOn(`iep-admin`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.guiceCore,
      Dependencies.guiceMulti,
      Dependencies.slf4jApi
    ))

  lazy val `iep-module-archaius1` = project
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
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
    .dependsOn(`iep-platformservice`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.archaiusCore,
      Dependencies.archaiusGuice,
      Dependencies.archaiusPersist,
      Dependencies.archaiusTypesafe,
      Dependencies.guiceCore,
      Dependencies.slf4jApi
    ))

  lazy val `iep-module-awsmetrics` = project
    .dependsOn(`iep-platformservice`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.spectatorApi,
      Dependencies.spectatorAws,
      Dependencies.guiceCore,
      Dependencies.slf4jApi
    ))

  lazy val `iep-module-eureka` = project
    .dependsOn(`iep-service`, `iep-module-admin`, `iep-eureka-testconfig` % "test")
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.eurekaClient,
      Dependencies.guiceCore,
      Dependencies.guiceMulti,
      Dependencies.slf4jApi
    ))

  lazy val `iep-module-jmxport` = project
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.guiceCore,
      Dependencies.slf4jApi
    ))

  lazy val `iep-module-rxnetty` = project
    .dependsOn(`iep-rxhttp`, `iep-module-eureka` % "test", `iep-eureka-testconfig` % "test")
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.guiceCore,
      Dependencies.rxnettyCore,
      Dependencies.rxnettyCtxts,
      Dependencies.rxnettySpectator,
      Dependencies.slf4jApi
    ))

  lazy val `iep-nflxenv` = project
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.typesafeConfig
    ))

  lazy val `iep-platformservice` = project
    .dependsOn(`iep-nflxenv`, `iep-module-admin`)
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
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
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.archaiusLegacy,
      Dependencies.eurekaClient,
      Dependencies.jzlib,
      Dependencies.rxjava,
      Dependencies.rxnettyCore,
      Dependencies.rxnettyCtxts,
      Dependencies.spectatorApi,
      Dependencies.spectatorSandbox,
      Dependencies.slf4jApi,
      Dependencies.equalsVerifier % "test"
    ))

  lazy val `iep-service` = project
    .settings(buildSettings: _*)
    .settings(libraryDependencies ++= commonDeps)
    .settings(libraryDependencies ++= Seq(
      Dependencies.inject,
      Dependencies.slf4jApi
    ))

  lazy val commonDeps = Seq(
    Dependencies.junitInterface % "test"
  )

  lazy val checkLicenseHeaders = taskKey[Unit]("Check the license headers for all source files.")
  lazy val formatLicenseHeaders = taskKey[Unit]("Fix the license headers for all source files.")
}
