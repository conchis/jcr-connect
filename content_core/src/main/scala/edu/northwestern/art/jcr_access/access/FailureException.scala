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
 * @version 06 [08] 2010
 */

package edu.northwestern.art.jcr_access.repositories

/**
 * Exception thrown when a repository or communications with a repository
 * fails.
 *
 * @param message descriptive message
 * @param reason Throwable that was the cause of this exception
 */

class FailureException(message: String, reason: Throwable)
        extends RuntimeException(message, reason) {

  /**
   * Constructs a FailureException with no message specified but
   * a reason Throwable that was the cause of the exception.
   */

  def this(reason: Throwable) = this(null, reason)

  /**
   * Constructs a FailureException with a message but no specified
   * reason.
   */

  def this(message: String) = this(message, null)

}