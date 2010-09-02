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

import java.awt.image.BufferedImage
import javax.persistence.{Lob, Entity}
import edu.northwestern.art.content_core.properties.Properties
import edu.northwestern.art.content_core.utilities.Storage

@Entity
class BinaryImage extends ImageSource {

  @Lob
  var image: BufferedImage = null

  def toJSON = Properties("name" -> name, "format" -> format,
                          "width" -> width, "height" -> height,
                          "type" -> "BinaryImage").toJSON
}

// FIX ME !!! - Added by Xin (2010-09-01)
object BinaryImage extends Storage[BinaryImage] {

  def initialize(source: BinaryImage, name: String, format: String,
      width: Int, height: Int, image: BufferedImage): BinaryImage = {
    ImageSource.initialize(source, name, format, width, height)
    source.image = image
    source
  }

  def create(name: String, image: BufferedImage, format: String = null,
      width: Int = 0, height: Int = 0): BinaryImage = {
    val source = new BinaryImage
    initialize(source, name, format, width, height, image)
    persist(source)
    source
  }

  def apply(name: String, image: BufferedImage, format: String = null,
      width: Int = 0, height: Int = 0): BinaryImage = {
    val source = new BinaryImage
    initialize(source, name, format, width, height, image)
    source
  }

}
