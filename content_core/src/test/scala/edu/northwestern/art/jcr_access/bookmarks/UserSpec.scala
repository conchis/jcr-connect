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

import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.utilities.Storage.transaction

@RunWith(classOf[JUnitRunner])
class UserSpec extends FlatSpec with ShouldMatchers {
  Storage.unit("Testing")

  "User" can "create a persistent User object" in {
    transaction {
      val user = User.create("jas")
      user.name should equal("jas")
    }
  }

  "User" can "find a user with a specified name" in {
    transaction {
      val u1 = User.create("abc")
      val u2 = User.create("bcd")
      
      val u1b = User.find("abc").get
      u1.id should equal(u1b.id)
      u1b.name should equal("abc")

      val u2b = User.find("bcd").get
      u2.id should equal(u2b.id)
      u2b.name should equal("bcd")

      val u3b = User.find("cde")
      u3b should equal(None)
    }
  }

  "User" can "create a new user, or return an existing one" in {
    transaction {
      val u1a = User.findOrCreate("U1")
      val u2a = User.findOrCreate("U2")
      val u3a = User.findOrCreate("U3")

      val u2b = User.findOrCreate("U2")
      val u1b = User.findOrCreate("U1")

      u1b.name should equal("U1")
      u1b.id should equal(u1a.id)

      u2b.name should equal("U2")
      u2b.id should equal(u2a.id)
    }
  }

  "A User" can "bookmark a path" in {
    val u1 = User.findOrCreate("jas")
    u1.addBookmark("ws", "p1", Array("C1", "C2", "C3"))
    println(u1.bookmarks)
  }

  "A User" can "Replace a bookmark" in {
    val u1 = User.findOrCreate("user1")
    u1.addBookmark("ws", "p1", Array("C4", "C5"))
    println(u1.categories)
  }

  "A User" can "Provide a catalog of all categories" in {
    val u1 = User.findOrCreate("U5")
    u1.addBookmark("ws", "p1", Array("C1", "C2", "C3", "C4"))
    val catalog = u1.toCatalog
    catalog.name should equal("U5")
    val children = catalog.children
    children.length should equal(4)
    val c0 = children(0)
    c0.name should equal("C" + u1.categories.get(0).id)
    c0.title should equal("C1")
    val c1 = children(1)
    c1.name should equal("C" + u1.categories.get(1).id)
    c1.title should equal("C2")

    println(catalog.toString)
  }
  
}