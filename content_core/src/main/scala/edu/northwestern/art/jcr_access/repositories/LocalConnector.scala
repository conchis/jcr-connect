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
 * @author Jonathan A. Smith
 * @version 29 July 2010
 */

package edu.northwestern.art.jcr_access.repositories

import java.io.ByteArrayInputStream
import collection.mutable.ListBuffer

import org.json.{JSONException, JSONArray, JSONObject}

import javax.jcr.query.Query
import javax.jcr.{Node, PathNotFoundException, Session}

import edu.northwestern.art.content_core.catalog.{CatalogImageItem, Thumbnail, Catalog, CatalogItem}
import edu.northwestern.art.content_core.content.{Metadata, Item}
import edu.northwestern.art.content_core.images.{ImageSource, ImageItem}
import edu.northwestern.art.content_core.utilities.Path
import edu.northwestern.art.jcr_access.access.{FailureException, NoItemException, RepositoryConnector}
import java.util.{Calendar, Date}

/**
 * Simple connector to a local repository storing content in a straightforward
 * way -- simple files and folders.
 */

class LocalConnector(repository_url: String, user: String,
          password: String) extends
        RepositoryConnector(repository_url, null, user, password) {

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
        modified = modified, sources = List())
  }

  /**                         
   * Creates a Metadata object from item JSON.
   */

  private def makeMetadata(json: JSONObject): Metadata = {
    val metadata_json = json.getJSONObject("metadata")
    val title: String = metadata_json.getString("title")
    val creators = extractJSONArray(metadata_json, "creators")
    val rights = extractJSONArray(metadata_json, "rights")
    val types = extractJSONArray(metadata_json, "types")

    Metadata(title = title, creators = creators, rights = rights,
      types = types)
  }

  /**
   * Extracts a list of strings from a field in a JSONObject.
   */

  private def extractJSONArray(json: JSONObject, field: String) = {
    val buffer = new ListBuffer[String]
    val json_array = json.getJSONArray(field)
    for (index <- 0 until json_array.length)
      buffer.append(json_array.getString(index))
    buffer.toList
  }

  /**
   * Creates list of ImageSource objects for each image source specified
   * in the JSON data.
   */

  private def makeImageSources(json: JSONObject): List[ImageSource] = {
    val sources = new ListBuffer[ImageSource]
    val sources_array = json.getJSONArray("sources")
    for (index <- 0 until sources_array.length) {
      val source_json = sources_array.getJSONObject(index)
    }

    sources.toList
  }

  /**
   * Creates or replaces an item in the repository.
   */

  def put(path: String, item: Item) = {
    session((jcr_session: Session) => {
      if (!isItem(path)) {
        val location = Path(path)
        val parent = jcr_session.getNode(location.parent)
        createItem(parent, item)
      }
      else {
        val item_node = jcr_session.getNode(path)
        updateItem(item_node, item)
      }
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
   *     Catalogs content under a specified folder.
   */

  def catalog(path: String): Catalog = {
    session((jcr_session: Session) => {
      val container = jcr_session.getNode(path)
      var items: List[CatalogItem] = List()
      val iterator = container.getNodes
      while (iterator.hasNext())
        items ::= makeCatalogItem(iterator.nextNode)
      new edu.northwestern.art.content_core.catalog.Catalog(container.getName, "", items.reverse)
    })
  }

  /**
   *  Free text search of the repository. Returns a Folder of results.
   */

  def search(text: String): Catalog = {
    val results =
      session((session: Session) => {
        var results: List[CatalogItem] = List()
        val statement = "//element(*, nt:file)[jcr:contains(jcr:content, '" + text + "')]"
        val query = session.getWorkspace.getQueryManager.createQuery(statement, Query.XPATH);
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
      val contents = node.getNode("contents.json/jcr:content");
      new JSONObject(contents.getProperty("jcr:data").getString)
    }
    catch {
        case except: PathNotFoundException =>
          throw new NoItemException
        case except =>
          throw new FailureException(except)
    }
  }

  /**
   * Returns the modified time for the item by examining the lastModified
   * property on the content.json file.
   */

  private def getModified(node: Node): Date = {
    try {
      val contents = node.getNode("contents.json/jcr:content");
      contents.getProperty("jcr:lastModified").getDate.getTime
    }
    catch {
        case except: PathNotFoundException =>
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