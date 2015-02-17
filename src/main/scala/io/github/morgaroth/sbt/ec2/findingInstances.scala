package io.github.morgaroth.sbt.ec2

import com.amazonaws.services.ec2.model.{DescribeInstancesRequest, Instance}
import io.github.morgaroth.sbt.ec2.EC2Plugin.autoImport._
import io.github.morgaroth.sbt.ec2.utilities.RichAmazonModelsSupport
import io.github.morgaroth.sbt.ec2.utilities.filters._
import sbt.Keys._
import sbt.Scoped.richTaskSeq
import sbt.{Def, InputKey, Task, _}

trait findingInstances extends RichAmazonModelsSupport {
  this: EC2PluginHelpers =>
  private def findInstancesByNameUsingRequest(source: String, task: InputKey[List[Instance]]): Def.Initialize[Task[List[Instance]]] = {
    ec2InitInputTask[List[Instance]](task, {
      case (client, progress) =>
        val request = new DescribeInstancesRequest().withFilters(InstanceName(source))
        client.describeInstances(request).readAllInstances
    }, msgInfo = {
      case list => Some("Searching for instances ends with %d results".format(list.size))
    })
  }

  private def findInstancesByNameUsingLocal(source: String, task: InputKey[List[Instance]]): Def.Initialize[Task[List[Instance]]] = {
    (streams in task, ec2ListRunning) map { (streams, instances) =>
      val result = instances.filter(_.getNameTag.toLowerCase.contains(source.toLowerCase)).toList
      streams.log.info("Searching for instances ends with %d results".format(result.size))
      result
    }
  }

  def findInstancesByName = findInstancesByNameUsingLocal _

  def findInstancesByNames(sources: Seq[String], task: InputKey[List[Instance]]): Def.Initialize[Task[List[Instance]]] = Def.taskDyn {
    sources.map(findInstancesByName(_, task)).join.map(_.flatten.toList)
  }
}
