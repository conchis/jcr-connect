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
 * @version 27 03 2010 5:39:54 AM
 */

package edu.northwestern.art.content_core.properties

import org.json.{JSONArray, JSONObject}
import collection.immutable.MapLike
import java.lang.String

object Properties {

  def apply(pairs: Tuple2[_, _]*): Properties =
    PropertyValue.asPropertyValue(Map(pairs: _*))

  def apply(map: Map[String, Any]): Properties =
    PropertyValue.asPropertyValue(map)

  def apply(json_object: JSONObject): Properties =
    PropertyValue.asPropertyValue(json_object)

  def apply(json_string: String): Properties =
    apply(new JSONObject(json_string))

  type Builder[T] = (Properties) => T

  private val builder_map =
      new collection.mutable.HashMap[String, Builder[Any]]

  def defBuilder[T](name: String, builder: Builder[T]) {
    builder_map(name) = builder
  }
  
  def getBuilder[T](name: String): Builder[T] =
    builder_map(name).asInstanceOf[Builder[T]]

}

class Properties(val property_map: Map[String, PropertyValue])
        extends PropertyValue
                with Map[String, PropertyValue]
                with JSONSerializable {

  def get(key: String): Option[PropertyValue] =
    property_map.get(key)

  def iterator: Iterator[(String, PropertyValue)] =
    property_map.iterator

  def +[B1 >: PropertyValue](kv: (String, B1)) =
    new Properties(property_map + kv)

  def - (key: String): Properties =
    new Properties(property_map - key)

  override def empty: Properties = new Properties(Map())

  override def size = property_map.size

  override def foreach[U](f: ((String, PropertyValue)) => U) =
    property_map.foreach(f)

  def setIn(json_object: JSONObject, name: String) {
    json_object.put(name, toJSON)
  }

  def addTo(json_array: JSONArray) {
    json_array.put(toJSON)
  }

  implicit def toJSON: JSONObject = {
    val json_object = new JSONObject
    for ((name, value) <- property_map)
      value.setIn(json_object, name)
    json_object
  }

  def as[T]: T = {
    val class_name: String = this("class")
    val builder = Properties.getBuilder(class_name)
    builder(this)
  }
}

