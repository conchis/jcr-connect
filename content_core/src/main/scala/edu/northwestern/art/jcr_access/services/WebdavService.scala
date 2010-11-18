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
 * @author Xin Xiang
 */

package edu.northwestern.art.jcr_access.services


import javax.jcr.Repository

import org.apache.jackrabbit.j2ee.RepositoryAccessServlet

import org.apache.jackrabbit.server.BasicCredentialsProvider
import org.apache.jackrabbit.server.CredentialsProvider
import org.apache.jackrabbit.server.SessionProvider
import org.apache.jackrabbit.server.SessionProviderImpl

import edu.northwestern.art.jcr_access.services.auth.CookieCredentialsProvider

/**
 * A wrapper around SimpleWebdavServlet to implement cookie-based credentials.
 * 
 * WebdavServlet provides webdav support (level 1 and 2 complient) for repository
 * resources.
 */
class WebdavService extends org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet {
  /** the 'missing-auth-mapping' init parameter */
  val INIT_PARAM_MISSING_AUTH_MAPPING = "missing-auth-mapping"

  /**
   * the jcr repository
   */
  var repository: Repository = null

  /**
   * the repository session provider
   */
  var sessionProvider: SessionProvider = null


  /**
   * Returns the <code>Repository</code>. If no repository has been set or
   * created the repository initialized by <code>RepositoryAccessServlet</code>
   * is returned.
   *
   * @return repository
   * @see RepositoryAccessServlet#getRepository(ServletContext)
   */
  def getRepository(): Repository = {
    if (repository == null) {
      // repository = RepositoryAccessServlet.getRepository(getServletContext());
      // use JNDI to get the repository instance
      val ctx = new javax.naming.InitialContext
      val env = ctx.lookup("java:comp/env").asInstanceOf[javax.naming.Context]
      repository = env.lookup("jcr/repository").asInstanceOf[javax.jcr.Repository]
    }

    return repository;
  }

  /**
   * Sets the <code>Repository</code>.
   *
   * @param repository
   */
  def setRepository(repository: Repository) = {
    this.repository = repository;
  }

  /**
   * Factory method for creating the credentials provider to be used for
   * accessing the credentials associated with a request. The default
   * implementation returns a {@link BasicCredentialsProvider} instance,
   * but subclasses can override this method to add support for other
   * types of credentials.
   *
   * @return the credentilas provider
   * @since 1.3
   */
  override def getCredentialsProvider(): CredentialsProvider = {
    return new CookieCredentialsProvider(getInitParameter(INIT_PARAM_MISSING_AUTH_MAPPING))
  }
}
