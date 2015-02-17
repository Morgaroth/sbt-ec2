package io.github.morgaroth.sbt.ec2

import com.amazonaws.services.ec2.model.Instance
import io.github.morgaroth.sbt.ec2.keys._
import sbt.Keys._
import sbt.complete.Parsers._
import sbt.{Def, InputKey, Task, _}

trait gettingInstances {
  def getInstances(sources: List[String], task: InputKey[_]): Def.Initialize[Task[Option[List[Instance]]]] = {
    (streams in task, ec2ListRunning, ec2GettingStrategy in task) map { (streams, instances, filter) =>
      val result = filter.getInstances(sources, instances)
      streams.log.info("Searching for instances ends with %s".format(result.map(x => "%d results".format(x.size)).getOrElse("error")))
      result
    }
  }

  def getInstancesTask = Def.inputTaskDyn {
    val args = spaceDelimited("<names>").parsed.toList
    getInstances(args, ec2GetInstances)
  }

}
