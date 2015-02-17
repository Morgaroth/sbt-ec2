import com.typesafe.sbt.pgp.PgpKeys._
import sbtrelease.ReleaseStep
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities._

name := "sbt-ec2"

description := "EC2 Plugin for sbt"

organization := "io.github.morgaroth"

sbtPlugin := true

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-ec2" % "1.9.17"
)

sbtVersion in Global := "0.13.5"

scalaVersion in Global := "2.10.4"

releaseSettings

sonatypeSettings

import SonatypeKeys.sonatypeReleaseAll

def singleTaskReleaseStep(task: TaskKey[_]) = ReleaseStep(action = (st: State) => {
  val extracted = st.extract
  val ref = extracted.get(thisProjectRef)
  extracted.runAggregated(task in ThisBuild in ref, st)
})
val publishArtifactsLocally = singleTaskReleaseStep(publishLocal)
val publishArtifactsSigned = singleTaskReleaseStep(publishSigned)
val finishReleseAtSonatype = singleTaskReleaseStep(sonatypeReleaseAll)

ReleaseKeys.releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // : ReleaseStep
  inquireVersions, // : ReleaseStep
  runTest, // : ReleaseStep
  setReleaseVersion, // : ReleaseStep
  publishArtifactsSigned,
  finishReleseAtSonatype,
  commitReleaseVersion, // : ReleaseStep, performs the initial git checks
  tagRelease, // : ReleaseStep
  setNextVersion, // : ReleaseStep
  commitNextVersion, // : ReleaseStep
  pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
)

scriptedSettings

scriptedLaunchOpts := {
  scriptedLaunchOpts.value ++
    Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false

publishMavenStyle := true

pomExtra := {
  <url>https://github.com/Morgaroth/sbt-ec2</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:git@github.com:Morgaroth/sbt-ec2.git</connection>
      <developerConnection>scm:git:git@github.com:Morgaroth/sbt-ec2.git</developerConnection>
      <url>https://github.com/Morgaroth/sbt-ec2</url>
    </scm>
    <developers>
      <developer>
        <id>Morgaroth</id>
        <name>Mateusz Jaje</name>
        <url>http://morgaroth.github.io/</url>
      </developer>
    </developers>
}

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false
