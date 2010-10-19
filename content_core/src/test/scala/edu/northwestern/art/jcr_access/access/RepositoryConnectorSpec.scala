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

package edu.northwestern.art.jcr_access.access

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import edu.northwestern.art.jcr_access.repositories.LocalConnector

@RunWith(classOf[JUnitRunner])
class RepositoryConnectorSpec extends FlatSpec with ShouldMatchers {

  val repository_url = "http://localhost:8080/jackrabbit/rmi"
  val user = "admin"
  val pass = "admin"

  "RepositoryConnector" should "Manage a pool of connector objects by source" in {
    RepositoryConnector.register("local", new LocalConnector(repository_url, user, pass))

    val c1 = RepositoryConnector.forSource("local")
    val c2 = RepositoryConnector.forSource("local")
    c1 should equal(c2)
  }
  
}