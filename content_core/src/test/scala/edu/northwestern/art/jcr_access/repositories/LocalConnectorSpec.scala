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

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import javax.jcr.Session
import edu.northwestern.art.content_core.images.ImageItem
import edu.northwestern.art.content_core.content.Metadata
import java.util.Date

@RunWith(classOf[JUnitRunner])
class LocalConnectorSpec extends FlatSpec with ShouldMatchers {

  val repository_url = "http://localhost:8080/jackrabbit/rmi"
  val user = "admin"
  val pass = "admin"

  val connector = new LocalConnector(repository_url, user, pass)

  "A LocalConnector" should "provide a method for executing code in a session" in {
    connector.session((s: Session) => {
      val content = s.getNode("/content")
      assert(content != null)
    })
  }

  it should "Allow session blocks to be nested without creating a second JCR session" in {
    connector.session((s1: Session) => {
      connector.session((s2: Session) => {
        s1 should equal(s2)
      })
    })
  }

  it should "Provide a method to determine if a specified path corresponds to an Item node" in {
    assert(!connector.isItem("/content/foo"))
    assert(connector.isItem("/content/ec_100647"))
  }

  it should "Support free-text search of a repository" in {
    val folder = connector.search("Water").toString
    folder should include("Photographer: Chicago Daily News")
    folder should include("Old 68th Street Water Intake Crib")
    folder should include("Commercial Fishermen along the Chicago River, 1911")
  }

  it should "provide a method to retrieve an item given a path" in {
    val item = connector.get("/content/ec_100647")
    println(item)
  }

  it should "create new nodes from Item instances" in {
    val now = new Date
    val item = ImageItem("abc", Metadata("T1", "T1 Item"), now)
    connector.put("/content/test1", item)

    val item2 = connector.get("/content/test1")
    item2.name should equal(item.name)
    item2.metadata.title should equal(item.metadata.title)
    println(item2)
  }

  //it should "Provide a way to generate a catalog of any content folder" in {
  //  val folder = connector.catalog("/content")
  //  println(folder)
  //}

}