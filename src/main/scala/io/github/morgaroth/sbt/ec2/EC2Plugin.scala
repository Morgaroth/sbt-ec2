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
 * EC2Plugin is a simple sbt plugin that can manipulate objects on Amazon EC2.
 *
 * == Example ==
 * Here is a complete example:
 *
 * - project/plugin.sbt:
 * {{{addSbtPlugin("io.github.morgaroth" % "sbt-ec2" % "0.1")}}}
 *
 * - build.sbt:
 * {{{
 *
 * host in  := "s3sbt-test.s3.amazonaws.com"
 *
 * credentials += Credentials(Path.userHome / ".s3credentials")
 * }}}
 *
 * - ~/.s3credentials:
 * {{{
 * realm=Amazon S3
 * host=s3sbt-test.s3.amazonaws.com
 * user=<Access Key ID>
 * password=<Secret Access Key>
 * }}}
 *
 * Just create two sample files called "a" and "b" in the same directory that contains build.sbt,
 * then try:
 * {{{$ sbt s3-upload}}}
 *
 * You can also see progress while uploading:
 * {{{
 * $ sbt
 * > set S3.progress in S3.upload := true
 * > s3-upload
 * [==================================================]   100%   zipa.txt
 * [=====================================>            ]    74%   zipb.jar
 * }}}
 *
 * Please select the nested `S3` object link, below, for additional information on the available tasks.
 */
object EC2Plugin extends AutoPlugin with progressHelpers with RichAmazonModelsSupport with ec2Connection with EC2PluginHelpers with findingInstances {

  /**
   * This nested object defines the sbt keys made available by the EC2Plugin: read here for tasks info.
   */
  object autoImport {

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
    val ec2ListRunning = taskKey[Seq[Instance]]("Return list running instances.")

    /**
     * The task "ec2PrintRunning" prints list of running machines with names and public IPs.
     * Depends on task ec2ListRunning, so require all of dependencies of ec2ListRunning, @see [[ec2ListRunning]]
     *
     * If you set logLevel to "Level.Debug", all info about instances will be printed.
     */
    val ec2PrintRunning = taskKey[Seq[Instance]]("Prints list of running instances.")

    val ec2FindByNames = inputKey[List[Instance]]("Filters instances")
    val ec2FindByName = inputKey[List[Instance]]("Filters instances")

    /**
     * The task "s3Download" downloads a set of files from a specificed S3 bucket.
     * Depends on:
     * - ''credentials in S3.download:'' security credentials used to access the S3 bucket, as follows:
     * - ''realm:'' "Amazon S3"
     * - ''host:'' the string specified by S3.host in S3.download, see below
     * - ''user:'' Access Key ID
     * - ''password:'' Secret Access Key
     * - ''mappings in S3.download:'' the list of local files and S3 keys (pathnames), for example:
     * `Seq((File("f1.txt"),"aaa/bbb/file1.txt"), ...)`
     * - ''S3.host in S3.download:'' the bucket name, in one of two forms:
     * 1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
     * 1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
     *
     * If you set logLevel to "Level.Debug", the list of files will be printed while downloading.
     */
    //    val s3Download = taskKey[Seq[DownloadResult]]("Downloads files from an S3 bucket.")

    /**
     * The task "s3Delete" deletes a set of files from a specified S3 bucket.
     * Depends on:
     * - ''credentials in S3.delete:'' security credentials used to access the S3 bucket, as follows:
     * - ''realm:'' "Amazon S3"
     * - ''host:'' the string specified by S3.host in S3.delete, see below
     * - ''user:'' Access Key ID
     * - ''password:'' Secret Access Key
     * - ''S3.keys in S3.delete:'' the list of S3 keys (pathnames), for example:
     * `Seq("aaa/bbb/file1.txt", ...)`
     * - ''S3.host in S3.delete:'' the bucket name, in one of two forms:
     * 1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
     * 1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
     *
     * If you set logLevel to "Level.Debug", the list of keys will be printed while the S3 objects are being deleted.
     */
    //    val s3Delete = taskKey[Seq[Unit]]("Delete files from an S3 bucket.")

    /**
     * A string representing the S3 bucket name, in one of two forms:
     * 1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
     * 1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
     */
    val ec2Region = settingKey[String]("Host used by the S3 operation, either \"mybucket.s3.amazonaws.com\" or \"mybucket\".")

    /**
     * A list of S3 keys (pathnames) representing objects in a bucket on which a certain operation should be performed.
     */
    //    val s3Keys = taskKey[Seq[String]]("List of S3 keys (pathnames) on which to perform a certain operation.")

    /**
     * If you set "progress" to true, a progress indicator will be displayed while the individual files are uploaded or downloaded.
     * Only recommended for interactive use or testing; the default value is false.
     */
    val ec2Progress = settingKey[Boolean]("Set to true to get a progress indicator during S3 uploads/downloads (default false).")

    /**
     * If you set InstanceProfileCredentials to true, You can use instance credentials, which are accesible from ec2 machine implicitly
     * instead of defining credentials in .s3setings file.
     * For more background look at http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html
     * Only recommended for code running in EC2 machines. Default is false.
     */
    val ec2UseInstanceProfileCredentials = settingKey[Boolean]("Set to true to getting credentials inside EC2 machine (default false).")

    val ec2PrintCredentials = taskKey[Unit]("Prints setting used to connect to AWS.")
  }

  import io.github.morgaroth.sbt.ec2.EC2Plugin.autoImport._

  override def projectSettings = Seq(

    ec2ListRunning <<= ec2InitTask[Seq[Instance]](ec2ListRunning, {
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

    ec2PrintCredentials <<= printCredentials,
    ec2Region := "",
    ec2Progress := true,
    ec2UseInstanceProfileCredentials := false
  )

  def printableInstanceInfo(i: Instance): String = {
    "name=%s\tip=%s".format(i.getTags.asScala.find(_.getKey == "Name").map(_.getValue).getOrElse("unnamed"), i.getPublicIpAddress)
  }
}
