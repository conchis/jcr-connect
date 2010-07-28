/** 
 * Copyright 2010 Northwestern University.
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
 * @version 22 July 2010
 */

package edu.northwestern.art.permissions.content

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.users.Agent
import javax.persistence.{ManyToOne, Entity}

@Entity
class Permission {
}

object Permission extends Storage[Permission] {

  @ManyToOne
  var agent: Agent = _

  var isOwner: Boolean = _

  var isEditor: Boolean = _

  var isViewer: Boolean = _
}