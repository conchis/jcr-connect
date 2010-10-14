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
 * @version 07 [09] 2010
 */

package edu.northwestern.art.jcr_access.wrappers

import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith

import JCRNodeSeq._

@RunWith(classOf[JUnitRunner])
class JCRNodeSeqSpec extends FlatSpec with ShouldMatchers {

  val repository_url = "http://localhost:8080/jackrabbit/rmi"
  val repository = new JCRRepository(repository_url, "admin", "admin")

  "A JCRNodeSeq" can "be created from any NodeIterator" in {
    repository.session((s) => {
      val nodes: JCRNodeSeq = (s \ "content" children)
      println(asNodeSeq(nodes) length)
    })
  }

  it can "find all decendant nodes with a specified path" in {
    repository.session((s) => {
      val nodes = (s \ "content" children) \ "contents.json"

      for (node <- nodes.sequence.take(100))
        println(node.getPath)
    })
  }

}