/**
 *     Copyright 2010 Northwestern University.
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

package edu.northwestern.art.jcr_access.access

import edu.northwestern.art.content_core.content.Item
import edu.northwestern.art.content_core.catalog.Folder

/**
 * A RepositoryConnector provides an interface for accessing content
 * as content_core Items in a repository.
 */

trait RepositoryConnector {

  /**
   * Returns true only if the specified path identifies an Item
   * in the repository.
   */

  def isItem(path: String): Boolean

  /**
   * Returns true only if the specified path corrsponds to a Folder
   * in the repository.
   */

  def isFolder(path: String): Boolean

  /**
   * Returns an Item (not managed) containing content from the specified
   * path in the repository. isItem(path) must be true.
   */

  def get(path: String): Item

  /**
   * Inserts an Item into the repository. After this is called, if it
   * succeeds, isItem(path) should be true.
   */

  def put(path: String, item: Item)

  /**
   * Returns a catalog of items under a specified node in the repository.
   * For each child summarized in the Folder object, isItem(path) should
   * be true.
   */

  def catalog(path: String): Folder

}