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

  lazy val baseSettings: Seq[Def.Setting[_]] = GitVersion.settings

  lazy val buildSettings: Seq[Def.Setting[_]] = baseSettings ++ Seq(
    organization := "com.netflix.iep",
    scalaVersion := Dependencies.Versions.scala,
    scalacOptions ++= BuildSettings.compilerFlags,
    javacOptions ++= BuildSettings.javaCompilerFlags,
    doc / javacOptions := BuildSettings.javadocFlags,
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),
    crossPaths := false,
    sourcesInBase := false,
    Test / fork := true,
    autoScalaLibrary := false,
    externalResolvers := BuildSettings.resolvers,

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

  // Don't create root.jar, from:
  // http://stackoverflow.com/questions/20747296/producing-no-artifact-for-root-project-with-package-under-multi-project-build-in
  lazy val noPackaging: Seq[Def.Setting[_]] = Seq(
    Keys.`package` :=  file(""),
    packageBin in Global :=  file(""),
    packagedArtifacts :=  Map()
  )

  def profile: Project => Project = p => {
    p.settings(SonatypeSettings.settings)
      .settings(buildSettings: _*)
      .settings(libraryDependencies ++= commonDeps)
  }
}
