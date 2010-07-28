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
 * @version 14 July 2010
 */

package edu.northwestern.art.content_core.content

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.utilities.Storage.transaction

@RunWith(classOf[JUnitRunner])
class CategorySpec extends FlatSpec with ShouldMatchers {
  Storage.unit("Testing")

  "The Category companion object" should "provide a method to create new trees of nodes" in {
    var n: Category = null
    transaction {
      n = Category("C1",
            Category("C2"), Category("C3"))
    }
    n.name should equal("C1")
    val children = n.children
    children.size should equal(2)
    children(0).name should equal("C2")
    children(1).name should equal("C3")
  }

  "A Category" should "track its index in the parent node" in {
    var n: Category = null
    transaction {
      n = Category("C4",
            Category("C5"),
            Category("C6"))
    }
    val children = n.children
    children.size should equal(2)
    children(0).index should equal(0)
    children(1).index should equal(1)
  }

  "A Category" should "provide information on the number of child categories" in {
    var n: Category = null
    transaction {
      n = Category("C4",
            Category("C5",
              Category("C5.1")),
            Category("C6"),
            Category("C7"))
    }

    n.count should equal(3)
    n.get("C5").get.count should equal(1)
    n.get("C7").get.count should equal(0)
  }

  "A Category" should "provide access to child nodes by name" in {
    var n: Category = null;
    transaction {
      n = Category("C8",
            Category("C9" ),
            Category("C10"),
            Category("C11"))
    }
    val c10 = n.get("C10")
    c10.get.name should equal("C10")
    val c11 = n.get("C11")
    c11.get.name should equal("C11")
    val c9 = n.get("C9")
    c9.get.name should equal("C9")

    n.get("X43") should equal(None)
    c9.get.get("X44") should equal(None)
  }

  it should "Support allow an existing category tree to be moved" in {
    var n: Category = null;
    transaction {
      n = Category("N1",
            Category("N2",
              Category("N3",
                Category("N4"))))
    }
    transaction {
      val n3 = n.get("N2").get.get("N3")
      n3 should not equal(None)
      n.add(n3.get)
    }
    val n2b = n.get("N3")
    n2b should not equal(None)
    n2b.get.get("N4") should not equal(None)
    n.get("N2").get.get("N3") should equal(None)
  }

  it should "support removing a category tree" in {
    var n1: Category = null
    var n2: Category = null
    var n3: Category = null
    var n4: Category = null
    transaction {
      n1 = Category("N1")
      n2 = Category("N2")
      n3 = Category("N3")
      n4 = Category("N4")

      n2.add(n4)
      n2.add(n3)
      n1.add(n2)
    }
    transaction {
      n2.remove
      Category.find(n2.id) should equal(None)
      Category.find(n3.id) should equal(None)
      Category.find(n2.id) should equal(None)
      n1.count should equal(0)
    }
  }

  it should "renumber child indexes when a child is removed" in {
    var n1: Category = null
    var n2: Category = null
    var n3: Category = null
    var n4: Category = null
    var n5: Category = null
    transaction {
      n1 = Category("N1")
      n2 = Category("N2")
      n3 = Category("N3")
      n4 = Category("N4")
      n5 = Category("N5")

      n1.add(n2)
      n1.add(n3)
      n1.add(n4)
      n1.add(n5)
    }
    transaction { n3.remove }
    Category.find(n2.id).get.index should equal(0)
    Category.find(n4.id).get.index should equal(1)
    Category.find(n5.id).get.index should equal(2)

    transaction { n2.remove }
    Category.find(n4.id).get.index should equal(0)
    Category.find(n5.id).get.index should equal(1)

    transaction { n5.remove }
    Category.find(n4.id).get.index should equal(0)
  }

  it should "Track all Items that are in the category" in {
    transaction {
      val c1 = Category("XC1")
      val c2 = Category("XC2")
      val c3 = Category("XC3")
      val i1 = Item("XI1", "XS1")
      val i2 = Item("XI2", "XS2")
      val i3 = Item("XI3", "XS3")
      c1.addItem(i1)
      c1.addItem(i2)

      c2.addItem(i2)
      c2.addItem(i3)

      c3.addItem(i1)
      c3.addItem(i3)

      assert(c1.containsItem(i1))
      assert(c1.containsItem(i2))
      assert(!c1.containsItem(i3))

      assert(!c2.containsItem(i1))
      assert(c2.containsItem(i2))
      assert(c2.containsItem(i3))

      assert(c3.containsItem(i1))
      assert(!c3.containsItem(i2))
      assert(c3.containsItem(i3))
    }
  }

  it should "Provide a method for removing an item from this category" in {
    transaction {
      val c1 = Category("YC1")
      val i1 = Item("YI1", "YS1")
      val i2 = Item("YI2", "YS2")
      c1.addItem(i1)
      c1.addItem(i2)

      assert(c1.containsItem(i1))
      assert(c1.containsItem(i2))

      c1.removeItem(i1)
      assert(!c1.containsItem(i1))
      assert(c1.containsItem(i2))

      c1.removeItem(i2)
      assert(!c1.containsItem(i1))
      assert(!c1.containsItem(i2))

      i1.categories.isEmpty
      i2.categories.isEmpty
    }

  }

  "The Category companion object" should "Support finding a category by categoryId" in {
    transaction {
      val t1 = Taxonomy("VT1")

      val c1 = t1.add(Category("VC1"))
      val c2 = t1.add(Category("VC2"))

      Category.find("VT1:VC1") should equal(c1)
      Category.find("VT1:VC2") should equal(c2)

      // TODO test invalid id
    }

  }


}