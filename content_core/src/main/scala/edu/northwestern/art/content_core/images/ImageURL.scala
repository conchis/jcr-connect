/**
 *  Copyright 2010 Northwestern University.
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
 * @version 14 July 2010
 */

package edu.northwestern.art.content_core.images

import edu.northwestern.art.content_core.properties.Properties
import javax.persistence.{Inheritance, InheritanceType, Entity}
import edu.northwestern.art.content_core.utilities.Storage

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
class ImageURL extends ImageSource {

  var url: String = ""

  def toJSON = Properties("name" -> name, "format" -> format,
      "width" -> width, "height" -> height, "url" -> url).toJSON
}

object ImageURL extends Storage[ImageURL] {

  def initialize(source: ImageURL, name: String, format: String,
      width: Int, height: Int, url: String): ImageURL = {
    ImageSource.initialize(source, name, format, width, height)
    source.url = url
    source
  }

  def create(name: String, url: String, format: String = null,
      width: Int = 0, height: Int = 0): ImageURL = {
    val source = new ImageURL
    initialize(source, name, format, width, height, url)
    source
  }

}