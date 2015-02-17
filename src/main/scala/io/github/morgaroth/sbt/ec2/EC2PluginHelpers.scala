package io.github.morgaroth.sbt.ec2

import com.amazonaws.services.ec2.AmazonEC2Client
import io.github.morgaroth.sbt.ec2.keys._
import sbt.Keys._
import sbt._

trait EC2PluginHelpers extends ec2Connection {
  def ec2InitTask[OperationResult](thisTask: TaskKey[OperationResult],
                                   operation: (AmazonEC2Client, Boolean) => OperationResult,
                                   msgInfo: OperationResult => Option[String] = (s: OperationResult) => None,
                                   msgDebug: OperationResult => Option[String] = (s: OperationResult) => None
                                    ): Def.Initialize[Task[OperationResult]] =
    (credentials in thisTask, ec2Region in thisTask, ec2Progress in thisTask, ec2UseInstanceProfileCredentials in thisTask, streams in thisTask) map {
      fun(operation, msgInfo, msgDebug)
    }

  def ec2InitInputTask[OperationResult](thisTask: InputKey[OperationResult],
                                        operation: (AmazonEC2Client, Boolean) => OperationResult,
                                        msgInfo: OperationResult => Option[String] = (s: OperationResult) => None,
                                        msgDebug: OperationResult => Option[String] = (s: OperationResult) => None
                                         ): Def.Initialize[Task[OperationResult]] =
    (credentials in thisTask, ec2Region in thisTask, ec2Progress in thisTask, ec2UseInstanceProfileCredentials in thisTask, streams in thisTask) map {
      fun(operation, msgInfo, msgDebug)
    }

  def fun[OperationResult](operation: (AmazonEC2Client, Boolean) => OperationResult,
                           msgInfo: OperationResult => Option[String] = (s: OperationResult) => None,
                           msgDebug: OperationResult => Option[String] = (s: OperationResult) => None
                            ) = (creds: Seq[Credentials], region: String, progress: Boolean, useInstanceCreds: Boolean, streams: TaskStreams) => {
    val client = getClient(creds, region, useInstanceCreds)
    val result = operation(client, progress)
    msgDebug(result).map(s => streams.log.debug(s))
    msgInfo(result).map(s => streams.log.info(s))
    result
  }

}
