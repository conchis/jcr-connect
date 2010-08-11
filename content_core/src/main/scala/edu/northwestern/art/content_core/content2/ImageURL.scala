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
 * @version 10 August 2010
 */

package edu.northwestern.art.content_core.content2

import org.json.JSONObject

class ImageURL(
    name:    String,
    val url: String,
    format:  String = "",
    width:   Int    = 0,
    height:  Int    = 0
  )
  extends ImageSource(name, format, width, height) {

  /**
   * Applies a visitor to this object.
   */

  override def accept[T](visitor: ContentVisitor[T]): T =
    visitor.visitImageURL(this)

  /**
   * Returns a JSON representation of this object.
   */

  override def toJSON: JSONObject = accept(JSONVisitor)

}