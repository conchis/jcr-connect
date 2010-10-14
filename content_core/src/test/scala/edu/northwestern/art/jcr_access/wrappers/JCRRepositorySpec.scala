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
import javax.jcr.{Node, SimpleCredentials}

@RunWith(classOf[JUnitRunner])
class JCRRepositorySpec extends FlatSpec with ShouldMatchers {

  val repository_url = "http://localhost:8080/jackrabbit/rmi"
  val repository = new JCRRepository(repository_url, "admin", "admin")

  "A JCRRepository" should "Provide access to a JCRSession" in {

    repository.session(jcr_session => {
      jcr_session.isInstanceOf[JCRSession]
      jcr_session.getRootNode.isInstanceOf[Node]
    })
    
  }




}
