package io.github.morgaroth.sbt.ec2

import com.amazonaws.services.ec2.model.Instance
import sbt._

object keys extends GettingInstancesStrategies {

  /**
   * The task "ec2ListRunning" return list of running machines.
   * Depends on:
   * - ''credentials in ec2ListRunning:'' security credentials used to access the EC2 console, as follows:
   * - ''realm:'' "Amazon EC2"
   * - ''host:'' the string specify ec2 region, see below ec2Region
   * - ''user:'' Access Key ID
   * - ''password:'' Secret Access Key
   * - ''ec2Region in ec2ListRunning:'' the region name (for more [[http://docs.aws.amazon.com/general/latest/gr/rande.html#ec2_region AWS EC2 Doc]])
   *
   * If you set logLevel to "Level.Debug", size of received list of instances will be printed.
   */
  val ec2ListRunning = taskKey[List[Instance]]("Return list running instances.")

  /**
   * The task "ec2PrintRunning" prints list of running machines with names and public IPs.
   * Depends on task ec2ListRunning, so require all of dependencies of ec2ListRunning, @see [[ec2ListRunning]]
   *
   * If you set logLevel to "Level.Debug", all info about instances will be printed.
   */
  val ec2PrintRunning = taskKey[Seq[Instance]]("Prints list of running instances.")


  /**
   * The task "{{{ec2FindByName}}}" finds running instances which name contains provided name, or provided name contain instance's name
   * It is week match, for possibly wide list of results
   * Example use:
   * {{{sbt ec2FindByNames my-server backend-stg}}}
   *
   *
   * It rests on access credentials described in {{{ec2ListRunning}}}
   */
  val ec2FindByName = inputKey[List[Instance]]("Filters instances")

  /**
   * The same working logic as {{{ec2FindByName}}} but for more than one query name
   * Example use:
   * {{{sbt ec2FindByNames my-server backend-stg}}}
   */
  val ec2FindByNames = inputKey[List[Instance]]("Filters instances")

  /**
   * Setting key for Getting instances strategy, getting strategies are described in readme. Look at it.
   */
  val ec2GettingStrategy = settingKey[GettingInstancesStrategy]("Sets strategy used during getting instances.")


  /**
   * The task "{{{ec2GetInstances}}}" finds running instances with names as provided in query, restricted selecting of ec2 instances
   * is done by getting instances strategy. For more info look at documentation in readme.
   *
   * It rests on access credentials described in {{{ec2ListRunning}}}
   */
  val ec2GetInstances = inputKey[Option[List[Instance]]]("Returns running instances that strategy accepts.")

  /**
   * Specifies region to which plugin should connect.
   */
  val ec2Region = settingKey[String]("AWS region used for operations.")

  /**
   * If you set "progress" to true, a progress indicator will be displayed while the individual files are uploaded or downloaded.
   * Only recommended for interactive use or testing; the default value is false.
   */
  val ec2Progress = settingKey[Boolean]("Set to true to get a progress indicator during S3 uploads/downloads (default false).")

  /**
   * If you set InstanceProfileCredentials to true, You can use instance credentials, which are accesible from ec2 machine implicitly
   * instead of defining credentials in .ec2Credentials file.
   * For more background look at http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html
   * Only recommended for code running in EC2 machines. Default is false.
   */
  val ec2UseInstanceProfileCredentials = settingKey[Boolean]("Set to true to getting credentials inside EC2 machine (default false).")

  val ec2PrintCredentials = taskKey[Unit]("Prints setting used to connect to AWS.")

}
