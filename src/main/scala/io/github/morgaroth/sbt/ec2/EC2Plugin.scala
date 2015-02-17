package io.github.morgaroth.sbt.ec2

import com.amazonaws.services.ec2.model.InstanceStateName.Running
import com.amazonaws.services.ec2.model.{DescribeInstancesRequest, Instance}
import io.github.morgaroth.sbt.ec2.utilities.filters._
import io.github.morgaroth.sbt.ec2.utilities.{RichAmazonModelsSupport, progressHelpers}
import sbt.Keys._
import sbt._
import sbt.complete.Parser._
import sbt.complete.Parsers._

import scala.collection.JavaConverters._
import scala.language.reflectiveCalls

/**
 * EC2Plugin is a simple sbt plugin that can looking for running instances on Amazon EC2.
 *
 * == Example ==
 * Here is a complete minimal example:
 *
 * - build.sbt:
 * {{{
 * enablePlugins(EC2Plugin)
 *
 * ec2Region := "eu-west-1"
 *
 * credentials += Credentials(Path.userHome / ".ec2Credentials")
 *
 * }}}
 *
 * - ~/.ec2Credentials:
 * {{{
 * realm=Amazon EC2
 * host=eu-west-1
 * user=<Access Key ID>
 * password=<Secret Access Key>
 * }}}
 */
object EC2Plugin extends AutoPlugin with progressHelpers with RichAmazonModelsSupport with ec2Connection with EC2PluginHelpers with findingInstances
with gettingInstances {

  /**
   * This nested object defines the sbt keys made available by the EC2Plugin: read here for tasks info.
   */
  val autoImport = keys

  import io.github.morgaroth.sbt.ec2.EC2Plugin.autoImport._

  override def projectSettings = Seq(

    ec2ListRunning <<= ec2InitTask[List[Instance]](ec2ListRunning, {
      case (client, progress) =>
        val request = new DescribeInstancesRequest().withFilters(InstanceState(Running))
        client.describeInstances(request).readAllInstances
    }, msgDebug = { result => Some("Received %d instances".format(result.size))}
    ),
    ec2PrintRunning <<= Def.taskDyn {
      (streams in ec2PrintRunning, ec2ListRunning) map { (streams, instances) =>
        streams.log.debug(instances.mkString("\n"))
        streams.log.info(instances.map(printableInstanceInfo).mkString("\n"))
        instances
      }
    },
    ec2FindByName := Def.inputTaskDyn {
      val name = ((token(Space) ~> token(StringBasic, "<name>")) <~ SpaceClass.*).parsed
      findInstancesByName(name, ec2FindByName)
    }.evaluated,
    ec2FindByNames := Def.inputTaskDyn {
      val args = spaceDelimited("<names>").parsed
      findInstancesByNames(args, ec2FindByNames)
    }.evaluated,
    ec2GettingStrategy := GettingInstancesStrategies.NameEqualStrategy,
    ec2GetInstances := getInstancesTask.evaluated,
    ec2PrintCredentials <<= printCredentials,
    ec2Region := "",
    ec2Progress := true,
    ec2UseInstanceProfileCredentials := false
  )

  def printableInstanceInfo(i: Instance): String = {
    "name=%s\tip=%s".format(i.getTags.asScala.find(_.getKey == "Name").map(_.getValue).getOrElse("unnamed"), i.getPublicIpAddress)
  }
}
