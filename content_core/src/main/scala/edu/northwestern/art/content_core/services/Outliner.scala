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
 * @version 19 [07] 2010
 */

package edu.northwestern.art.content_core.services

import java.util.HashSet
import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.jcr_access.services.SearchService

class Outliner extends javax.ws.rs.core.Application {
  Storage.unit("Testing")

  override def getClasses: java.util.Set[Class[_]] = {
    val classes = new HashSet[Class[_]]
    classes.add(classOf[TaxonomyService])
    classes.add(classOf[SearchService])
    classes
  }
}