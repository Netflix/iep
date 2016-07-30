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

  lazy val baseSettings =
    sbtrelease.ReleasePlugin.releaseSettings ++
      GitVersion.settings

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
    checkLicenseHeaders := License.checkLicenseHeaders(streams.value.log, sourceDirectory.value),
    formatLicenseHeaders := License.formatLicenseHeaders(streams.value.log, sourceDirectory.value)
  )

  lazy val commonDeps = Seq(
    Dependencies.junitInterface % "test"
  )

  val resolvers = Seq(
    Resolver.mavenLocal,
    Resolver.jcenterRepo,
    "jfrog" at "http://oss.jfrog.org/oss-snapshot-local")

  // Don't create root.jar, from:
  // http://stackoverflow.com/questions/20747296/producing-no-artifact-for-root-project-with-package-under-multi-project-build-in
  lazy val noPackaging = Seq(
    Keys.`package` :=  file(""),
    packageBin in Global :=  file(""),
    packagedArtifacts :=  Map()
  )

  def profile: Project => Project = p => {
    bintrayProfile(p)
      .settings(buildSettings: _*)
      .settings(libraryDependencies ++= commonDeps)
  }

  // Disable bintray plugin when not running under CI. Avoids a bunch of warnings like:
  //
  // ```
  // Missing bintray credentials /Users/brharrington/.bintray/.credentials. Some bintray features depend on this.
  // [warn] Credentials file /Users/brharrington/.bintray/.credentials does not exist
  // ```
  def bintrayProfile(p: Project): Project = {
    val credsPath = Path.userHome / ".bintray" / ".credentials"
    if (credsPath.exists)
      p.settings(Bintray.settings)
    else
      p.disablePlugins(bintray.BintrayPlugin)
  }
}
