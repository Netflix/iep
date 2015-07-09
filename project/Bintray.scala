import sbt._
import sbt.Keys._
import bintray.BintrayCredentials.api
import bintray.BintrayKeys._

object Bintray {

  lazy val storeBintrayCredentials = taskKey[Unit]("store bintray credentials")

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    bintrayRepository := "maven",
    bintrayPackage := "iep",
    bintrayOrganization := Some("netflixoss"),
    bintrayReleaseOnPublish := false,
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt")),

    storeBintrayCredentials := IO.write(bintrayCredentialsFile.value, api.template(sys env "bintrayUser", sys env "bintrayKey")),

    pomExtra := (
      <url>https://github.com/netflix/iep/wiki</url>
      <licenses>
        <license>
          <name>The Apache Software License, Version 2.0</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
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
