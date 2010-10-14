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

package edu.northwestern.art.jcr_access.bookmarks

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.utilities.Storage.transaction

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class CategorySpec extends FlatSpec with ShouldMatchers {
  Storage.unit("Testing")

  val user = User.create("jas2")

  "Category" can "create a persistent Category object" in {
    transaction {
      val c = Category.create(user, "C1")
      c.user.id should equal(user.id)
      c.name should equal("C1")
    }
  }

  "Category" can "find a Category with a specified user and name" in {
    transaction {
      val c1 = Category.create(user, "C3")
      val c2 = Category.create(user, "C4")

      val c1b = Category.find(user, "C3").get
      c1.id should equal(c1b.id)
      c1b.name should equal("C3")

      val c2b = Category.find(user, "C4").get
      c2.id should equal(c2b.id)
      c2b.name should equal("C4")

      val c3b = Category.find(user, "C8")
      c3b should equal(None)
    }
  }

  "Category" can "create a new Category, or return an existing one" in {
    transaction {
      val c1a = Category.findOrCreate(user, "C5")
      val c2a = Category.findOrCreate(user, "C6")
      val c3a = Category.findOrCreate(user, "C7")

      val c2b = Category.findOrCreate(user, "C6")
      val c1b = Category.findOrCreate(user, "C5")

      c1b.name should equal("C5")
      c1b.id should equal(c1a.id)

      c2b.name should equal("C6")
      c2b.id should equal(c2a.id)
    }
  }

}