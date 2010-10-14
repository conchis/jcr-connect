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
 * @author Jonathan A. Smith
 * @version 29 July 2010
 */

package edu.northwestern.art.jcr_access.services

import java.io.{OutputStreamWriter, OutputStream}

import edu.northwestern.art.jcr_access.repositories.LocalConnector
import edu.northwestern.art.jcr_access.repositories.FedoraConnector
import edu.northwestern.art.jcr_access.repositories.XTFConnector
import javax.ws.rs.core.{StreamingOutput, Response}
import javax.ws.rs._
import edu.northwestern.art.jcr_access.access.{FailureException, NoItemException}
import javax.ws.rs.core.Context
import javax.servlet.ServletConfig
import edu.northwestern.art.content_core.utilities.Storage.transaction
import scala.collection.JavaConversions._
import collection.mutable.ArrayBuffer
import edu.northwestern.art.content_core.properties.Properties
import edu.northwestern.art.jcr_access.bookmarks.{Category, User}
import edu.northwestern.art.content_core.content.Item


@Path("/shelf")
class ShelfService {

  @Context
  val config: ServletConfig = null

  @GET @Path("/{user}")
  @Produces(Array("application/json"))
  def getShelf(@PathParam("user") user_name: String): String = {
    transaction {
      val user = User.findOrCreate(user_name)
      user.toJSON.toString
    }
  }

  @GET @Path("/{user}/{category_id}")
  @Produces(Array("application/json"))
  def getCategory(@PathParam("user") user_name: String,
      @PathParam("category_id") category_id: Int): String = {

    transaction {
      // Find user object
      val user = User.findOrCreate(user_name)

      // Get category, insure that it belongs to the named user
      Category.find(category_id) match {
        case Some(category) =>
          if (category.user.id != user.id)
            throw new WebApplicationException(Response.Status.FORBIDDEN)
          category.toJSON.toString
        case None =>
          throw new WebApplicationException(Response.Status.NOT_FOUND)
      }
    }
  }

  @GET @Path("/{user}/{category_id}/{item_name}")
  @Produces(Array("application/json"))
  def getItem(@PathParam("user") user_name: String, 
      @PathParam("category_id") category_id: Int): String = {
    "item"
  }

  private def getConnector(workspace: String) = {

    val user = config.getInitParameter("username")
    val pass = config.getInitParameter("password")
    val repository_url = config.getInitParameter("repositoryurl")

    workspace match {
      case "fedora" => new FedoraConnector(repository_url, user, pass)
      case "xtf"    => new XTFConnector(repository_url, user, pass)
      case _        => new LocalConnector(repository_url, user, pass)
    }
  }

  private def findBookmarkedItems(user: User) = {
    val items = new ArrayBuffer[Item]
    for (category <- user.categories)
      for (bookmark <- category.bookmarks)
        items += findItem(bookmark.workspace, bookmark.path)
    items.toArray
  }

  private def findCategoryItems(category: Category): Array[Item] = {
    val items = new ArrayBuffer[Item]
    for (bookmark <- category.bookmarks)
      items += findItem(bookmark.workspace, bookmark.path)
    items.toArray
  }

  private def findItem(workspace: String, path: String): Item = {
    val connector = getConnector(workspace)
    val repository_path =
      if (path.startsWith("/"))
        path
      else
        "/" + path
    connector.get(repository_path).asInstanceOf[Item]
  }

}
