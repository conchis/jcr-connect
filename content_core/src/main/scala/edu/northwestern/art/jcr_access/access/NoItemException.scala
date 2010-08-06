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
 * @version 06[08] 2010
 */

package edu.northwestern.art.jcr_access.access

/**
 * Exception thrown when a specified repository path does not correspond
 * to valid item data.
 *
 * @param message descriptive message
 */

class NoItemException(message: String) extends RuntimeException(message) {

  /**
   * Constructs a NoItemException with no message.
   */

  def this() = this(null)

}