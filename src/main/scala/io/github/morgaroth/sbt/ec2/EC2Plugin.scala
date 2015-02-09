package io.github.morgaroth.sbt.ec2

import com.amazonaws.auth.{BasicAWSCredentials, InstanceProfileCredentialsProvider}
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.{Filter, DescribeInstancesResult, DescribeInstancesRequest}
import com.amazonaws.{ClientConfiguration, Protocol}
import sbt.Keys._
import sbt.Scoped.RichTaskable3
import sbt._
import sbt.complete.Parsers.spaceDelimited

import scala.language.reflectiveCalls

/**
 * EC2Plugin is a simple sbt plugin that can manipulate objects on Amazon S3.
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
object EC2Plugin extends AutoPlugin with progressHelpers {

  /**
   * This nested object defines the sbt keys made available by the EC2Plugin: read here for tasks info.
   */
  object autoImport {

    /**
     * The task "s3Upload" uploads a set of files to a specificed S3 bucket.
     * Depends on:
     * - ''credentials in S3.upload:'' security credentials used to access the S3 bucket, as follows:
     * - ''realm:'' "Amazon S3"
     * - ''host:'' the string specified by S3.host in S3.upload, see below
     * - ''user:'' Access Key ID
     * - ''password:'' Secret Access Key
     * - ''mappings in S3.upload:'' the list of local files and S3 keys (pathnames), for example:
     * `Seq((File("f1.txt"),"aaa/bbb/file1.txt"), ...)`
     * - ''S3.host in S3.upload:'' the bucket name, in one of two forms:
     * 1. "mybucket.s3.amazonaws.com", where "mybucket" is the bucket name, or
     * 1. "mybucket", for instance in case the name is a fully qualified hostname used in a CNAME
     *
     * If you set logLevel to "Level.Debug", the list of files will be printed while uploading.
     */
    val ec2ListRunning = taskKey[Seq[String]]("Lists running instances.")
    val ec2Filter = inputKey[Unit]("Filters instances")

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
    val ec2Host = settingKey[String]("Host used by the S3 operation, either \"mybucket.s3.amazonaws.com\" or \"mybucket\".")

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

  type Bucket = String

  private def getClient(creds: Seq[Credentials], host: String, useInstanceProfileCredentials: Boolean) = {
    val credentialsProvider = getCredentials(creds, host, useInstanceProfileCredentials)._1
    new AmazonEC2Client(credentialsProvider, new ClientConfiguration().withProtocol(Protocol.HTTPS))
  }

  def getCredentials(creds: Seq[Credentials], host: String, useInstanceProfileCredentials: Boolean) = {
    Credentials.forHost(creds, host) match {
      case Some(cred) => new StaticCredentialsProvider(new BasicAWSCredentials(cred.userName, cred.passwd)) -> Left(cred)
      case None if useInstanceProfileCredentials =>
        val instance = new InstanceProfileCredentialsProvider
        instance -> Right(instance) // for get both amazon credentials and direct sbt credentials too
      case _ => sys.error("Could not find S3 credentials for the host: %s.".format(host))
    }
  }

  private def ec2InitTask[Item, OperationResult](thisTask: TaskKey[OperationResult],
                                                 operation: (AmazonEC2Client, Boolean) => OperationResult,
                                                 msg: OperationResult => String) =

    (credentials in thisTask, ec2Host in thisTask, ec2Progress in thisTask, ec2UseInstanceProfileCredentials in thisTask, streams) map {
      (creds, host, progress, useInstanceCreds, streams) =>
        val client = getClient(creds, host, useInstanceCreds)
        val result = operation(client, progress)
        streams.log.info(msg(result))
        result
    }

  def printCredentials: RichTaskable3[Seq[Credentials], TaskStreams, String]#App[Unit] = {
    (credentials, streams, ec2Host, ec2UseInstanceProfileCredentials) map { (creds, streams, host, useInstanceProfileCredentials) =>
      val credentials = getCredentials(creds, host, useInstanceProfileCredentials)._2.left
      credentials.map { cred =>
        streams.log.info("\taccess id:\t%s".format(cred.userName.replaceFirst( """.{15}""", "*****")))
        streams.log.info("\taccess key:\t%s".format(cred.passwd.replaceFirst( """.{35}""", "*****")))
      }
    }
  }

  /*
   * Include the line {{{s3Settings}}} in your build.sbt file, in order to import the tasks defined by this S3 plugin.
   */

  override def projectSettings = Seq(

    ec2ListRunning <<= ec2InitTask[String, Seq[String]](ec2ListRunning, {
      case (client, progress) =>
        val request = new DescribeInstancesRequest().withFilters(new Filter())
        val a: DescribeInstancesResult = client.describeInstances(request)
        a.getReservations
        Seq.empty
    }, {
      case result => "Uploading dsa as dsa into fds end with result %s".format(result)
    }
    ),

    ec2Filter := {
      val args = spaceDelimited("<args>").parsed
      (streams in ec2Filter).value.log.info("checked %s".format(args))
    },

    //    s3Download <<= s3InitTask[(File, String), DownloadResult](s3Download, mappings, { case (client, bucket, (file, key), progress) =>
    //      val request = new GetObjectRequest(bucket, key)
    //      val objectMetadata = client.getObjectMetadata(bucket, key)
    //      if (progress) addProgressListener(request, objectMetadata.getContentLength, key)
    //      client.getObject(request, file)
    //      DownloadResult(file)
    //    }, {
    //      case (bucket, (file, key)) => "Downloading %s as %s from %s".format(file.getAbsolutePath, key, bucket)
    //    }, {
    //      (bucket, mapps) => "Downloaded %d files from the S3 bucket \"%s\".".format(mapps.length, bucket)
    //    }
    //    ),
    //
    //    s3Delete <<= s3InitTask[String, Unit](s3Delete, s3Keys, {
    //      (client, bucket, key, _) => client.deleteObject(bucket, key)
    //    }, {
    //      (bucket, key) => "Deleting %s from %s.".format(key, bucket)
    //    }, {
    //      (bucket, keys1) => "Deleted %d objects from the S3 bucket \"%s\".".format(keys1.length, bucket)
    //    }
    //    ),

    ec2PrintCredentials <<= printCredentials,

    ec2Host := "",
    //    s3Keys := Seq(),
    //    mappings in s3Download := Seq(),
    //    mappings in s3Upload := Seq(),
    ec2Progress := true,
    ec2UseInstanceProfileCredentials := false
  )

}
