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
 * @version 19 [07] 2010
 */

package edu.northwestern.art.content_core.services

import java.io.{OutputStreamWriter, Writer, BufferedOutputStream, OutputStream}
import javax.ws.rs.core.{Context, UriInfo, StreamingOutput}
import javax.ws.rs.{PathParam, Produces, GET, Path}

@Path("/outline")
class OutlineService {

  @GET @Path("/{path: .*}")
  @Produces(Array("application/json"))
  def getContent(@PathParam("path") path: String): StreamingOutput = {
    new StreamingOutput {
      def write(output: OutputStream) = {
        val out = new OutputStreamWriter(output)
        out.write("{\"message\":\"Ok\", \"path\":\"" + path + "\"}")
        out.flush
      }
    }
  }

}