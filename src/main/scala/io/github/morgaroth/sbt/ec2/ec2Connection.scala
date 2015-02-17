package io.github.morgaroth.sbt.ec2

import com.amazonaws.auth.{BasicAWSCredentials, InstanceProfileCredentialsProvider}
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.{ClientConfiguration, Protocol}
import io.github.morgaroth.sbt.ec2.EC2Plugin.autoImport._
import sbt.Keys._
import sbt.{Credentials, Def, Task}

import scala.util.Try

trait ec2Connection {
  def getClient(creds: Seq[Credentials], regionName: String, useInstanceProfileCredentials: Boolean): AmazonEC2Client = {
    val credentialsProvider = getCredentials(creds, regionName, useInstanceProfileCredentials)._1
    Try(Regions.fromName(regionName)).map { region =>
      val cli = new AmazonEC2Client(credentialsProvider, new ClientConfiguration().withProtocol(Protocol.HTTPS))
      cli.setRegion(region)
      cli.setEndpoint("ec2.%s.amazonaws.com".format(region.getName))
      cli
    }.getOrElse {
      sys.error("Could not recover EC2 region from name \"%s\".".format(regionName))
    }
  }

  def getCredentials(creds: Seq[Credentials], region: String, useInstanceProfileCredentials: Boolean) = {
    Credentials.forHost(creds, region) match {
      case Some(cred) => new StaticCredentialsProvider(new BasicAWSCredentials(cred.userName, cred.passwd)) -> Left(cred)
      case None if useInstanceProfileCredentials =>
        val instance = new InstanceProfileCredentialsProvider
        instance -> Right(instance) // for get both amazon credentials and direct sbt credentials too
      case _ => sys.error("Could not find EC2 credentials for region (passed in credentials as host): %s.".format(region))
    }
  }

  def printCredentials: Def.Initialize[Task[Unit]] = {
    (credentials, streams, ec2Region, ec2UseInstanceProfileCredentials) map { (creds, streams, host, useInstanceProfileCredentials) =>
      val credentials = getCredentials(creds, host, useInstanceProfileCredentials)._2.left
      credentials.map { cred =>
        streams.log.info("\taccess id:\t%s".format(cred.userName.replaceFirst( """.{15}""", "*****")))
        streams.log.info("\taccess key:\t%s".format(cred.passwd.replaceFirst( """.{35}""", "*****")))
      }
    }
  }

}
