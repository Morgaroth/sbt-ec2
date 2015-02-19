import io.github.morgaroth.sbt.ec2.EC2Plugin
import io.github.morgaroth.sbt.ec2.EC2Plugin.autoImport._
import sbt.Keys._
import sbt.{Build => SbtBuild, _}


object Build extends SbtBuild {


  override def settings: Seq[Def.Setting[_]] = super.settings ++ Seq(

  )

  lazy val root = (project in file(".")).
    enablePlugins(EC2Plugin).
    settings(
      name := "test-ec2-project",
      version := "0.1.0",
      credentials += Credentials(baseDirectory.value / ".ec2credentials"),
      scalaVersion := "2.10.5",
      ec2Region := "eu-west-1"
    )
}