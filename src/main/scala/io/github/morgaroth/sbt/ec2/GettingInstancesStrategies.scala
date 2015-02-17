package io.github.morgaroth.sbt.ec2

import com.amazonaws.services.ec2.model.Instance
import io.github.morgaroth.sbt.ec2.utilities.RichAmazonModelsSupport

trait GettingInstancesStrategy {
  def getInstances(queryNames: List[String], foundInstances: List[Instance]): Option[List[Instance]]
}

trait NameEqualStrategy extends GettingInstancesStrategy with RichAmazonModelsSupport {
  override def getInstances(queryNames: List[String], foundInstances: List[Instance]): Option[List[Instance]] =
    Some(foundInstances.filter(x => queryNames.filter(_.equals(x.getNameTag)).nonEmpty))
}

trait InstanceNameContainNameQueryStrategy extends GettingInstancesStrategy with RichAmazonModelsSupport {
  override def getInstances(queryNames: List[String], foundInstances: List[Instance]): Option[List[Instance]] =
    Some(foundInstances.filter(x => queryNames.filter(x.getNameTag.contains(_)).nonEmpty))
}

trait ForceOnlyOneInResult extends GettingInstancesStrategy {
  override abstract def getInstances(queryNames: List[String], foundInstances: List[Instance]): Option[List[Instance]] = {
    super.getInstances(queryNames, foundInstances) match {
      case r@Some(maxOne) if maxOne.size <= 1 => r
      case anyOtherCase => None
    }
  }
}

trait GettingInstancesStrategies {
  val NameEqualStrategy = GettingInstancesStrategies.NameEqualStrategy

  val NameEqualForceOnlyOneStrategy = GettingInstancesStrategies.NameEqualForceOnlyOneStrategy

  val InstanceNameContainNameQueryStrategy = GettingInstancesStrategies.InstanceNameContainNameQueryStrategy

  val InstanceNameContainNameQueryForceOnlyOneStrategy = GettingInstancesStrategies.InstanceNameContainNameQueryForceOnlyOneStrategy
}

object GettingInstancesStrategies {

  object NameEqualStrategy extends NameEqualStrategy

  object NameEqualForceOnlyOneStrategy extends NameEqualStrategy with ForceOnlyOneInResult

  object InstanceNameContainNameQueryStrategy extends InstanceNameContainNameQueryStrategy

  object InstanceNameContainNameQueryForceOnlyOneStrategy extends InstanceNameContainNameQueryStrategy with ForceOnlyOneInResult

}