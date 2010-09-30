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

import edu.northwestern.art.jcr_access.access.RepositoryConnector
import edu.northwestern.art.jcr_access.repositories.LocalConnector
import edu.northwestern.art.jcr_access.repositories.FedoraConnector
import edu.northwestern.art.jcr_access.repositories.XTFConnector
import javax.ws.rs.{Produces, QueryParam, GET, Path}
import javax.ws.rs.core.Context
import javax.servlet.ServletConfig

@Path("/search")
class SearchService {

  @Context 
  val config: ServletConfig = null

  var repository_url = ""
  var user = ""
  var pass = ""

  var connector: RepositoryConnector = null

  @GET @Path("/")
  @Produces(Array("application/json"))
  def search(@QueryParam("q") query: String, @QueryParam("ws") workspace: String) = {
    repository_url = config.getInitParameter("repositoryurl")
    user = config.getInitParameter("username")
    pass = config.getInitParameter("password")

    workspace match {
      case "fedora" => connector = new FedoraConnector(repository_url, user, pass)
      case "xtf" => connector = new XTFConnector(repository_url, user, pass)
      case _ => connector = new LocalConnector(repository_url, user, pass)
    }

    if (query == null)
       "Ready"
    else {
      val results = connector.search(query)
      results.toJSON.toString
    }
  }
  
}
