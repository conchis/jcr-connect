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

import edu.northwestern.art.content_core.content.{Item, Metadata}
import edu.northwestern.art.content_core.properties.Properties

class ImageItem(metadata: Metadata, val sources: List[ImageSource] = List())
        extends Item {

  override def toJSON = Properties(
      "metadata"  -> metadata,
      "sources"   -> sources
    ).toJSON
}