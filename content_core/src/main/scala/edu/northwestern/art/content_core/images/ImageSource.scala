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

import edu.northwestern.art.content_core.properties.JSONSerializable
import edu.northwestern.art.content_core.utilities.Storage
import javax.persistence._

@Entity
abstract class ImageSource extends JSONSerializable {

  @Id @GeneratedValue
  var id: Int = 0

  @ManyToOne
  var item: ImageItem = null

  var name:   String = null
  var format: String = null
  var width:  Int    = 0
  var height: Int    = 0
}

object ImageSource {

  def initialize(source: ImageSource, name: String, format: String,
      width: Int, height: Int) {
    source.name = name
    source.format = format
    source.width = width
    source.height = height
    source
  }
}
