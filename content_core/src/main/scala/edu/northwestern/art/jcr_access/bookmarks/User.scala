/**
 *  Copyright 2010 Northwestern University.
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
 * @version 11 October 2010
 */

package edu.northwestern.art.jcr_access.bookmarks

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.properties.{Properties, JSONSerializable}
import javax.persistence._
import java.util.ArrayList
import scala.collection.JavaConversions._
import edu.northwestern.art.content_core.catalog.Catalog

@Entity
class User(init_name: String) extends JSONSerializable {

  def this() = this(null)

  @Id @GeneratedValue
  var id: Int = 0

  var name: String = init_name

  @OneToMany(mappedBy="user")
  var categories: java.util.List[Category] = new ArrayList

  @OneToMany(mappedBy="user")
  var bookmarks: java.util.List[Bookmark] = new ArrayList

  def addBookmark(workspace: String, path: String, category_names: Array[String]) = {
    val bookmark = Bookmark.findOrCreate(this, workspace, path)
    bookmark.categoryNames = category_names
    bookmark
  }

  def findBookmark(workspace: String, path: String): Option[Bookmark] =
    Bookmark.find(this, workspace, path)

  def categoryAdded(category: Category) =
    categories.add(category)

  def categoryRemoved(category: Category) =
    categories.remove(category)

  def bookmarkAdded(bookmark: Bookmark) =
    bookmarks.add(bookmark)

  def bookmarkRemoved(bookmark: Bookmark) =
    bookmarks.remove(bookmark)

  def toJSON =
    Properties("id" -> id, "user" -> name, "categories" -> categories.toList).toJSON

  def toCatalog = {
    val category_items =
      categories map (category => new Catalog("C" + category.id, category.name, null))
    new Catalog(name, name + "'s bookmarks", category_items.toList)
  }
  
}

object User extends Storage[User] {

  /**
   * Creates a new User with a specified name.
   *
   * @param name user name
   */
  
  def create(name: String): User = {
    val new_user = new User(name)
    persist(new_user)
    new_user
  }

  def find(name: String): Option[User] = {
    val query = Category.manager.createQuery(
      "SELECT u FROM User u WHERE u.name = :name")
    query.setParameter("name", name)
    try {

      val result = Some(query.getSingleResult.asInstanceOf[User])
      println("got: " + result.toString)
      result
    }
    catch {
      case except: NoResultException =>
        None
    }
  }

  def findOrCreate(name: String) = {
    find(name) match {
      case Some(user) =>
        user
      case None =>
        create(name)
    }
  }
}