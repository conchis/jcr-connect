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

  it should "Allow session blocks to be nested without creating a second session" in {
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

}