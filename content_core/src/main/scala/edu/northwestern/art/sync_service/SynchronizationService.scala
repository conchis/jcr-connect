/**
 *   Copyright 2010 Northwestern University.
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

package edu.northwestern.art.sync_service

import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Consumes
import javax.ws.rs.Produces
import javax.ws.rs.Path
import javax.ws.rs.FormParam

import java.net.URL
import java.net.URLConnection
import java.net.URLEncoder

import java.io.InputStream
import java.io.OutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.IOException

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

import java.util.Date
import java.text.SimpleDateFormat

// import flexjson.JSON
// import flexjson.JSONSerializer

import net.sf.json.JSONObject
import net.sf.json.JSONArray
import net.sf.json.JSONSerializer


import javax.jcr.Session
import edu.northwestern.art.jcr_access.repositories.LocalConnector
import edu.northwestern.art.jcr_access.access.NoItemException
import edu.northwestern.art.content_core.catalog.Catalog
import edu.northwestern.art.content_core.content.Metadata
import edu.northwestern.art.content_core.images.BinaryImage
import edu.northwestern.art.content_core.images.ImageItem

// The class will be hosted at the URI path "/sync"
@Path("/sync")
class SynchronizationService {

  val repository_url = "http://localhost:4004/jackrabbit/rmi"
  val user = "admin"
  val pass = "admin"

  val connector = new LocalConnector(repository_url, user, pass)

  def getFolder(folderName: String): Catalog = {
    connector.session((jcr_session: Session) => {
      val container = jcr_session.getNode(folderName)
      var items: List[edu.northwestern.art.content_core.catalog.CatalogItem] = List()
      val iterator = container.getNodes
      while (iterator.hasNext()) {
        var node = iterator.nextNode
        println(node.getName)
        if (node.getName != "contents.json" && node.getName != "._contents.json") {
          val contents = node.getNode("contents.json/jcr:content");
          val content = new org.json.JSONObject(contents.getProperty("jcr:data").getString)
          val name: String = node.getName
          // println(name)
          val metadata = content.getJSONObject("metadata")
          val creators_array = metadata.getJSONArray("creators")
          var creators: List[String] = List()
          for (index <- 0 until creators_array.length)
            creators ::= creators_array.getString(index)
          creators.reverse
          val title: String = metadata.getString("title")
          val modified = node.getProperty("jcr:created").getDate.getTime
          
          val sources = content.getJSONObject("sources")
          val thumb = sources.getJSONObject("thumbnail")
          val tname   = thumb.getString("name")
          val format = thumb.getString("format")
          val width  = thumb.getInt("width")
          val height = thumb.getInt("height")
          val catalogThumb = new edu.northwestern.art.content_core.catalog.Thumbnail(name + "/" + tname + "." + format, width, height)

          items ::= new edu.northwestern.art.content_core.catalog.CatalogImageItem(name, title, 
                                                                            creators, 
                                                                            catalogThumb,
                                                                            modified)
        }
      }
      new Catalog(container.getName, "", items.reverse)
    })
  }

  def retrieveFile(urlString: String): Array[Byte] = {
    var url = new URL(urlString)
    var is = url.openStream()

    val channel = Channels.newChannel(is).asInstanceOf[ReadableByteChannel]
    // Create a direct ByteBuffer
    val buf = ByteBuffer.allocateDirect(10 * 1024 * 1024);
    var numRead = 0
    var length = 0

    while (numRead >= 0) {
      // Read bytes from the channel
      try {
        numRead = channel.read(buf)
      } catch {
        case e: IOException => e.printStackTrace()
      }

      if (numRead > 0) {
        length += numRead
      }
    }  

    val bytes: Array[Byte] = new Array(length)
    // reset the position of the buffer to zero
    buf.rewind
    buf.get(bytes)

    return bytes
  }

  @GET 
  def getCurrentTime(): String = {
    return "" + System.currentTimeMillis
  }

  @POST
  def getClichedMessagePOST(@FormParam("manifest") manifest: String, @FormParam("path") path: String): String = {
    var json = JSONSerializer.toJSON( manifest ).asInstanceOf[JSONObject]
    var body = json.getJSONObject("body")
    var header = json.getJSONObject("header")
    var completed = 0
    var total = body.keySet.size

    var destinationPath = header.getString("destinationPath")
    var basePath = header.getString("basePath")

    // val folder = getFolder(destinationPath)
    // val folder = connector.catalog(destinationPath)
    // println(folder)
    // val children = folder.children
    // val childrenName = folder.children.map(child => child.name)
    
    var iterator = body.keySet.iterator
    while (iterator.hasNext) {
      var id = iterator.next.asInstanceOf[String]
      var item = body.getJSONObject(id)
      // relative path of the item
      var p = item.getString("path")

      val clientModified = item.getString("modified")
      var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
      var date = new Date
      try {
        date = dateFormat.parse(clientModified)
      } catch {
        case e: Exception => e.printStackTrace()
      }

      try {
        // get an Item object
        var repositoryItem = connector.get(destinationPath + p)

        if (date.after(repositoryItem.modified)) {
          // updated
          println("updated: " + repositoryItem.name + ", " + repositoryItem.modified + ", " + date)

          val metadata = repositoryItem.metadata
          val properties = item.getJSONObject("properties")
          val title = properties.getString("title")
          val description = properties.getString("description")
          metadata.title = title
          metadata.description = description
          repositoryItem.metadata = metadata

          connector.put(destinationPath + p, repositoryItem)          
        }
      } catch {
        case e: NoItemException =>
      // if (! childrenName.contains(id)) {
        // a new item
        println("new item: " + id)

        try {
          // val bytes = retrieveFile(filePath)
          // var f = new File("test.jpg")
          // var out = new FileOutputStream(f).asInstanceOf[OutputStream]
          // out.write(bytes, 0, bytes.length)
          // out.close()
          // println("Image file is created")
          // var bufferedImage = ImageIO.read(f)
          // f = new File("test.png")
          // ImageIO.write(bufferedImage, "png", f)

          // write to the repository
          val properties = item.getJSONObject("properties")
          val title = properties.getString("title")
          val description = properties.getString("description")
          println("title: " + title + ", description: " + description)
          val metadata = Metadata.apply(title, description)

          var contentType = item.getString("type")

          contentType match {
            case "BinaryImage" => {
              // retrieve image data
              val filePath = basePath + p + "/image"
              val bufferedImage = ImageIO.read(new URL(filePath))

              // write to a file
              val f = new File("test.png")
              ImageIO.write(bufferedImage, "png", f)
              println("PNG file is created")

              // write to the repository
              val binaryImage = new BinaryImage()
              binaryImage.image = bufferedImage
              binaryImage.name = id
              binaryImage.format = "JPEG"
              binaryImage.width = bufferedImage.getWidth
              binaryImage.height = bufferedImage.getHeight
              val imageItem = ImageItem.initialize(new ImageItem, id, metadata, date, List(), Map())
              imageItem.sources.put("fullsize", binaryImage)
              
              // imageItem.created = date
              // imageItem.updated = date
    
              connector.put(destinationPath + p, imageItem)
            }              
            case _ => {

            }
          }
        }
        catch {
          case e: IOException => e.printStackTrace()
        }
      }

      completed += 1

      try {
        var updateURL = new URL( 
          basePath + "/updateSyncProgress?n=" + 
          URLEncoder.encode(p, "UTF-8") + "&c=" + completed + "&t=" + total)
        var updateConnection = updateURL.openConnection()
        updateConnection.connect()      
        updateConnection.getInputStream().close()
      } catch {               // openConnection() failed
        case e: IOException => e.printStackTrace()
      }
    }

    // Return some cliched textual content
    return "JCR synchronization service"
  }
}
