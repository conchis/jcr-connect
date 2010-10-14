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
 * @version 11 [10] 2010
 */

package edu.northwestern.art.jcr_access.bookmarks

import edu.northwestern.art.content_core.utilities.Storage
import java.util.ArrayList
import javax.persistence._
import edu.northwestern.art.content_core.properties.{Properties, JSONSerializable}
import edu.northwestern.art.content_core.catalog.Catalog
import scala.collection.JavaConversions._


@Entity
class Category(init_user: User, init_name: String) extends JSONSerializable {

  def this() = this(null, null)

  @Id @GeneratedValue
  var id: Int = 0

  @ManyToOne
  var user: User = init_user

  var name: String = init_name

  @ManyToMany(mappedBy="categories")
  var bookmarks: java.util.List[Bookmark] = new ArrayList

  def addedTo(bookmark: Bookmark) {
    bookmarks.add(bookmark)
  }

  def removedFrom(bookmark: Bookmark) {
    bookmarks.remove(bookmark)
  }

  def toJSON =
    Properties("name" -> name, "bookmarks" -> bookmarks).toJSON

  def toCatalog =
    new Catalog("C" + id, name, (bookmarks map (_.toCatalog)).toList)
}

object Category extends Storage[Category] {

  /**
   * Creates a new Category with a specified user and name.
   *
   * @param name category name
   */

  def create(user: User, name: String): Category = {
    val new_category = new Category(user, name)
    persist(new_category)
    user.categoryAdded(new_category)
    new_category
  }

  def find(user: User, name: String): Option[Category] = {
    val query = Category.manager.createQuery(
      "SELECT c FROM Category c WHERE c.user = :user AND c.name = :name")
    query.setParameter("user", user)
    query.setParameter("name", name)
    try {
      Some(query.getSingleResult.asInstanceOf[Category])
    }
    catch {
      case except: NoResultException =>
        None
    }
  }

  def findOrCreate(user: User, name: String) = {
    find(user, name) match {
      case Some(category) =>
        category
      case None =>
        create(user, name)
    }
  }

}