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
 * @version 14 July 2010
 */

package edu.northwestern.art.content_core.content

import java.util.ArrayList
import javax.persistence._

import scala.collection.JavaConversions._

import org.json.JSONObject

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.properties.{Properties, JSONSerializable}

@Entity
class Item extends JSONSerializable {

  @Id @GeneratedValue
  var id: Int = 0

  /** Name of item */
  var name: String = ""

  @Embedded
  var metadata: Metadata = null

  @ManyToMany
  var categories: java.util.List[Category] = new ArrayList

  /**
   * Selectively initializes fields of an Item.
   */

  protected def initialize(metadata: Metadata = this.metadata,
          categories: Iterable[Category] = this.categories) {
    this.metadata = metadata
    setCategories(this.categories)
  }

  /**
   *  Returns true only if this item is in a specified category.
   */

  def inCategory(category: Category): Boolean = category.containsItem(this)

  /**
   * Returns true only if this item is in a category specified by categoryId.
   */

  def inCategory(categoryId: String): Boolean = inCategory(Category.find(categoryId))

  /**
   * Adds this item to a specified category.
   */

  def addTo(category: Category): Boolean = category.addItem(this)

  /**
   * Adds this item to a category specified by categoryId.
   */

  def addTo(categoryId: String): Boolean = addTo(Category.find(categoryId))

  /**
   * Removes this item from a specified category.
   */

  def removeFrom(category: Category): Boolean = category.removeItem(this)

  /**
   * Removes this item from a category specified by categoryId.
   */

  def removeFrom(categoryId: String): Boolean = removeFrom(Category.find(categoryId))

  /**
   * Sets the categories that this item belongs to.
   */

  def setCategories(newCategories: Iterable[Category]) {
    categories.toList.forall(removeFrom)
    newCategories.forall(addTo)
  }

  /**
   * Returns an Array of ids of all categories in the form taxonomy:category
   * for all categories in this item.
   */

  def categoryIds = categories map(_.categoryId)

  /**
   * Sets the ids of all categories that should contain this item.
   */

  def categoryIds_= (newIds: Iterable[String]) {
    setCategories(newIds map(Category.find))
  }

  /**
   * Returns a JSON representation of this Item.
   */

  def toJSON: JSONObject =
    Properties("id" -> id, "metadata" -> metadata,
      "categories" -> categoryIds).toJSON

}

object Item extends Storage[Item] {

  def create(metadata: Metadata = null): Item = {
    val item = new Item
    persist(item)

    item.metadata = metadata
    item
  }

  def apply(title: String, source: String): Item =
    create(Metadata(title))

  def apply(title: String): Item =
    create(Metadata(title))

  def apply(metadata: Metadata, source: String): Item =
    create(metadata)
  
}