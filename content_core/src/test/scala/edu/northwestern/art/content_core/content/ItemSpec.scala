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
 * @version 20 July 2010
 */

package edu.northwestern.art.content_core.content

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.utilities.Storage.transaction

@RunWith(classOf[JUnitRunner])
class ItemSpec extends FlatSpec with ShouldMatchers {
  Storage.unit("Testing")

  "An Item" should "provide an Array of catgories that include the item" in {

    transaction {
      val t1 = Taxonomy.create("XT1")
      val c1 = t1.add(Category.create("XC1"))
      val c2 = t1.add(Category.create("XC2"))
      val c3 = t1.add(Category.create("XC3"))

      val item = Item.create("I1")
      c1.addItem(item)
      c2.addItem(item)
      c3.addItem(item)

      item.categories.map(_.categoryId) should equal(ArrayBuffer("XT1:XC1", "XT1:XC2", "XT1:XC3"))

      item.categoryIds should equal(ArrayBuffer("XT1:XC1", "XT1:XC2", "XT1:XC3"))
    }
  }

  it should "provide methods for adding and testing if the item is in a specified category" in {

    transaction {
      val t1 = Taxonomy.create("AT1")
      val c1 = t1.add(Category.create("AC1"))
      val c2 = t1.add(Category.create("AC2"))

      val item = Item.create("I1")
      item.addTo(c1)
  
      assert(item.inCategory(c1))
      assert(!item.inCategory(c2))

      item.addTo("AT1:AC2")
      assert(item.inCategory(c2))
      assert(item.inCategory(c1))

      item.removeFrom(c1)
      assert(!item.inCategory(c1))
      assert(item.inCategory(c2))

      item.removeFrom("AT1:AC2")
      assert(!item.inCategory(c1))
      assert(!item.inCategory(c2))
    }
  }

  it should "allow a client to replace all categories" in {
    transaction {
      val t1 = Taxonomy.create("BT1")
      val c1 = t1.add(Category.create("BC1"))
      val c2 = t1.add(Category.create("BC2"))
      val c3 = t1.add(Category.create("BC3"))
      val c4 = t1.add(Category.create("BC4"))

      val item = Item.create("IB1")
      item.addTo(c1)
      item.addTo(c2)

      item.categoryIds should equal(ArrayBuffer("BT1:BC1", "BT1:BC2"))

      println("START in cats " + item.categoryIds)
      item.categoryIds = Array("BT1:BC3", "BT1:BC4")

      println("cat ids: " + item.categoryIds)

      item.categoryIds should equal(ArrayBuffer("BT1:BC3", "BT1:BC4"))
    }
  }

  it should "track metadata" in {
    var id = 0

    transaction {
      val md = Metadata("Tc", "Dc", creators = Array("C1c", "C2c"),
        rights = Array("R1c"), types = Array("T1c", "T2c", "T3c"))
      val item = Item.create("T1", metadata=md)
      id = item.id
    }

    val item2 = Item.find(id).get
    val md2 = item2.metadata
    md2.title should equal("Tc")
    md2.description should equal("Dc")
    md2.creators should equal(Array("C1c", "C2c"))
    md2.rights should equal(Array("R1c"))
    md2.types should equal(Array("T1c", "T2c", "T3c"))
  }

}