/** 
 * Copyright 2010 Northwestern University.
 *
 * Licensed under the Educational Community License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *    http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author Xin Xiang
 */

package edu.northwestern.art.jcr_access.services

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.File

import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import scala.xml.XML
import scala.xml.Node

object Int {
  def unapply(s : String) : Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _ : java.lang.NumberFormatException => None
  }
}

class XTFURLService extends HttpServlet {

  override def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    // Get the value of a request parameter; the name is case-sensitive
    val mets =     req.getParameter("mets")
    val idString = req.getParameter("id")

    val id = idString match {
       case Int(x) => x
       case _ => 1
    }

    val metsXML = XML.load(new URL(mets).openConnection.getInputStream)

    val os = res.getOutputStream

    var file: Node = null
    var found = false
    
    var myID = id
    do {
      file = (metsXML \\ "file")(myID - 1)
      val fileID = (file \ "@ID").text
      if (id == 1 && (fileID.contains("thumbnail") || fileID.contains("THUMBNAIL")) ||
        id == 2 && ! fileID.contains("thumbnail") && ! fileID.contains("THUMBNAIL")) {
        found = true
      }
      myID += 1
    } while (! found && myID <= (metsXML \\ "file").length)

    if (! found) {
      // use the default
      file = (metsXML \\ "file")(id - 1)
    }

    val flocatNode = (file \ "FLocat")(0)
	var urlString: String = (flocatNode \ "@{http://www.w3.org/1999/xlink}href").text
    if (urlString == "") {
      urlString = (flocatNode \ "@{http://www.w3.org/TR/xlink}href").text
    }

    if (urlString == null || ! urlString.startsWith("http")) {
      os.close
      return
    }

	// retrieve the image
    var connection: HttpURLConnection = null
    try {
      val url: URL = new URL(urlString)
      connection = url.openConnection.asInstanceOf[HttpURLConnection]

      connection.setRequestMethod("GET")
	  
      var is = connection.getInputStream

	  val inputChannel = Channels.newChannel(is).asInstanceOf[ReadableByteChannel]
      val outputChannel = Channels.newChannel(os).asInstanceOf[WritableByteChannel]
	  // Create a direct ByteBuffer
      val buf = ByteBuffer.allocateDirect(50 * 1024)

      while (inputChannel.read(buf) > 0) {
        buf.flip
        outputChannel.write(buf)
        buf.clear
      }
    } catch {
       case e: IOException => {
         // send a 1x1 image in case error occurs when retrieving the original image
         val image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
         ImageIO.write(image, "png", os)
       }
    } finally {
      connection.disconnect
      os.close
    }
  }
}
