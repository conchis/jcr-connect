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
 * @version 29 July 2010
 */

package edu.northwestern.art.jcr_access.access

import edu.northwestern.art.content_core.content.Item
import edu.northwestern.art.content_core.catalog.Catalog
import org.apache.jackrabbit.rmi.repository.URLRemoteRepository
import org.apache.jackrabbit.rmi.repository.RMIRemoteRepository
import javax.jcr.{SimpleCredentials, Session}

/**
 * A RepositoryConnector provides an interface for accessing content
 * as content_core Items in a repository.
 */

abstract class RepositoryConnector(val repository_url: String, workspace: String,
    val user: String, val password: String) {

  /** JCR Repository accessed via this connector */

  // use RMI to access the repository
  // val repository = new URLRemoteRepository(repository_url)
  // val repository = new RMIRemoteRepository("//localhost/jackrabbit.repository");
  // use JNDI to access the repository
  val ctx = new javax.naming.InitialContext
  val env = ctx.lookup("java:comp/env").asInstanceOf[javax.naming.Context]
  val repository = env.lookup("jcr/repository").asInstanceOf[javax.jcr.Repository]

  /** Login credentials for repository. */
  val credentials = new SimpleCredentials(user, password.toCharArray)

  /** JCR Session if active, null otherwise. */
  private var saved_session: Session = null

  /**
   * Returns the current session if one if active.
   */

  def session: Session =
    if (saved_session !=null)
      saved_session
    else
      throw new ConnectorException("No active JCR session")

  /**
   * Executes a function within a JCR repository session. The JCR session is
   * passed to the callback function. This method is designed to allow
   * calls to be nested without creating a new session.
   */

  def session[T] (callback: (Session) => T): T = {
    if (saved_session == null) {
      saved_session = repository.login(credentials, workspace)
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

  /**
   *  Returns true only if the specified path identifies an Item
   * in the repository.
   */

  def isItem(path: String): Boolean

  /**
   * Returns an Item (not managed) containing content from the specified
   * path in the repository. isItem(path) must be true.
   */

  def get(path: String): Item

  /**
   * Inserts an Item into the repository. After this is called, if it
   * succeeds, isItem(path) should be true.
   */

  def put(path: String, item: Item)

  /**
   * Returns a catalog of items under a specified node in the repository.
   * For each child summarized in the Folder object, isItem(path) should
   * be true.
   */

  def catalog(path: String): Catalog

  /**
   * Free text search of the repository. Returns a Folder of results.
   */

  def search(text: String, offset: Long, limit: Long): Catalog

}

object RepositoryConnector {

  type ConnectorFactory = () => RepositoryConnector

  private val factories: collection.mutable.Map[String, ConnectorFactory] =
      collection.mutable.Map()

  private val connectors: collection.mutable.Map[String, RepositoryConnector] =
      collection.mutable.Map()

  def register(source: String, factory: => RepositoryConnector) {
    factories(source) = factory _
  }

  def forSource(source: String) =
    connectors.getOrElseUpdate(source, {
      val factory = factories(source)
      if (factory == null)
        throw new ConnectorException("No connector for source: " + source)
      factory()
    })

}

