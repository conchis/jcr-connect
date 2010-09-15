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
package edu.northwestern.art.jcr_access.services.auth

import org.apache.jackrabbit.util.Base64
import org.apache.jackrabbit.webdav.DavConstants
import org.apache.jackrabbit.server.CredentialsProvider

import javax.jcr.Credentials
import javax.jcr.LoginException
import javax.jcr.SimpleCredentials
import javax.jcr.GuestCredentials
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Cookie
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Enumeration

/**
 * This Class implements a credentials provider that extracts security code
 * from cookie, in addition to the credentials
 * from the 'WWW-Authenticate' header and only supports 'Basic' authentication.
 */
class CookieCredentialsProvider(defaultHeaderValue: String) extends CredentialsProvider {

  val EMPTY_DEFAULT_HEADER_VALUE = ""
  val GUEST_DEFAULT_HEADER_VALUE = "guestcredentials"

  def getCookieValue(cookies: Array[Cookie], cookieName: String): String = {
    cookies.foreach { cookie: Cookie =>
      if (cookieName.equals(cookie.getName()))
        return(cookie.getValue())
    }
    
    return("")
  }
  

  /**
   * {@inheritDoc}
   *
   * Build a {@link Credentials} object for the given authorization header.
   * The creds may be used to login to the repository. If the specified header
   * string is <code>null</code> the behaviour depends on the
   * {@link #defaultHeaderValue} field:<br>
   * <ul>
   * <li> if this field is <code>null</code>, a LoginException is thrown.
   *      This is suiteable for clients (eg. webdav clients) for with
   *      sending a proper authorization header is not possible, if the
   *      server never send a 401.
   * <li> if this an empty string, null-credentials are returned, thus
   *      forcing an null login on the repository
   * <li> if this field has a 'user:password' value, the respective
   *      simple credentials are generated.
   * </ul>
   * <p/>
   * If the request header is present but cannot be parsed a
   * <code>ServletException</code> is thrown.
   *
   * @param request the servlet request
   * @return credentials or <code>null</code>.
   * @throws ServletException If the Authorization header cannot be decoded.
   * @throws LoginException if no suitable auth header and missing-auth-mapping
   *         is not present
   */
  override def getCredentials(request: HttpServletRequest): Credentials = {
    try {
      // extract code from cookie
      val cookies = request.getCookies()
      if (cookies != null) {
        val authStr = getCookieValue(cookies, "auth")
        if (authStr != "") {
          println(authStr)
          return new SimpleCredentials("cookie", authStr.toCharArray)
        }
      }
    
      // extract limited-time key from query string
      val e = request.getParameterNames
      while (e.hasMoreElements) {
        val name = e.nextElement.asInstanceOf[String]
    
        if (name == "key") {
          val values = request.getParameterValues(name)
          if (values != null) {
            return new SimpleCredentials("key", values(0).toCharArray)
          }
        }
      }
    
      // extract user name and password from authentication header
      val authHeader: String = request.getHeader(DavConstants.HEADER_AUTHORIZATION)
    
      // println("authHeader: " + authHeader)
    
      // dump the headers
      val headerNames = request.getHeaderNames
      while(headerNames.hasMoreElements()) {
        val name = headerNames.nextElement().asInstanceOf[String];
        println(name + ": " + request.getHeader(name))
      }
    
      if (authHeader != null) {
        val authStr = authHeader.split(" ")
        if (authStr.length >= 2 && authStr(0).equalsIgnoreCase(HttpServletRequest.BASIC_AUTH)) {
          val out: ByteArrayOutputStream = new ByteArrayOutputStream()
          Base64.decode(authStr(1).toCharArray(), out)
          val decAuthStr: String = out.toString("ISO-8859-1")
          val pos: Int = decAuthStr.indexOf(':')
          val userid: String = decAuthStr.substring(0, pos)
          val passwd: String = decAuthStr.substring(pos + 1)
    
          return new SimpleCredentials(userid, passwd.toCharArray())
        }
    
        throw new ServletException("Unable to decode authorization.")
      } else {
        // check special handling
        if (defaultHeaderValue == null) {
          throw new LoginException()
        } else if (EMPTY_DEFAULT_HEADER_VALUE.equals(defaultHeaderValue)) {
          return null
        } else if (GUEST_DEFAULT_HEADER_VALUE.equals(defaultHeaderValue)) {
          return new GuestCredentials()
        } else {
          val pos: Int = defaultHeaderValue.indexOf(':')
          if (pos < 0) {
            return new SimpleCredentials(defaultHeaderValue, new Array[Char](0))
          } else {
            return new SimpleCredentials(
              defaultHeaderValue.substring(0, pos),
              defaultHeaderValue.substring(pos+1).toCharArray()
            )
          }
        }
      }
    } catch {
      case e: IOException =>
        throw new ServletException("Unable to decode authorization: " + e.toString());
    }
  }

}
