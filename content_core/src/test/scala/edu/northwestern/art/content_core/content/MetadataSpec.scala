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
 * @version 28 [07] 2010
 */

package edu.northwestern.art.content_core.content

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class MetadataSpec extends FlatSpec with ShouldMatchers  {

  "A Metadata" should "Store basic information about each Item" in {

    val md = Metadata("T", "D", creators = Array("C1", "C2"),
      rights = Array("R1"), types = Array("T1", "T2", "T3"))
    println(md)

    md.title should equal("T")
    md.description should equal("D")
    md.creators should equal(Array("C1", "C2"))
    md.rights should equal(Array("R1"))
    md.types should equal(Array("T1", "T2", "T3"))
  }

  it should "provide a method for conversion to JSON" in {
    val md = Metadata("Tb", "Db", creators = Array("C1b", "C2b"),
      rights = Array("R1b"), types = Array("T1b", "T2b", "T3b"))
    val json = md.toJSON.toString
    json should include("\"description\":\"Db\"")
    json should include("\"title\":\"Tb\"")
    json should include("\"rights\":[\"R1b\"]")
    json should include(""""types":["T1b","T2b","T3b"]""")
    json should include(""""creators":["C1b","C2b"]""")
  }

}