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
 * @version 24 [07] 2010
 */

package edu.northwestern.art.content_core.services

import java.io.{OutputStream, OutputStreamWriter}
import javax.ws.rs._
import core.{Response, StreamingOutput}

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.utilities.Storage.transaction
import edu.northwestern.art.content_core.content.{Category, NotFoundException, Taxonomy}


@Path("/taxon")
class TaxonomyService {

  @GET @Path("/{tid}")
  @Produces(Array("application/json"))
  def getTaxonomy(@PathParam("tid") taxonomyName: String) = {
    try {
      val taxonomy = Taxonomy.find(taxonomyName)
      taxonomy.toString
    }
    catch {
      case except: NotFoundException =>
        throw new WebApplicationException(Response.Status.NOT_FOUND)
    }
  }

  @POST @Path("/")
  @Produces(Array("application/json"))
  def createTaxonomy(@QueryParam("name") taxonomyName: String): String = {
    val taxonomy: Taxonomy = transaction { Taxonomy.create(taxonomyName) }
    return taxonomy.toString
  }
  
  @POST @Path("/{tid}")
  @Produces(Array("application/json"))
  def addCategory(
      @PathParam("tid")          taxonomyName: String,
      @QueryParam("name")        categoryName: String,
      @QueryParam("parent")      parentName:   String,
      @QueryParam("description") description:  String): String = {

    try {
      val taxonomy = Taxonomy.find(taxonomyName)
      val category: Category = transaction {
        taxonomy.add(Category.create(categoryName))
      }
      return category.toString
    }
    catch {
      case except: NotFoundException =>
        throw new WebApplicationException(Response.Status.NOT_FOUND)
    }
  }

  @GET @Path("{tld}/{cid}")
  @Produces(Array("application/json"))
  def getCategory(@PathParam("tid") taxonomyName: String,
                  @PathParam("cid") categoryName: String): String = {
    try {
      val taxonomy = Taxonomy.find(taxonomyName)
      val category: Category = taxonomy.category(categoryName)
      category.toString
    }
    catch {
      case except: NotFoundException =>
        throw new WebApplicationException(Response.Status.NOT_FOUND)
    }    
  }


}