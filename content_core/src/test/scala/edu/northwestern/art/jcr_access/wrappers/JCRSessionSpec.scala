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
import org.apache.jackrabbit.rmi.repository.URLRemoteRepository
import javax.jcr.SimpleCredentials

@RunWith(classOf[JUnitRunner])
class JCRSessionSpec extends FlatSpec with ShouldMatchers {

  val repository_url = "http://localhost:8080/jackrabbit/rmi"
  val repository = new JCRRepository(repository_url, "admin", "admin")

  "A JCRSession" should "provide access to the repository root node" in {
    repository.session((jcr_session) => {
      jcr_session.root.name should equal("")
    })
  }

  it should "provide access to any node by path" in {
    repository.session((jcr_session) => {
      val content = jcr_session \ "content"
      content.path should equal("/content")
    })
    
  }



  
}
