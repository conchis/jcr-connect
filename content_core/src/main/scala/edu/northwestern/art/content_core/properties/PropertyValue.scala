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
 * @version 27 03 2010 5:01:50 AM
 */

package edu.northwestern.art.content_core.properties

import scala.collection.JavaConversions._
import org.json.{JSONArray, JSONObject}
import java.util.Date

/**
 * Wrappers used to support type safety via implicit conversion when getting
 * property values.
 */

object PropertyValue {

  implicit def asPropertyValue(value: Int) =
    new IntegerValue(value)

  implicit def asPropertyValue(value: Boolean) =
    new BooleanValue(value)

  implicit def asPropertyValue(value: String) =
    new StringValue(value)

  implicit def asPropertyValue(value: Date) =
    new DateValue(value)

  implicit def asPropertyValue(value: JSONSerializable) =
    new JSONValue(value)

  implicit def asPropertyValue(values: Iterable[_]) =
    new ArrayValue(values.toList map apply)

  implicit def asPropertyValue(map: Map[_, _]) = {
    val property_map =
      for ((name, value) <- map)
        yield name match {
          case name: String => (name -> apply(value))
          case name: Symbol => (name.name -> apply(value))
        }
    new Properties(property_map)
  }

  implicit def asPropertyValue(items: Array[_]) =
    new ArrayValue(items map apply)

  def asPropertyValue(json_object: JSONObject) = {
    var names: List[String] = List()
    val keys = json_object.keys
    while (keys.hasNext)
      names = keys.next.asInstanceOf[String] :: names
    val pairs =
      for (name <- names)
        yield name -> apply(json_object.get(name))
    Properties(pairs: _*)
  }

  def asPropertyValue(json_array: JSONArray) = {
    val length = json_array.length
    val elements =
          for (index <- 0 until length)
            yield apply(json_array.get(index))
    new ArrayValue(elements)
  }

  def apply(source: Any): PropertyValue = source match {
    case null                           => new NullValue
    case value: Int                     => asPropertyValue(value)
    case value: Boolean                 => asPropertyValue(value)
    case value: String                  => asPropertyValue(value)
    case value: Date                    => asPropertyValue(value)
    case value: Map[_, _]               => asPropertyValue(value)
    case value: Iterable[_]             => asPropertyValue(value)
    case value: Array[_]                => asPropertyValue(value)
    case value: java.util.Collection[_] => asPropertyValue(value)
    case value: JSONObject              => asPropertyValue(value)
    case value: JSONArray               => asPropertyValue(value)
    case value: JSONSerializable        => asPropertyValue(value)
    case value: PropertyValue           => value
    case _ =>
      throw new PropertyException("Unknown property type: " +
              source.asInstanceOf[AnyRef].getClass)
  }

  implicit def toInt(property_value: PropertyValue): Int =
    property_value match {
      case property_value: IntegerValue => property_value.value
    }

  implicit def toBoolean(property_value: PropertyValue): Boolean =
    property_value match {
      case property_value: BooleanValue => property_value.value
    }

  implicit def toString(property_value: PropertyValue): String =
    property_value match {
      case property_value: StringValue => property_value.value
    }

  implicit def toSeq(property_value: PropertyValue) =
    property_value match {
      case property_value: ArrayValue => property_value.value
    }

  implicit def toMap(property_value: PropertyValue) =
    property_value match {
      case property_value: Properties => property_value.property_map
    }

}

abstract class PropertyValue {

  def setIn(json_object: JSONObject, name: String): Unit

  def addTo(json_array: JSONArray)
}

