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
 * @version 11 [10] 2010
 */

package edu.northwestern.art.jcr_access.bookmarks

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.utilities.Storage.transaction

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import edu.northwestern.art.jcr_access.access.RepositoryConnector
import edu.northwestern.art.jcr_access.repositories.LocalConnector

@RunWith(classOf[JUnitRunner])
class BookmarkSpec extends FlatSpec with ShouldMatchers {
  Storage.unit("Testing")

  val repository_url = "http://localhost:8080/jackrabbit/rmi"
  RepositoryConnector.register("local", new LocalConnector(repository_url, "admin", "admin"))

  val user = User.create("jas2")

  "Bookmark" can "create a persistent Bookmark object" in {
    transaction {
      val b = Bookmark.create(user, "W1", "p1")
      b.user.id should equal(user.id)
      b.source should equal("W1")
      b.path should equal("p1")
    }
  }

  "Bookmark" can "find a Bookmark with a specified user, workspace, and path" in {
    transaction {
      val b1 = Bookmark.create(user, "W3", "p1")
      val b2 = Bookmark.create(user, "W4", "p2")

      val b1b = Bookmark.find(user, "W3", "p1").get
      b1.id should equal(b1b.id)
      b1b.path should equal("p1")

      val b2b = Bookmark.find(user, "W4", "p2").get
      b2.id should equal(b2b.id)
      b2b.path should equal("p2")

      val b3b = Bookmark.find(user, "W3", "x1")
      b3b should equal(None)
    }
  }

  "Bookmark" can "create a new Bookmark, or return an existing one" in {
    transaction {
      val b1a = Bookmark.findOrCreate(user, "W5", "p1")
      val b2a = Bookmark.findOrCreate(user, "W6", "p2")
      val b3a = Bookmark.findOrCreate(user, "W7", "p3")

      val b2b = Bookmark.findOrCreate(user, "W6", "p2")
      val b1b = Bookmark.findOrCreate(user, "W5", "p1")

      b1b.path should equal("p1")
      b1b.id should equal(b1a.id)

      b2b.path should equal("p2")
      b2b.id should equal(b2a.id)
    }
  }

  "A Bookmark" should "be able to provide a catalog for the item it refers to" in {
    val b = Bookmark.findOrCreate(user, "local", "content/ec_100014")
    val catalog = b.toCatalog

    println(catalog)
  }

}