import sbt._
import sbt.Keys._

object BuildSettings {

  val javaCompilerFlags = Seq(
    "-Xlint:unchecked",
    "-source", "1.8",
    "-target", "1.8")

  val javadocFlags = Seq("-Xdoclint:none")

  val compilerFlags = Seq(
    "-deprecation",
    "-unchecked",
    "-Xexperimental",
    "-Xlint:_,-infer-any",
    "-feature",
    "-target:jvm-1.8")

  lazy val checkLicenseHeaders = taskKey[Unit]("Check the license headers for all source files.")
  lazy val formatLicenseHeaders = taskKey[Unit]("Fix the license headers for all source files.")

  lazy val baseSettings = GitVersion.settings

  lazy val buildSettings = baseSettings ++ Seq(
    organization := "com.netflix.iep",
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

    // Evictions: https://github.com/sbt/sbt/issues/1636
    // Linting: https://github.com/sbt/sbt/pull/5153
    (evictionWarningOptions in update).withRank(KeyRanks.Invisible) := EvictionWarningOptions.empty,

    checkLicenseHeaders := License.checkLicenseHeaders(streams.value.log, sourceDirectory.value),
    formatLicenseHeaders := License.formatLicenseHeaders(streams.value.log, sourceDirectory.value),

    packageOptions in (Compile, packageBin) += Package.ManifestAttributes(
      "Build-Date"   -> java.time.Instant.now().toString,
      "Build-Number" -> sys.env.getOrElse("GITHUB_RUN_ID", "unknown"),
      "Commit"       -> sys.env.getOrElse("GITHUB_SHA",    "unknown"))
  )

  lazy val commonDeps = Seq(
    Dependencies.junitInterface % "test"
  )

  val resolvers = Seq(
    Resolver.mavenLocal,
    Resolver.jcenterRepo,
    "jfrog" at "https://oss.jfrog.org/oss-snapshot-local")

  // Don't create root.jar, from:
  // http://stackoverflow.com/questions/20747296/producing-no-artifact-for-root-project-with-package-under-multi-project-build-in
  lazy val noPackaging = Seq(
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
