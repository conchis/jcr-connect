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
 * @version 11 [08] 2010
 */

package edu.northwestern.art.content_core.content2

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec

@RunWith(classOf[JUnitRunner])
class MetadataSpec extends FlatSpec with ShouldMatchers  {

  "A Metadata" should "Provide a method for generating JSON" in {
    val m1 = new Metadata("M1", description = "Test Mete")
    val json_string = m1.toString
    json_string should include("\"description\":\"Test Mete\"")
    json_string should include("\"title\":\"M1\"")
    json_string should include("\"creators\":[]")
  }

}