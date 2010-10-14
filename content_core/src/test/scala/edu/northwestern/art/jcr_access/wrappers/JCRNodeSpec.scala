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
 * @version 03 [09] 2010
 */

package edu.northwestern.art.jcr_access.wrappers

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec

import JCRNode._

@RunWith(classOf[JUnitRunner])
class JCRNodeSpec extends FlatSpec with ShouldMatchers {

  val repository_url = "http://localhost:8080/jackrabbit/rmi"
  val repository = new JCRRepository(repository_url, "admin", "admin")

  "A JCRNode" can "find a decendent node by path with the / operator" in {
    repository.session((jcr_session) => {
      val root = jcr_session.root
      val content = root \ "content"
      content.path should equal("/content")
      val ec_11601 = content \ "ec_11601"
      ec_11601.path should equal("/content/ec_11601")
      val ec_11601b = root \ "content/ec_11601"
      ec_11601b.path should equal(ec_11601.path)
    })
  }

  it can "find child nodes" in {
    repository.session((jcr_session) => {
      val content = jcr_session \ "content/ec_11601"
      val listing = (content.children map (_.name)).mkString("\n")
      listing should include("contents.json")
      listing should include("document.xml")
      listing should include("entry.xml")
      listing should include("thumbnail.jpg")
    })
  }
}