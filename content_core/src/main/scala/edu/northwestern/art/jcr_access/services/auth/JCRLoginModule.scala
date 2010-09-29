/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.northwestern.art.jcr_access.services.auth

import org.apache.jackrabbit.core.config.LoginModuleConfig
import org.apache.jackrabbit.core.security.authentication.AbstractLoginModule
import org.apache.jackrabbit.core.security.authentication.Authentication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.jcr.Credentials
import javax.jcr.SimpleCredentials
import javax.jcr.RepositoryException
import javax.jcr.Session
import javax.security.auth.Subject
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.login.LoginException
import java.security.Principal
import java.security.acl.Group
import java.util.Map
import java.util.Date

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence

/**
 * <code>SimpleLoginModule</code>...
 */
class JCRLoginModule extends AbstractLoginModule {

  val log: Logger = LoggerFactory.getLogger(getClass)

  var superUser = ""
  var superPassword = ""

  /**
   * Initialize this LoginModule and sets the following fields for later usage:
   * <ul>
   * <li>{@link PrincipalProvider} for user-{@link Principal} resolution.</li>
   * <li>{@link LoginModuleConfig#PARAM_ADMIN_ID} option is evaluated</li>
   * <li>{@link LoginModuleConfig#PARAM_ANONYMOUS_ID} option is evaluated</li>
   * </ul>
   * Implementations are called via
   * {@link #doInit(CallbackHandler, Session, Map)} to implement
   * additional initalization
   *
   * @param subject         the <code>Subject</code> to be authenticated. <p>
   * @param callbackHandler a <code>CallbackHandler</code> for communicating
   *                        with the end user (prompting for usernames and
   *                        passwords, for example). <p>
   * @param sharedState     state shared with other configured
   *                        LoginModules.<p>
   * @param options         options specified in the login <code>Configuration</code>
   *                        for this particular <code>LoginModule</code>.
   * @see LoginModule#initialize(Subject, CallbackHandler, Map, Map)
   * @see #doInit(CallbackHandler, Session, Map)
   * @see #isInitialized()
   */
  override def initialize(subject: Subject, callbackHandler: CallbackHandler,
                          sharedState: Map[String, _], options: Map[String, _]) = {
    super.initialize(subject, callbackHandler, sharedState, options)

    if (options.containsKey(LoginModuleConfig.PARAM_ADMIN_ID)) {
      superUser = options.get(LoginModuleConfig.PARAM_ADMIN_ID).asInstanceOf[String]
    }
    else {
      superUser = "admin"
    }

    if (options.containsKey("password")) {
      superPassword = options.get("password").asInstanceOf[String]
    }
  }

  /**
   * @see AbstractLoginModule#doInit(javax.security.auth.callback.CallbackHandler, javax.jcr.Session, java.util.Map)
   */
  override def doInit(callbackHandler: CallbackHandler, session: Session, options: Map[_, _]) = {
    // nothing to do
    log.debug("init: JCRLoginModule. Done.")
  }

  /**
   * @see AbstractLoginModule#impersonate(java.security.Principal, javax.jcr.Credentials)
   */
  def impersonate(principal: Principal, credentials: Credentials): Boolean = {
    principal match {
      case x: Group => return false
      case _ =>
    }

    val impersSubject: Subject = getImpersonatorSubject(credentials)

    return impersSubject != null
  }

  /**
   * Gets the date by key.
   * 
   * @param key String
   */
  def getDateByKey(key: String): Date = {
    val emf: EntityManagerFactory = Persistence.createEntityManagerFactory("key")
    val em: EntityManager = emf.createEntityManager
    val q = em.createQuery("SELECT x FROM AccessKey x WHERE x.key = :key")
    q.setParameter("key", key)
    val results = q.getResultList.asInstanceOf[java.util.List[AccessKey]]
    if (results != null && results.size > 0) {
      return results.get(0).getDate
    }

    return null
  }

  /**
   * @see AbstractLoginModule#getAuthentication(java.security.Principal, javax.jcr.Credentials)
   */
  override def getAuthentication(principal: Principal, creds: Credentials): Authentication = {
    principal match {
      case x: Group => {
        return null
      }
      case _ =>
    }

    return new Authentication() {
      def canHandle(credentials: Credentials): Boolean = {
        return true
      }
      
      def authenticate(credentials: Credentials): Boolean = {
        val username = credentials.asInstanceOf[SimpleCredentials].getUserID
        val password = credentials.asInstanceOf[SimpleCredentials].getPassword

        // if (username == "anonymous") {
        //   // disable anonymous access
        //   return false
        // }

        if (username == superUser && new String(password) == superPassword) {
          return true
        }

        if (username == "key") {
          // limited-time key based authentication
          val keyDate = getDateByKey(new String(password))
          if (keyDate == null) {
            println("invalid key!")
            return false
          }
          println(keyDate)
          val currentDate = new Date
          val diff = (currentDate.getTime - keyDate.getTime) / 1000
          if (diff > 30)
            return false
          else
            return true
        }

        if (username == "cookie") {
          // cookie-based authentication
          // to be added
        }

        return false
      }
    }
  }

