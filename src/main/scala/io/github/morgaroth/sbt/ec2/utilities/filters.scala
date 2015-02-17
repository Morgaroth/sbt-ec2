package io.github.morgaroth.sbt.ec2.utilities

import com.amazonaws.services.ec2.model.{Filter, InstanceStateName}

import scala.collection.JavaConverters._

object filters {
  def InstanceState(state: InstanceStateName): Filter = InstanceStates(List(state))

  def InstanceStates(states: InstanceStateName*): Filter = InstanceStates(states.toList)

  def InstanceStates(states: List[InstanceStateName]): Filter = new Filter("instance-state-name", states.map(_.toString).asJava)

  def InstanceName(name: String): Filter = InstanceNames(List(name))

  def InstanceNames(names: String*): Filter = InstanceNames(names.toList)

  def InstanceNames(names: List[String]): Filter = InstanceTags("Name", names)

  def InstanceTags(name: String, values: List[String]) = new Filter("tag:%s".format(name), values.asJava)

}
