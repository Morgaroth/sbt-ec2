package io.github.morgaroth.sbt.ec2

import java.io.File

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.event.{ProgressEvent, ProgressEventType, ProgressListener}

import scala.language.reflectiveCalls

trait progressHelpers {
  protected def progressBar(percent: Int) = {
    val b = "=================================================="
    val s = "                                                 "
    val p = percent / 2
    val z: StringBuilder = new StringBuilder(80)
    z.append("\r[")
    z.append(b.substring(0, p))
    if (p < 50) {
      z.append(">")
      z.append(s.substring(p))
    }
    z.append("]   ")
    if (p < 5) z.append(" ")
    if (p < 50) z.append(" ")
    z.append(percent)
    z.append("%   ")
    z.mkString
  }

  protected def addProgressListener(request: AmazonWebServiceRequest {// structural
  def setGeneralProgressListener(progressListener: ProgressListener): Unit
  }, fileSize: Long, key: String) = request.setGeneralProgressListener(new ProgressListener() {
    var uploadedBytes = 0L
    val fileName = {
      val area = 30
      val n = new File(key).getName
      val l = n.length()
      if (l > area - 3)
        "..." + n.substring(l - area + 3)
      else
        n
    }

    def progressChanged(progressEvent: ProgressEvent) {
      if (progressEvent.getEventType == ProgressEventType.TRANSFER_PART_COMPLETED_EVENT) {
        uploadedBytes = uploadedBytes + progressEvent.getBytesTransferred
      }
      print(progressBar(if (fileSize > 0) ((uploadedBytes * 100) / fileSize).toInt else 100))
      print(fileName)
      if (progressEvent.getEventType == ProgressEventType.TRANSFER_COMPLETED_EVENT)
        println()
    }
  })
}
