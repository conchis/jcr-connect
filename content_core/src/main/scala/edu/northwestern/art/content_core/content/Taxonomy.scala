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
 * @version 23 July 2010
 */

package edu.northwestern.art.content_core.content

import javax.persistence._

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.properties.{Properties, JSONSerializable}

@Entity
class Taxonomy extends JSONSerializable {

  /** Taxonomy id */
  @Id @GeneratedValue
  var id: Int = _

  /** Taxonomy name */
  var name: String = _

  /** Description of this Taxonomy */
  @Lob
  var description: String = _

  /** Tree of categories. */
  @OneToOne
  var categoryTree: Category = _

  /**
   * Adds a Category to the Category tree. By default the category is added to
   * the top level. Optional arguments allow the category to be added at a
   * specified index, under a specific parent category.
   */

  def add(new_category: Category, index: Int = -1,
          parent: Category = categoryTree): Category = {
    if (parent.taxonomy != this)
      throw new InvalidParentException("Parent category not in this Taxonomy")
    try {
      category(new_category.name)
      throw new DuplicateCategoryException(new_category.name)
    }
    catch {
      case _: NotFoundException =>
        new_category.belongsTo(this)
        categoryTree.add(new_category, index)
        new_category
    }
  }

  /**
   *   Returns a specific Category specified by name.
   */

  def category(name: String): Category =
    try {
      Category.find(this, name)
    }
    catch {
      case _: NoResultException =>
        throw new NotFoundException("No category named: " + name)
    }

  /**
   * Removes this Taxonomy and all associated categories.
   */

  def remove {
    categoryTree.remove
    Taxonomy.manager.remove(this)
  }

  /**
   * Returns a JSON representation of this Taxonomy.
   */

  def toJSON = Properties(
    "class" -> classOf[Taxonomy].getCanonicalName,
    "id" -> id,
    "name" -> name,
    "categories" -> categoryTree
  ).toJSON

}

object Taxonomy extends Storage[Taxonomy] {

  def initialize(taxonomy: Taxonomy, name: String, description: String,
      categoryTree: Category) = {
    taxonomy.name = name
    taxonomy.description = description
    taxonomy.categoryTree = categoryTree
    categoryTree.belongsTo(taxonomy)
    taxonomy
  }

  def create(name: String, description: String = null): Taxonomy = {
    val taxonomy = new Taxonomy
    manager.persist(taxonomy)
    initialize(taxonomy, name, description, Category.create("/"))
  }

  /**
   * Finds a Taxonomy by name.
   */

  def find(name: String): Taxonomy = {
    val query = Category.manager.createQuery(
      "SELECT t FROM Taxonomy t WHERE t.name = :name")
    query.setParameter("name", name)
    try {
      query.getSingleResult.asInstanceOf[Taxonomy]
    }
    catch {
      case _: NoResultException =>
        throw new NotFoundException("No Taxonomy: " + name)
    }
  }

}