/**
 * Copyright 2010 Jonathan A. Smith
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 * 
 * @author Jonathan A. Smith
 * @version 27 03 2010 5:40:38 AM
 */

package edu.northwestern.art.content_core.properties

import org.json.{JSONArray, JSONObject}

case class ArrayValue(value: Seq[PropertyValue]) extends PropertyValue {

  def setIn(json_object: JSONObject, name: String) {
    json_object.put(name, toJSONArray)
  }

  def addTo(json_array: JSONArray) {
    json_array.put(toJSONArray)
  }

  def toJSONArray = {
    val json_array = new JSONArray
    value.foreach(_.addTo(json_array))
    json_array
  }

  override def toString = toJSONArray.toString

}