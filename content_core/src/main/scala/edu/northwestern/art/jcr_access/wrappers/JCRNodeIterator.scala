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
 * @version 03 [09] 2010
 */

package edu.northwestern.art.jcr_access.wrappers

import collection.JavaConversions.JIteratorWrapper
import javax.jcr.{Node, NodeIterator}

class JCRNodeIterator(val iterator: NodeIterator)
        extends JIteratorWrapper[Node](
          iterator.asInstanceOf[java.util.Iterator[Node]]) {

}

object JCRNodeIterator {

  implicit def asJCRNodeIterator(iterator: NodeIterator): JCRNodeIterator =
    new JCRNodeIterator(iterator)

  implicit def asNodeIterator(jcr_iterator: JCRNodeIterator): NodeIterator =
    jcr_iterator.iterator
}