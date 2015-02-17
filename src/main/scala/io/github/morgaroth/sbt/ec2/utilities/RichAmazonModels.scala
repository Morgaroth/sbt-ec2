package io.github.morgaroth.sbt.ec2.utilities

import com.amazonaws.services.ec2.model.{DescribeInstancesResult, Instance}

import scala.collection.JavaConverters._
import scala.language.implicitConversions

class RichDescribeInstancesResult(wrapped: DescribeInstancesResult) extends RichAmazonModelsSupport {

  /**
   * reads instances only from this response
   */
  def readInstances = wrapped.getReservations.asScala.map(_.getInstances.asScala).flatten.toList

  /**
   * Reads instances from sequence of responses using nextToken field
   */
  def readAllInstances = {
    def read(response: DescribeInstancesResult, instances: List[Instance]): List[Instance] =
      (response.getNextToken, instances ::: response.readInstances) match {
        case (null, ready) => ready
        case (emptyString, ready) if emptyString.isEmpty => ready
        case (next, ready) => read(response.withNextToken(next), ready)
      }
    read(wrapped, Nil)
  }
}

class RichInstance(wrapped: Instance) {
  def getNameTag: String = wrapped.getTags.asScala.find(_.getKey == "Name").map(_.getValue).getOrElse("")
}

trait RichAmazonModelsSupport {
  implicit def wrapToRichDescribeInstancesResult(obj: DescribeInstancesResult): RichDescribeInstancesResult = new RichDescribeInstancesResult(obj)

  implicit def wrapToRichInstance(obj: Instance): RichInstance = new RichInstance(obj)
}

object RichAmazonModelsSupport extends RichAmazonModelsSupport