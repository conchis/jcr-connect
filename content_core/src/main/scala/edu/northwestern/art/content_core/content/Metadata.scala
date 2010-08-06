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

package edu.northwestern.art.content_core.content

import java.util.Date

import java.io.Serializable
import javax.persistence.{Temporal, TemporalType, Embeddable, Entity}

import edu.northwestern.art.content_core.properties.{JSONSerializable, Properties}

/**
 * Basic DC fields stored for each item.
 */

@Embeddable
class Metadata extends Serializable with JSONSerializable {

  var title: String           = ""
  var description: String     = ""
  var creators: Array[String] = Array()
  var rights: Array[String]   = Array()
  var types: Array[String]    = Array()

  @Temporal(TemporalType.TIMESTAMP)
  var date: Date              = null

  def toJSON = Properties(
      "title"       -> title,
      "description" -> description,
      "creators"    -> creators,
      "rights"      -> rights,
      "types"       -> types,
      "date"        -> date
    ).toJSON

}

object Metadata {

  def apply(title: String, description: String = null,
            creators: Iterable[String] = List(), rights: Iterable[String] = List(),
            types: Iterable[String] = List()): Metadata = {
    val metadata = new Metadata

    metadata.title = title
    metadata.description = description
    metadata.creators    = creators.toArray
    metadata.rights      = rights.toArray
    metadata.types       = types.toArray

    metadata
  }

}