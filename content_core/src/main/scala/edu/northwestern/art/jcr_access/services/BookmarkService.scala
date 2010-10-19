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
 * @version 11 October 2010
 */

package edu.northwestern.art.jcr_access.services

import javax.ws.rs._
import edu.northwestern.art.jcr_access.bookmarks.{Bookmark, User}
import edu.northwestern.art.content_core.utilities.Storage.transaction

@Path("/bookmark")
class BookmarkService {

  @GET @Path("/")
  @Produces(Array("application/json"))
  def get() = {
    "Bookmarks"
  }

  @GET @Path("/{user}")
  @Produces(Array("application/json"))
  def getCategories(@PathParam("user") user_name: String,
          @QueryParam("ws") workspace: String,
          @QueryParam("path") path: String,
          @QueryParam("cat") category_string: String) = {

    var result = ""
    transaction {
      val user = User.findOrCreate(user_name)
      println("Found user: " + user.toString)
      if (path == null)
        result = user.toJSON.toString
      else if (path == "test")
        result = createTestData(user)
      else if (category_string == null) {
        user.findBookmark(workspace, path) match {
          case Some(bookmark) => result = bookmark.toJSON.toString
          case None => result = "null"
        }
      }
      else {
        val names = category_string.split(";")
        val bookmark = user.addBookmark(workspace, path, names)
        result = bookmark.toJSON.toString
      }
    }
    result
  }

  def createTestData(user: User) = {

    // Local repository
    user.addBookmark("local", "content/ec_100014", Array("Category 1"))
    user.addBookmark("local", "content/ec_100019", Array("Category 1"))
    user.addBookmark("local", "content/ec_10012",  Array("Category 2"))
    user.addBookmark("local", "content/ec_101149", Array("Category 2"))
    user.addBookmark("local", "content/ec_101492", Array("Category 3"))
    user.addBookmark("local", "content/ec_10699",  Array("Category 3"))
    user.addBookmark("local", "content/ec_10717",  Array("Category 1", "Category 2"))
    user.addBookmark("local", "content/ec_11105",  Array("Category 2", "Category 3"))
    user.addBookmark("local", "content/ec_11126",  Array("Category 1", "Category 3"))
    user.addBookmark("local", "content/ec_1772",   Array("Category 1", "Category 2", "Category 3"))

    // Fedora
    user.addBookmark("fedora", "ec_100014", Array("Category F1"))
    user.addBookmark("fedora", "ec_100019", Array("Category F1"))
    user.addBookmark("fedora", "ec_10012",  Array("Category F2"))
    user.addBookmark("fedora", "ec_101149", Array("Category F2"))
    user.addBookmark("fedora", "ec_101492", Array("Category F3"))
    user.addBookmark("fedora", "ec_10699",  Array("Category F3"))
    user.addBookmark("fedora", "ec_10717",  Array("Category F1", "Category F2"))
    user.addBookmark("fedora", "ec_11105",  Array("Category F2", "Category F3"))
    user.addBookmark("fedora", "ec_11126",  Array("Category F1", "Category F3"))
    user.addBookmark("fedora", "ec_1772",   Array("Category F1", "Category F2", "Category F3"))

    // XTF
    user.addBookmark("xtf", "/cdl/13030/3g/tf9489p03g", Array("Category X1"))
    user.addBookmark("xtf", "/cdl/13030/jg/tf0v19n4jg", Array("Category X1"))
    user.addBookmark("xtf", "/cdl/13030/9k/tf3000029k", Array("Category X1"))
    user.addBookmark("xtf", "/cdl/13030/12/tf0p300112", Array("Category X1"))
    user.addBookmark("xtf", "/cdl/13030/39/kt6489n839", Array("Category X1"))

    user.toJSON.toString
  }

}

