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
 * @author Jonathan A. Smith, Xin Xiang
 * @version September 1, 2010
 */

package edu.northwestern.art.jcr_access.repositories

import java.io.ByteArrayInputStream
import collection.mutable.ListBuffer

import java.util.{Calendar, Date}
import javax.jcr.query.Query
import javax.jcr.{Node, PathNotFoundException, Session}

import java.net.URL

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import scala.xml._

import org.json.{JSONException, JSONArray, JSONObject}

import edu.northwestern.art.content_core.catalog.{CatalogImageItem, Thumbnail, Catalog, CatalogItem}
import edu.northwestern.art.content_core.content.{Metadata, Item}
import edu.northwestern.art.content_core.utilities.Path
import edu.northwestern.art.jcr_access.access.{FailureException, NoItemException, RepositoryConnector}
import edu.northwestern.art.content_core.images.{TiledImageURL, ImageURL, ImageSource, ImageItem, BinaryImage}

/**
 * Simple connector to a XTF repository storing content in a straightforward
 * way -- simple files and folders.
 */

class XTFConnector(repository_url: String, user: String,
          password: String) extends
        RepositoryConnector(repository_url, "xtf", user, password) {

  def isItem(path: String): Boolean = {
    session((jcr_session: Session) => {
      try {
        getContentJSON(jcr_session.getNode(path))
        true
      }
      catch {
        case _: PathNotFoundException =>
          false
        case _: NoItemException =>
          false
      }
    })
  }

  def get(path: String): Item = {
    session((jcr_session: Session) => {
      try {
        val node = jcr_session.getNode(path)
        val json = getContentJSON(node)
        val modified = getModified(node)

        // FIXME should handle any type of item
        makeImageItem(node.getName, json, modified)
      }
      catch {
        case _: PathNotFoundException =>
          throw new NoItemException
        case except: JSONException =>
          throw new FailureException(except)
      }
    })
  }

  /**
   * Constructs an ImageItem from contents.json.
   */

  private def makeImageItem(name: String, json: JSONObject,
      modified: Date): ImageItem = {
    ImageItem(name, metadata = makeMetadata(json),
        modified = modified, sources = makeImageSources(json))
  }

  /**                         
   * Creates a Metadata object from item JSON.
   */

  private def makeMetadata(json: JSONObject): Metadata = {
    val metadata_json = json.getJSONObject("metadata")
    val title: String = metadata_json.getString("title")
    val description: String = metadata_json.getString("description")
    val creators = extractJSONArray(metadata_json, "creators")
    val rights = extractJSONArray(metadata_json, "rights")
    val types = extractJSONArray(metadata_json, "types")

    Metadata(title = title, description = description, creators = creators, rights = rights,
      types = types)
  }

  /**
   * Extracts a list of strings from a field in a JSONObject.
   */

  private def extractJSONArray(json: JSONObject, field: String) = {
    val buffer = new ListBuffer[String]
    if (json.has(field)) {
      val json_array = json.getJSONArray(field)
      for (index <- 0 until json_array.length)
        buffer.append(json_array.getString(index))
    }
    buffer.toList
  }

  /**
   * Creates list of ImageSource objects for each image source specified
   * in the JSON data.
   */

  private def makeImageSources(json: JSONObject): Map[String, ImageSource] = {
    val sources = new collection.mutable.HashMap[String, ImageSource]

    if (json.has("sources")) {
      val sources_json = json.getJSONObject("sources")
      val names = sources_json.keys
      while (names.hasNext) {
        val name = names.next.asInstanceOf[String]
        val source_json = sources_json.getJSONObject(name)
        val source = makeImageSource(source_json)
        if (source != null)
          sources(name) = source
      }
    }

    sources.toMap
  }

  /**
   * Extracts a String valued property from a JSONObject. Allows the specification
   * of a default value if the property is not set.
   */

  private def extractString(json: JSONObject, name: String, default: String) = {
    if (json.has(name))
      json.getString(name)
    else
      default
  }

  /**
   * Extracts an Int valued property from a JSONObject. Allows the specification
   * of a default value if the property is not set.
   */

  private def extractInt(json: JSONObject, name: String, default: Int) = {
    if (json.has(name))
      json.getInt(name)
    else
      default
  }

  /**
   * Builds an ImageSource object from a json representation.
   */

  private def makeImageSource(source_json: JSONObject) = {
    val name = source_json.getString("name")
    val format = extractString(source_json, "format", null)
    val width = extractInt(source_json, "width", 0)
    val height = extractInt(source_json, "height", 0)
    var type_name: String = ""
    try {
      type_name = source_json.getString("type")
    } catch {
      case e: Exception =>
        // FIXME as type is not serialized in contens.json for ImageItem
        type_name = "BinaryImage"
    }

    type_name match {
      case "ImageURL" =>
        val url = extractString(source_json, "url", null)
        ImageURL(name, url, format, width, height)
      case "TiledImageURL" =>
        val url = extractString(source_json, "url", null)
        TiledImageURL(name, url, format, width, height)
      case "BinaryImage" =>  // FIXME implement this
        BinaryImage(name, null, format, width, height)
    }
  }

  /**
   * Creates or replaces an item in the repository.
   */

  def put(path: String, item: Item) = {
    val location = Path(path)
    item.name = location.name
    session((jcr_session: Session) => {
      if (!isItem(path)) {
        val parent = jcr_session.getNode(location.parent)
        createItem(parent, item)
      }
      else {
        val item_node = jcr_session.getNode(path)
        updateItem(item_node, item)
      }
      jcr_session.save
    })
  }

  def createItem(parent: Node, item: Item) {
    val item_json = item.toJSON.toString
    val item_node = parent.addNode(item.name, "nt:folder")
    addFileNode(item_node, "contents.json", "application/json",
      item_json.toString.getBytes, item.modified)
  }

  def updateItem(node: Node, item: Item) {
    val item_json = item.toJSON.toString
    val contents = node.getNode("contents.json")
    setFileNode(contents, "application/json", item_json.getBytes,
      item.modified)
  }

  def addFileNode(parent: Node, name: String, mime_type: String,
      bytes: Array[Byte], modified: Date) {
    val file_node = parent.addNode(name, "nt:file")
    val content_node = file_node.addNode("jcr:content", "nt:unstructured")
    setFileNode(file_node, mime_type, bytes, modified)
  }

  def setFileNode(file_node: Node, mime_type: String, bytes: Array[Byte],
      modified: Date = new Date) {
    val content_node = file_node.getNode("jcr:content")
    content_node.setProperty("jcr:mimeType", mime_type)
    val byte_stream = new ByteArrayInputStream(bytes)
    content_node.setProperty("jcr:data", byte_stream)
    val modified_calendar = Calendar.getInstance
    modified_calendar.setTime(modified)
    content_node.setProperty("jcr:lastModified", modified_calendar)
  }

  /**
   * Catalogs content under a specified folder.
   */

  def catalog(path: String): Catalog = {

    session((jcr_session: Session) => {
      try {
        val container = jcr_session.getNode(path)
        val items = new ListBuffer[CatalogItem]
        val iterator = container.getNodes
        while (iterator.hasNext()) {
          try {
            items.append(makeCatalogItem(iterator.nextNode))
          }
          catch {
            case _: NoItemException       => ;
            case _: JSONException         => ;
            case _: PathNotFoundException => ;
          }
        }
        new Catalog(container.getName, "", items.toList)
      }
      catch {
        case _: PathNotFoundException =>
          throw new NoItemException
        case except: JSONException =>
          throw new FailureException(except)
      }
    })
  }

  /**
   *  Free text search of the repository. Returns a Folder of results.
   */

  def search(text: String): Catalog = {
    val results =
      session((session: Session) => {
        var results: List[CatalogItem] = List()
        // val statement = "//element(*, nt:file)[jcr:contains(jcr:content, '" + text + "')]"
        // val statement = "//*[jcr:contains(@jcr:data, '" + text + "')]"
        val statement = "//element(*, nt:unstructured)[jcr:contains(@subject, 'renaissance') and jcr:contains(@facet-type-tab, 'image')]"

        val query = session.getWorkspace.getQueryManager.createQuery(statement, Query.XPATH)
        val iterator = query.execute.getNodes
        while (iterator.hasNext()) {
          val node = iterator.nextNode
          try {
            results ::= makeCatalogItem(node.getParent)
          }
          catch {
            case _: NoItemException => ;
          }
        }
        results
      })
    new Catalog("content", "Search Results", results.reverse)
  }

  /**
   * Creates a catalog item.
   */

  private def makeCatalogItem(node: Node): CatalogItem = {
    val content = getContentJSON(node)
    val name = node.getName
    val metadata_json = content.getJSONObject("metadata")
    val creators = extractJSONArray(metadata_json, "creators")
    val title = metadata_json.getString("title")
    val modified = getModified(node)
    new CatalogImageItem(
      name, title, creators, getCatalogThumb(name, content), modified)
  }

  /**
   * Returns a JSONObject describing the node's content.
   */

  private def getContentJSON(node: Node): JSONObject = {
    try {
      var description: String = ""
      var creator: String = ""
      var rights: String = ""
      var t: String = ""
      var property =  node.getProperty("description")
      var i = 0

      if (property.getDefinition.isMultiple) {
		val values = property.getValues();
        
		for (value <- values) {
          if (i > 0) {
            description += "\n\n"
          }

		  description += value.getString
        }
      }
      else {
        description = property.getString
      }

      if (node.hasProperty("creator")) {
        creator = node.getProperty("creator").getString
      }
      
      if (node.hasProperty("rights")) {
        rights = node.getProperty("rights").getString
      }
      
      if (node.hasProperty("type")) {
        t = node.getProperty("type").getString
      }
      
      val metadata: Metadata = Metadata(node.getProperty("title").getString, 
                                        description,
                                        List(creator),
                                        List(rights),
                                        List(t))

      property = node.getProperty("fileLocation");

      val sources = new collection.mutable.HashMap[String, ImageSource]
      var source: ImageSource = null
      var image: BufferedImage = null
      var name: String = ""

	  if (property.getDefinition.isMultiple) {
		val values = property.getValues();

        i = 0
		for (value <- values) {
		  image = ImageIO.read(new URL(value.getString))
          name = "image" + i
          source = BinaryImage(name, image, "jpg", image.getWidth, image.getHeight)

          if (source != null)
            sources(name) = source
          i += 1
		}
	  }
	  else {
		image = ImageIO.read(new URL(property.getString))
        name = "image"
        source = BinaryImage(name, image, "jpg", image.getWidth, image.getHeight)
        
        if (source != null)
          sources(name) = source
	  }

      val item = ImageItem(node.getName, metadata, new Date, List(), sources.toMap)

      return item.toJSON
    }
    catch {
      case except: PathNotFoundException =>
        throw new NoItemException
      case except =>
        except.printStackTrace
        throw new FailureException(except)
    }
  }

  /**
   * Returns the modified time for the item by examining the lastModified
   * property on the content.json file.
   */

  private def getModified(node: Node): Date = {
    try {
      // val contents = node.getNode("contents.json/jcr:content");
      // contents.getProperty("jcr:lastModified").getDate.getTime
      node.getNode(node.getName + ".mets.xml").getProperty("jcr:created").getDate.getTime
    }
    catch {
        case except: PathNotFoundException =>
          except.printStackTrace
          throw new NoItemException
        case except =>
          throw new FailureException(except)
    }
  }

  /**
   *  Constructs a Thumbnail object from JSON data. 
   */

  private def getCatalogThumb(node_name: String, contents: JSONObject) = {
    val sources = contents.getJSONObject("sources")
    val thumb = sources.getJSONObject("thumbnail")

    val name   = thumb.getString("name")
    val format = thumb.getString("format")
    val width  = thumb.getInt("width")
    val height = thumb.getInt("height")
    new Thumbnail(node_name + "/" + name + "." + format, width, height)
  }

}