  /**
  * Uses the configured {@link org.apache.jackrabbit.core.security.principal.PrincipalProvider} to retrieve the principal.
  * It takes the {@link org.apache.jackrabbit.core.security.principal.PrincipalProvider#getPrincipal(String)} for the User-ID
  * resolved by  {@link #getUserID(Credentials)}, assuming that
  * User-ID and the corresponding principal name are always identical.
  *
  * @param credentials Credentials for which the principal should be resolved.
  * @return principal or <code>null</code> if the principal provider does
  * not contain a user-principal with the given userID/principal name.
  *
  * @see AbstractLoginModule#getPrincipal(Credentials)
  */
  def getPrincipal(credentials: Credentials): Principal = {
    val userId = getUserID(credentials)
    val principal = principalProvider.getPrincipal(userId)

    principal match {
      case x: Group => return null
      case _ => return principal
    }
  }

  /**
  * Method to authenticate a <code>Subject</code> (phase 1).<p/>
  * The login is devided into 3 Phases:<p/>
  *
  * <b>1) User-ID resolution</b><br>
  * In a first step it is tried to resolve a User-ID for further validation.
  * As for JCR the identification is marked with the {@link Credentials}
  * interface, credentials are accessed in this phase.<br>
  * If no User-ID can be found, anonymous access is granted with the ID of
  * the anonymous user (as defined in the security configuration).
  * Anonymous access can be switched off removing the configuration entry.
  * <br> This implementation uses two helper-methods, which allow for
    * customization:
  * <ul>
  * <li>{@link #getCredentials()}</li> and
  * <li>{@link #getUserID(Credentials)}</li>
  * </ul>
  * <p/>
  *
  * <b>2) User-Principal resolution </b><br>
  * In a second step it is tested, if the resolved User-ID belongs to a User
  * known to the system, i.e. if the {@link PrincipalProvider} has a principal
  * for the given ID and the principal can be found via
  * {@link PrincipalProvider#findPrincipals(String)}.<br>
  * The provider implemenation can be set by the configuration option with the
  * name {@link LoginModuleConfig#PARAM_PRINCIPAL_PROVIDER_CLASS principal_provider.class}.
  * If the option is missing, the system default prinvipal provider will
  * be used.<p/>
  *
  * <b>3) Verification</b><br>
  * There are four cases, how the User-ID can be verfied:
  * The login is anonymous, preauthenticated or the login is the result of
  * an impersonation request (see {@link javax.jcr.Session#impersonate(Credentials)}
  * or of a login to the Repository ({@link javax.jcr.Repository#login(Credentials)}).
  * The concrete implementation of the LoginModule is responsible for all
  * four cases:
  * <ul>
  * <li>{@link #isAnonymous(Credentials)}</li>
  * <li>{@link #isPreAuthenticated(Credentials)}</li>
  * <li>{@link #authenticate(Principal, Credentials)}</li>
  * <li>{@link #impersonate(Principal, Credentials)}</li>
  * </ul>
  *
  * Under the following conditions, the login process is aborted and the
  * module is marked to be ignored:
  * <ul>
  * <li>No User-ID could be resolve, and anyonymous access is switched off</li>
  * <li>No Principal is found for the User-ID resolved</li>
  * </ul>
  *
  * Under the follwoing conditions, the login process is marked to be invalid
  * by throwing an LoginException:
  * <ul>
  * <li>It is an impersonation request, but the impersonator is not allowed
  * to impersonate to the requested User-ID</li>
  * <li>The user tries to login, but the Credentials can not be verified.</li>
  * </ul>
  * <p/>
  * The LoginModule keeps the Credentials and the Principal as instance fields,
  * to mark that login has been successfull.
  *
  * @return true if the authentication succeeded, or false if this
  *         <code>LoginModule</code> should be ignored.
  * @throws LoginException if the authentication fails
  * @see javax.security.auth.spi.LoginModule#login()
  * @see #getCredentials()
  * @see #getUserID(Credentials)
  * @see #getImpersonatorSubject(Credentials)
  */
  override def login(): Boolean = {
    if (!isInitialized()) {
      log.warn("Unable to perform login: initialization not completed.")
      return false
    }

    // check the availability of Credentials
    val creds = getCredentials()
    // val creds = new SimpleCredentials("abc", new Array[Char](1))
    
    if (creds == null) {
      log.warn("No credentials available -> try default (anonymous) authentication.")
    }
    try {
      val userPrincipal = getPrincipal(creds)
      if (userPrincipal == null) {
        // unknown principal or a Group-principal
        log.debug("Unknown User -> ignore.")
        return false
      }
    
      var authenticated = false
      // test for anonymous, pre-authentication, impersonation or common authentication.
      if (/*isAnonymous(creds) ||*/ isPreAuthenticated(creds)) {
        authenticated = true
      } else if (isImpersonation(creds)) {
        authenticated = impersonate(userPrincipal, creds)
      } else {
        authenticated = authenticate(userPrincipal, creds)
      }
    
      // process authenticated user
      if (authenticated) {
        creds match {
          case x: SimpleCredentials => {
            credentials = creds.asInstanceOf[SimpleCredentials]
            // System.out.println(credentials.getPassword())
            // println("isAnonymous: " + isAnonymous(creds))
            // println("isPreAuthenticated: " + isPreAuthenticated(creds))
            // println("isImpersonation: " + isImpersonation(creds))
            // if (getUserID(creds) == "superuser") {
            //   throw new LoginException("Invalid user name!")
            // }
          } 
          case _ => {
            credentials = new SimpleCredentials(getUserID(creds), new Array[Char](1))
          }
        }
    
        principal = userPrincipal
        return true
      }
    } catch {
      case e: RepositoryException => log.error("Login failed:", e)
    }
    
    return false
  }
}
