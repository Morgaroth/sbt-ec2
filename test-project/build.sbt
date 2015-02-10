import sbt.Keys._

name := "test-ec2-project"

version := "0.1.0"

credentials += Credentials(Path.userHome / ".ec2credentials")

ec2Region := "eu-west-1"

enablePlugins(EC2Plugin)
