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
package edu.northwestern.jcr;

import org.apache.jackrabbit.core.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Map;

import net.sf.jpam.Pam;

/**
 * <code>SimpleLoginModule</code>...
 */
public class JCRLoginModule extends AbstractLoginModule {

    private static Logger log = LoggerFactory.getLogger(JCRLoginModule.class);

    /**
     * @see AbstractLoginModule#doInit(javax.security.auth.callback.CallbackHandler, javax.jcr.Session, java.util.Map)
     */
    protected void doInit(CallbackHandler callbackHandler, Session session, Map options) throws LoginException {
        // nothing to do
        log.debug("init: JCRLoginModule. Done.");
    }

    /**
     * @see AbstractLoginModule#impersonate(java.security.Principal, javax.jcr.Credentials)
     */
    protected boolean impersonate(Principal principal, Credentials credentials) throws RepositoryException, LoginException {
        if (principal instanceof Group) {
            return false;
        }
        Subject impersSubject = getImpersonatorSubject(credentials);
        return impersSubject != null;
    }

    /**
     * @see AbstractLoginModule#getAuthentication(java.security.Principal, javax.jcr.Credentials)
     */
    protected Authentication getAuthentication(Principal principal, Credentials creds) throws RepositoryException {
        if (principal instanceof Group) {
            return null;
        }
        return new Authentication() {
            public boolean canHandle(Credentials credentials) {
                return true;
            }
            public boolean authenticate(Credentials credentials) throws RepositoryException {
                String username = ((SimpleCredentials) credentials).getUserID();
                char [] password = ((SimpleCredentials) credentials).getPassword();
                if (username.equals("anonymous")) {
                    // disable anonymous access
                    return false;
                }

                Pam pam = new Pam();
                System.out.println("authenticating " + username);
                // return true;
                return pam.authenticateSuccessful(username, new String(password));
            }
        };
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
    protected Principal getPrincipal(Credentials credentials) {
        String userId = getUserID(credentials);
        Principal principal = principalProvider.getPrincipal(userId);
        if (principal == null || principal instanceof Group) {
            // no matching user principal
            return null;
        } else {
            return principal;
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
    public boolean login() throws LoginException {
        if (!isInitialized()) {
            log.warn("Unable to perform login: initialization not completed.");
            return false;
        }

        // check the availability of Credentials
        Credentials creds = getCredentials();
        if (creds == null) {
            log.warn("No credentials available -> try default (anonymous) authentication.");
        }
        try {
            Principal userPrincipal = getPrincipal(creds);
            if (userPrincipal == null) {
                // unknown principal or a Group-principal
                log.debug("Unknown User -> ignore.");
                return false;
            }
            boolean authenticated;
            // test for anonymous, pre-authentication, impersonation or common authentication.
            if (/*isAnonymous(creds) ||*/ isPreAuthenticated(creds)) {
                authenticated = true;
            } else if (isImpersonation(creds)) {
                authenticated = impersonate(userPrincipal, creds);
            } else {
                authenticated = authenticate(userPrincipal, creds);
            }

            System.out.println("authenticated: " + authenticated);

            // process authenticated user
            if (authenticated) {
                if (creds instanceof SimpleCredentials) {
                    credentials = (SimpleCredentials) creds;
                    // System.out.println(credentials.getPassword());
                    System.out.println("isAnonymous: " + isAnonymous(creds));
                    System.out.println("isPreAuthenticated: " + isPreAuthenticated(creds));
                    System.out.println("isImpersonation: " + isImpersonation(creds));
                    if (getUserID(creds).equals("superuser")) {
                        throw new LoginException("Invalid user name!");
                    }
                } else {
                    credentials = new SimpleCredentials(getUserID(creds), new char[0]);
                }
                principal = userPrincipal;
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Login failed:", e);
        }
        return false;
    }
}
