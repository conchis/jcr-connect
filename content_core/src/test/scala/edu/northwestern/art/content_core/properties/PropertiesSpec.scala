/**
 * Copyright 2010 Jonathan A. Smith
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 * 
 * @author Jonathan A. Smith
 * @version 16 06 2010 12:24:39 PM
 */

package edu.northwestern.art.content_core.properties

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.util.Date

import org.json.{JSONException, JSONObject}

@RunWith(classOf[JUnitRunner])
class PropertiesSpec extends FlatSpec with ShouldMatchers {

  "A Properties object" should "store a map from names to values with types" in {
    val p = Properties("x" -> 1, "name" -> "hello", "check" -> true)

    val x: Int = p("x")
    x should equal(1)

    val name: String = p("name")
    name should equal("hello")

    val check: Boolean = p("check")
    check should be(true)
  }

  it should "support storage of Seq values" in {
    val p = Properties("a" -> List(2, 4, 6, 8))
    val a: Seq[PropertyValue] = p("a")
    (a(0): Int) should equal(2)
    (a(1): Int) should equal(4)
    (a(3): Int) should equal(8)
  }

  it should "support storage of map values" in {
    val p = Properties("m" -> Map("x" -> 1, "y" -> 2))
    val m: Map[String, PropertyValue] = p("m")
    (m("x"): Int) should equal(1)
    (m("y"): Int) should equal(2)
  }

  it should "have a toString method that returns a JSON rendering" in {
    val p1 = Properties("x" -> 2, "y" -> 4)
    p1.toString should equal("{\"y\":4,\"x\":2}")

    val p2 = Properties("i" -> 33, "b" -> true, "s" -> "string",
        "a" -> List(2, 4), "m" -> Map("x" -> 3, "y" -> 4))
    p2.toString should equal("{\"b\":true,\"s\":\"string\",\"a\":[2,4],\"m\":{\"y\":4,\"x\":3},\"i\":33}")
  }

  case class JPoint(val x: Int, val y: Int) extends JSONSerializable {
    def toJSON: JSONObject =
      Properties("class" -> "JPoint", "x" -> x, "y" -> y).toJSON
  }

  it should "support rendering of nested JSONSerializable objects" in {
    val p = Properties("points" -> List(
      JPoint(1, 2), JPoint(3, 4), JPoint(5, 0)
    ))
    p.toString should equal("""{"points":[{"class":"JPoint","y":2,"x":1},{"class":"JPoint","y":4,"x":3},{"class":"JPoint","y":0,"x":5}]}""")
  }

  "The Properties object" should "support contruction of a Properties intance from a JSON string" in {
    val p = Properties("""{"x": 11, "y": 33, "name":"no7", "check": true}""")
    (p("x"): Int) should equal(11)
    (p("y"): Int) should equal(33)
    (p("name"): String) should equal("no7")
    (p("check"): Boolean) should equal(true)
  }

  it should "Throw an exception when initialized with an incorrect JSON string" in {
    intercept[JSONException] {
      val p = Properties("{\"x\":")
    }
  }

}


