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
 * @version 29 July 2010
 */

package edu.northwestern.art.jcr_access.services

import java.io.{OutputStreamWriter, OutputStream}

import edu.northwestern.art.jcr_access.repositories.LocalConnector
import javax.ws.rs.core.{StreamingOutput, Response}
import javax.ws.rs._
import edu.northwestern.art.jcr_access.access.{FailureException, NoItemException}

@Path("/access")
class AccessService {

  val repository_url = "http://localhost:8080/jackrabbit/rmi"
  val user = "admin"
  val pass = "admin"

  val connector = new LocalConnector(repository_url, user, pass)

  private def getJSON(path: String): String = {
    val repository_path =
      if (path.startsWith("/"))
        path
      else
        "/" + path
      if (connector.isItem(repository_path))
         connector.get(repository_path).toJSON.toString
      else
         connector.catalog("/" + path).toJSON.toString
  }

  @GET @Path("/{path: .*}")
  @Produces(Array("application/json"))
  def getContent(@PathParam("path") path: String): StreamingOutput = {
    val repository_path = "/" + path
    new StreamingOutput {
      def write(output: OutputStream) = {
        try {
          val out = new OutputStreamWriter(output)
          out.write(getJSON(path))
          out.flush
        }
        catch {
          case _: NoItemException =>
            throw new WebApplicationException(Response.Status.NOT_FOUND)
          case except: FailureException =>
            throw new WebApplicationException(except, Response.Status.INTERNAL_SERVER_ERROR)
        }
      }
    }
  }
  
}