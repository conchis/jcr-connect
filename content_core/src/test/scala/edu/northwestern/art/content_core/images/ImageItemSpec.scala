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
 * @version 31 [07] 2010
 */

package edu.northwestern.art.content_core.images

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.utilities.Storage.transaction

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import edu.northwestern.art.content_core.content.Metadata
import java.util.Date

@RunWith(classOf[JUnitRunner])
class ImageItemSpec extends FlatSpec with ShouldMatchers {
  Storage.unit("Testing")

  "An ImageItem" should "do something" in {
    val item = ImageItem.create("IM1", Metadata("Test Image"),
      modified = new Date,
      categories = List(),
      sources = Map(
        "thumbnail" -> ImageURL.create("u1", "http://u1"),
        "large" -> TiledImageURL.create("u2", "http://u2")))
    println(item)
  }

}