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
 * @version 10 August 2010
 */

package edu.northwestern.art.content_core.stores

import edu.northwestern.art.content_core.catalog.Catalog
import edu.northwestern.art.content_core.content2.Item

/**
 * Interface for Item containers.
 */

trait Store {

  /**
   * Returns true only if the specified path identifies a valid
   * item in the Store.
   */

  def isItem(path: String): Boolean

  /**
   * Returns an Item containing content from the specified path in the Store.
   * isItem(path) must be true.
   */

  def get(path: String): Item

  /**
   * Inserts an Item into the Store. After this is called, if it succeeds,
   * isItem(path) should be true.
   */

  def put(path: String, item: Item)

  /**
   * Deletes an item from the repository.
   */

  def delete(path: String): Boolean

  /**
   * Returns a catalog of items under a specified node in the Store. For each
   * child summarized in the Folder object, isItem(path) should be true.
   */

  def catalog(path: String): Catalog

  /**
   * Free text search of the repository. Returns a Folder of results.
   */

  def search(text: String): Catalog


}