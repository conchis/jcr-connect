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
 * @version 03[09] 2010
 */

package edu.northwestern.art.jcr_access.wrappers

import org.apache.jackrabbit.rmi.repository.URLRemoteRepository
import javax.jcr.{SimpleCredentials, Session, Repository}

class JCRRepository(val repository_url: String, val user: String,
                    val password: String, workspace: String = null) {

  /** JCR Repository accessed via this connector */
  val repository = new URLRemoteRepository(repository_url)

  /** Login credentials for repository. */
  val credentials = new SimpleCredentials(user, password.toCharArray)

  /** JCR Session if active, null otherwise. */
  private var saved_session: JCRSession = null

  /**
   * Returns the current session if one if active.
   */

  def session: JCRSession =
    if (saved_session !=null)
      saved_session
    else
      throw new RuntimeException("No active JCR session")

  /**
   * Executes a function within a JCR repository session. The JCR session is
   * passed to the callback function. This method is designed to allow
   * calls to be nested without creating a new session.
   */

  def session[T] (callback: (JCRSession) => T): T = {
    if (saved_session == null) {
      saved_session =
          new JCRSession(repository.login(credentials, workspace))
      try {
        callback(saved_session)
      }
      finally {
        saved_session.logout
        saved_session = null
      }
    }
    else
      callback(saved_session)
  }

}

object JCRRepository {
}