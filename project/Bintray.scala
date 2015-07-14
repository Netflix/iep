import sbt._
import sbt.Keys._
import bintray.BintrayCredentials.api
import bintray.BintrayKeys._

object Bintray {

  lazy val now = System.currentTimeMillis
  lazy val isPullRequest = sys.env.getOrElse("TRAVIS_PULL_REQUEST", "false") != "false"
  lazy val (user, pass) = {
    if (isPullRequest) ("dummyUser", "dummyPass")
    else (sys.env.getOrElse("bintrayUser", "missingUser"), sys.env.getOrElse("bintrayKey", "missingKey"))
  }
  lazy val storeBintrayCredentials = taskKey[Unit]("store bintray credentials")

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    bintrayRepository := "maven",
    bintrayPackage := "iep",
    bintrayOrganization := Some("netflixoss"),
    bintrayReleaseOnPublish := false,
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    credentials += Credentials("Artifactory Realm", "oss.jfrog.org", user, pass),

    publishTo := {
      if (isSnapshot.value)
        Some("OJO" at s"https://oss.jfrog.org/oss-snapshot-local;timestamp=${now}/")
      else
        publishTo.value
        //Some("bintray" at s"https://api.bintray.com/maven/${bintrayOrganization.value.get}/${bintrayRepository.value}/maven/")
    },

    storeBintrayCredentials := {
      IO.write(bintrayCredentialsFile.value, api.template(user, pass))
    },

    pomExtra := (
      <url>https://github.com/netflix/iep/wiki</url>
      <scm>
        <url>git@github.com:netflix/iep.git</url>
        <connection>scm:git:git@github.com:netflix/iep.git</connection>
      </scm>
      <developers>
        <developer>
          <id>brharrington</id>
          <name>Brian Harrington</name>
          <email>brharrington@netflix.com</email>
        </developer>
      </developers>
    )
  )
}
