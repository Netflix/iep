import sbt._
import sbt.Keys._

object BuildSettings {

  val javaCompilerFlags: Seq[String] = Seq(
    "-Xlint:unchecked",
    "--release", "17")

  val javadocFlags: Seq[String] = Seq("-Xdoclint:none")

  val compilerFlags: Seq[String] = Seq(
    "-deprecation",
    "-unchecked",
    "-Xexperimental",
    "-Xlint:_,-infer-any",
    "-feature",
    "-release", "17")

  lazy val checkLicenseHeaders = taskKey[Unit]("Check the license headers for all source files.")
  lazy val formatLicenseHeaders = taskKey[Unit]("Fix the license headers for all source files.")

  lazy val baseSettings: Seq[Def.Setting[?]] = GitVersion.settings

  lazy val buildSettings: Seq[Def.Setting[?]] = baseSettings ++ Seq(
    organization := "com.netflix.iep",
    scalaVersion := Dependencies.Versions.scala,
    scalacOptions ++= BuildSettings.compilerFlags,
    javacOptions ++= BuildSettings.javaCompilerFlags,
    doc / javacOptions := BuildSettings.javadocFlags,
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),
    crossPaths := false,
    sourcesInBase := false,
    Test / fork := true,

    // sbt 2 defaults this to true, which runs the test classes for a project concurrently
    // within the forked JVM. Some tests compare snapshots of JVM state, like the set of
    // live threads or system properties, and cannot tolerate other tests running at the
    // same time. Restore the sbt 1 behavior of running them serially.
    Test / testForkedParallel := false,

    autoScalaLibrary := false,
    externalResolvers := Def.uncached(BuildSettings.resolvers),

    // Evictions: https://github.com/sbt/sbt/issues/1636
    // Linting: https://github.com/sbt/sbt/pull/5153
    (update / evictionWarningOptions).withRank(KeyRanks.Invisible) := EvictionWarningOptions.empty,

    checkLicenseHeaders := LicenseCheck.checkLicenseHeaders(streams.value.log, sourceDirectory.value),
    formatLicenseHeaders := LicenseCheck.formatLicenseHeaders(streams.value.log, sourceDirectory.value),

    packageBin / packageOptions += Package.ManifestAttributes(
      "Build-Date"   -> java.time.Instant.now().toString,
      "Build-Number" -> sys.env.getOrElse("GITHUB_RUN_ID", "unknown"),
      "Commit"       -> sys.env.getOrElse("GITHUB_SHA",    "unknown"))
  )

  lazy val commonDeps: Seq[ModuleID] = Seq(
    Dependencies.junitInterface % "test"
  )

  val resolvers: Seq[Resolver] = Seq(
    Resolver.mavenLocal,
    Resolver.mavenCentral
  )

  // Don't create or publish an artifact for the aggregate root project
  lazy val noPackaging: Seq[Def.Setting[?]] = Seq(
    publish / skip := true,
    packagedArtifacts := Def.uncached(Map.empty)
  )

  def profile: Project => Project = p => {
    p.settings(SonatypeSettings.settings)
      .settings(buildSettings*)
      .settings(libraryDependencies ++= commonDeps)
  }
}
