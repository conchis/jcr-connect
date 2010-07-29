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
 * @version 26 July 2010
 */

package edu.northwestern.art.content_core.services

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.content.Taxonomy

import com.sun.jersey.api.client._

import com.sun.jersey.core.util.MultivaluedMapImpl

@RunWith(classOf[JUnitRunner])
class TaxonomyServiceSpec extends FlatSpec with ShouldMatchers {

  val serviceUrl = "http://localhost:8080/content_core/taxon"
  val client = Client.create


  "A TaxonomyService" should " allow the definiton of new taxonomies" in {
    val resource = client.resource(serviceUrl)

    val parameters = new MultivaluedMapImpl
    parameters.add("name", "T1")
    val result = resource.queryParams(parameters).post(classOf[String])

    result should include("edu.northwestern.art.content_core.content.Taxonomy")
    result should include("\"name\":\"T1\"")
  }

  it should "provide access to JSON representing existing taxonomies" in {
    val resource = client.resource(serviceUrl + "/T1")
    val result = resource.get(classOf[String])
    println("r2: " + result)
    result should include("edu.northwestern.art.content_core.content.Taxonomy")
    result should include("\"name\":\"T1\"")
  }
  
  it should "support the addition of new categories" in {
    //val result = service.addCategory("T1", "C1", "", "")
    val resource = client.resource(serviceUrl + "/T1")

    val parameters = new MultivaluedMapImpl
    parameters.add("name", "C1")
    val result = resource.queryParams(parameters).post(classOf[String])


    result should include("edu.northwestern.art.content_core.content.Category")
    result should include("\"name\":\"C1\"")

    //val taxon = Taxonomy.find("T1")
    //val c1 = taxon.category("C1")
    //c1.name should equal("C1")
  }

  it should "provide JSON for a specific category" in {
    //val result = service.getCategory("T1", "C1")
    //result should include("edu.northwestern.art.content_core.content.Category")
    //result should include("\"name\":\"C1\"")
  }

}