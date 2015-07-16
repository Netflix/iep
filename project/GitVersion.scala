import sbt._
import sbt.Keys._
import sbtrelease.Version
import com.typesafe.sbt.SbtGit._

object GitVersion {
  lazy val now = new java.util.Date()
  lazy val dateFormatter = {
    val f = new java.text.SimpleDateFormat("yyyyMMdd.HHmmss")
    f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
    f
  }

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    version in ThisBuild := {
      val versionBranch = """([0-9\.]+?)(x)?""".r
      val snapshotVersion = """v?([0-9\.]+)-(\d+)-([0-9a-z]+)""".r
      val releaseVersion = """v?([0-9\.]+)""".r
      val isPullRequest = sys.env.getOrElse("TRAVIS_PULL_REQUEST", "false") != "false"
      val branch = GitKeys.gitReader.value.withGit(_.branches).headOption.getOrElse("unknown")
println(s"BRANCH: ${branch}")
      git.gitDescribedVersion.value getOrElse "0.1-SNAPSHOT" match {
        case v if (isPullRequest) => s"0.0.0-PULLREQUEST"
        case snapshotVersion(v, n, h) => {
          val v2 = Version(v).map(_.bump.string).getOrElse(v)
          val suffix = "-SNAPSHOT" //s"-${dateFormatter.format(now)}" //s".${"%02d".format(n.toInt)}-SNAPSHOT"
          branch match {
            case versionBranch(b, x) if (!v2.startsWith(b)) => {
              val newVersion = s"${b}${Option(x).map(_ => "0").getOrElse("")}"
println(s"BRANCH_VERSION: ${newVersion}")
              s"${Version(newVersion).map(_.string).getOrElse(v2)}${suffix}"
            }
            case _ => s"${v2}${suffix}"
          }
        }
        case releaseVersion(v) => v
        case v => v
      }
    }
  )
}
