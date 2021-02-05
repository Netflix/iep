import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.SonatypeKeys._

object SonatypeSettings {

  lazy val now = System.currentTimeMillis

  private def get(k: String): String = {
    sys.env.getOrElse(s"sonatype$k", s"missing$k")
  }

  lazy val user = get("User")
  lazy val pass = get("Password")

  lazy val settings: Seq[Def.Setting[_]] = sonatypeSettings ++ Seq(
    sonatypeProfileName := "com.netflix",
    sonatypeProjectHosting := Some(GitHubHosting("brharrington", "iep", "brharrington@netflix.com")),

    publishMavenStyle := true,
    licenses += ("APL2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
    credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", user, pass),

    publishTo := sonatypePublishToBundle.value
  )
}
