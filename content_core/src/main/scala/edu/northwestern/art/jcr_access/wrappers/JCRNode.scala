/**
 *   Copyright 2010 Northwestern University.
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
 * @version 03[09] 2010
 */

package edu.northwestern.art.jcr_access.wrappers
import javax.jcr.Node

/**
 * Wrapper class for JCR Node objects.
 */

class JCRNode(val node: Node) {

  def name = node.getName

  def path = node.getPath

  def \ (path: String) = new JCRNode(node.getNode(path))

  def parent = new JCRNode(node.getParent)

  def children = new JCRNodeIterator(node.getNodes)

  /**
   * Determines if two objects are equal. This may be used to compare
   * to another JCRNode instance, or a Node instance.
   *
   * @param other object to compare
   * @return true if both refer to the same Node, false otherwise.
   */

  override def equals(other: Any): Boolean = other match {
    case other: JCRNode => node.equals(other.node)
    case other: Node => other.equals(node)
    case _ => false
  }
}

object JCRNode {

  /**
   *  Converts a Node to a JCRNode.
   *
   * @param node Node to be converted.
   * @return JCRNode
   */

  implicit def asJCRNode(node: Node): JCRNode =
    new JCRNode(node)

  /**
   * Converts a JCRNode to a Node.
   *
   * @param jcr_node JCRNode to be converted.
   * @return Node
   */

  implicit def asNode(jcr_node: JCRNode): Node =
    jcr_node.node


}