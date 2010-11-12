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

import javax.imageio.ImageIO

import scala.xml.XML

object Int {
  def unapply(s : String) : Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _ : java.lang.NumberFormatException => None
  }
}

class XTFURLService extends HttpServlet {

  override def doGet(req: HttpServletRequest, res: HttpServletResponse) = {
    // Get the value of a request parameter; the name is case-sensitive
    val mets =     req.getParameter("mets")
    val idString = req.getParameter("id")

    val id = idString match {
       case Int(x) => x
       case _ => 1
    }

    println(mets)

    val metsXML = XML.load(new URL(mets).openConnection.getInputStream)

    val out = res.getOutputStream

	val file = (metsXML \\ "file")(id - 1)
    println((file \ "@MIMETYPE").text)
	val urlString: String = (file \ "FLocat" \ "@{http://www.w3.org/TR/xlink}href").text
    println((file \ "FLocat" \ "@LOCTYPE").text)
    println((file \ "FLocat" \ "@{http://www.w3.org/TR/xlink}role").text)
    println(urlString)
    if (urlString == null || ! urlString.startsWith("http")) {
      out.close
    }

	// retrieve the image
    val url: URL = new URL(urlString);
    val connection: HttpURLConnection = url.openConnection.asInstanceOf[HttpURLConnection]
    // connection.setDoOutput(true);

    connection.setRequestMethod("GET")
	
    var is = connection.getInputStream();

	val channel = Channels.newChannel(is).asInstanceOf[ReadableByteChannel]
	// Create a direct ByteBuffer
    val buf = ByteBuffer.allocateDirect(10 * 1024 * 1024);
	var numRead = 0
	var length = 0

	while (numRead >= 0) {
      // Read bytes from the channel
	  try {
		numRead = channel.read(buf);
	  } catch {
		case e: IOException => e.printStackTrace();
	  }

	  if (numRead > 0) {
		length += numRead;
	  }
    }	

	val bytes: Array[Byte] = new Array(length);
	// reset the position of the buffer to zero
	buf.rewind;
	buf.get(bytes);

	connection.disconnect
	
	out.write(bytes, 0, length);
    out.close
  }
}
