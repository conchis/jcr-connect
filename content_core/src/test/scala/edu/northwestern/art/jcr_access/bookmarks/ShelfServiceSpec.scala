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
 * @version 11 October 2010
 */

package edu.northwestern.art.jcr_access.services

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import com.sun.jersey.api.client.Client
import com.sun.jersey.core.util.MultivaluedMapImpl

@RunWith(classOf[JUnitRunner])
class ShelfServiceSpec extends FlatSpec with ShouldMatchers {

  val serviceUrl = "http://localhost:8080/content/service/"
  val client = Client.create

  // Set up test data
  val res1 = client.resource(serviceUrl + "bookmark/jsm712")
  val par1 = new MultivaluedMapImpl
  par1.add("path", "test")
  val out1 = res1.queryParams(par1).get(classOf[String])
  println("setup:\n" + out1)


  "the bookmark service" should "do something" in {
    val resource = client.resource(serviceUrl + "bookmark")
    val result = resource.get(classOf[String])
    println(result)
  }
 

}