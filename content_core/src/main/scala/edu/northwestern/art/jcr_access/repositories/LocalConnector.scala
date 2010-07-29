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
 * @version 29 [07] 2010
 */

package edu.northwestern.art.jcr_access.repositories

import edu.northwestern.art.jcr_access.access.RepositoryConnector
import edu.northwestern.art.content_core.catalog.Folder
import edu.northwestern.art.content_core.content.Item

/**
 * Simple connector to a local repository storing content in a straightforward
 * way -- as files and folders.
 */

class LocalConnector extends RepositoryConnector {

  def catalog(path: String): Folder = null

  def put(path: String, item: Item) = null

  def get(path: String): Item = null

  def isFolder(path: String): Boolean = null

  def isItem(path: String): Boolean = null

}