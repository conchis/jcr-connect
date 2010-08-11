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
 * @version 11 August 2010
 */

package edu.northwestern.art.content_core.content2

import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import edu.northwestern.art.content_core.content2.ImageURL

@RunWith(classOf[JUnitRunner])
class ImageItemSpec extends FlatSpec with ShouldMatchers  {

  "An ImageURL" should "provide a JSON representation" in {

    val source = new ImageURL("I1", "http://i1.jpg")
    val json_string = source.toJSON.toString
    json_string should include("\"name\":\"I1\"")
    json_string should include("\"url\":\"http://i1.jpg\"")
  }

  "A TimedImage" should "provide a JSON represenation" in {

    val source = new TiledImage("I2", "http://i2.jpg")
    val json_string = source.toJSON.toString
    json_string should include("\"name\":\"I2\"")
    json_string should include("\"url\":\"http://i2.jpg\"")
  }

}