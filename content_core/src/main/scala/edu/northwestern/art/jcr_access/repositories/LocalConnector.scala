/** 
 *Copyright 2010 Northwestern University.
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
 * @version 29 [07] 2010
 */

package edu.northwestern.art.jcr_access.repositories

import edu.northwestern.art.jcr_access.access.RepositoryConnector
import edu.northwestern.art.content_core.content.Item
import javax.jcr.query.Query
import edu.northwestern.art.content_core.catalog

import catalog.{CatalogImageItem, Thumbnail, Catalog, CatalogItem}
import org.apache.jackrabbit.rmi.repository.URLRemoteRepository
import org.json.JSONObject
import javax.jcr.{Node, PathNotFoundException, Session}
import edu.northwestern.art.content_core.images.ImageItem

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
        getItemJSON(jcr_session.getNode(path))
        true
      }
      catch {
        case _: PathNotFoundException =>
          false
      }
    })
  }

  def get(path: String): Item = {
    null    
  }

  def put(path: String, item: Item) = null

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
   * Free text search of the repository. Returns a Folder of results.
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
          results ::= makeCatalogItem(node.getParent)
        }
        results
      })
    new edu.northwestern.art.content_core.catalog.Catalog(
      "content", "Search Results", results.reverse)
  }

  private def getItemJSON(node: Node): JSONObject = {
    val contents = node.getNode("contents.json/jcr:content");
    new JSONObject(contents.getProperty("jcr:data").getString)
  }

  private def makeCatalogItem(node: Node): CatalogItem = {
    val content = getContentJSON(node)
    val name: String = node.getName
    val metadata = content.getJSONObject("metadata")
    val creators = getCreators(metadata)
    val title: String = metadata.getString("title")
    val modified = node.getProperty("jcr:created").getDate.getTime
    new CatalogImageItem(
      name, title, creators, getCatalogThumb(name, content), modified)
  }

  private def getContentJSON(node: Node) = {
    val contents = node.getNode("contents.json/jcr:content");
    new JSONObject(contents.getProperty("jcr:data").getString)
  }

  private def getCreators(metadata: JSONObject) = {
    val creators_array = metadata.getJSONArray("creators")
    var creators: List[String] = List()
    for (index <- 0 until creators_array.length)
      creators ::= creators_array.getString(index)
    creators.reverse
  }

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