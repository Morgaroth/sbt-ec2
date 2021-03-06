import sbt.Keys._

name := "test-ec2-project"

version := "0.1.0"

credentials += Credentials(baseDirectory.value / ".ec2credentials")

scalaVersion := "2.10.5"

ec2Region := "eu-west-1"

enablePlugins(EC2Plugin)
