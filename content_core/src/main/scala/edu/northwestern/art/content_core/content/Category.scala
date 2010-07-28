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
 * @version 13 July 2010
 */

package edu.northwestern.art.content_core.content

import javax.persistence._
import org.json.JSONObject

import edu.northwestern.art.content_core.utilities.Storage
import edu.northwestern.art.content_core.properties.{Properties, JSONSerializable}
import java.util.{ArrayList, Date}

@Entity
class Category extends JSONSerializable {

  /**Each Category's unique id. */
  @Id @GeneratedValue
  var id: Int = 0

  /** The Taxonomy that owns this Category. */
  @ManyToOne
  var taxonomy: Taxonomy = _

  /** Parent Category. */
  @ManyToOne @JoinColumn
  var parent: Category = _

  /** Index of this Category in parent Category */
  var index: Int = 0

  /** Category Name */
  var name: String = null

  /** Category description */
  @Lob
  var description: String = null

  /** Items in category. */
  @ManyToMany(mappedBy="category_references")
  var item_references: java.util.List[Item] = new ArrayList

  /**Time stamp for node creation. */
  @Temporal(TemporalType.TIMESTAMP)
  var created: Date = null

  /**Time stamp for node changes. */
  @Temporal(TemporalType.TIMESTAMP)
  var updated: Date = null

  /**
   * Category id is in the form "taxonomy:category" where taxonomy is the
   * Category Taxonomy's name, and category is the Category name. This
   * identifier uniquely identifies a category.
   */

  def categoryId: String =
    if (taxonomy != null)
      taxonomy.name + ":" + name
    else
      name

  /**
   * Sets the taxonomy of this and every descendant Category.
   */

  def belongsTo(taxonomy: Taxonomy) {
    if (taxonomy != this.taxonomy) {
      this.taxonomy = taxonomy
      children.foreach(_.belongsTo(taxonomy))  
    }
  }

  /**
   * Returns a count of child (sub) Categories.
   */
  
  def count: Int = {
    val query = Category.manager.createQuery(
      "SELECT Count(n) FROM Category n WHERE n.parent.id = :this_id ")
    query.setParameter("this_id", id)
    query.getSingleResult.asInstanceOf[Long].toInt
  }

  /**
   * Returns an Array[Category] of child (sub) Categories.
   */

  def children: Array[Category] = {
    val query = Category.manager.createQuery(
      "SELECT n FROM Category n " +
              "WHERE n.parent.id = :this_id " +
              "ORDER BY n.index ASC")
    query.setParameter("this_id", id)
    query.getResultList.toArray map (_.asInstanceOf[Category])
  }

  /**
   * Returns Some(category) if the category has a child with a
   * specified name. Returns None otherwise.
   */

  def get(name: String): Option[Category] =
    children.find(_.name == name)

  /**
   * Adds a child node to this node. An index may be specified,
   * otherwise the new node is added to the end.
   */

  def add(child: Category, index: Int = -1) {
    child.detach
    if (index == -1) {
      child.index = count
      child.parent = this
    }
    else {
      val new_index =
        if (child.parent != null && child.parent.id == id &&
                child.index < index)
          index - 1
        else
          index
      reorderChildren(new_index, 1)
      child.index = new_index
      child.parent = this
    }
    updated = new Date
  }

  /**
   * Detaches a specified child node from this node.
   */

  def detachChild(child: Category): Boolean = {
    if (child.parent != null && child.parent.id == id) {
      child.parent = null
      reorderChildren(child.index, 0)
      updated = new Date
      true
    }
    else
      false
  }

  /**
   * Detach this node from its parent (if any)
   */

  def detach: Boolean =
    (parent != null) && parent.detachChild(this)

  /**
   * Remove this node, all descendant nodes, and attached items.
   */

  def remove {
    for (child <- children)
      child.remove
    detach
    Category.manager.remove(this)
  }

  /**
   * Resets the order field for child Categories so as to
   * make space for a new child, or to remove an existing
   * child.
   */

  private def reorderChildren(start: Int, offset: Int) {
    val categories = children
    println("children: " + categories.toList)
    println("start: " + start + ", end = " + (categories.length - 1))
    for (index <- start to categories.length - 1) {
      val child = categories(index)
      println("renum " + child + " index := " + (index + offset))
      child.index = index + offset
    }
  }

  /**
   * Returns true if this Category contains a specified Item.
   */

  def containsItem(item: Item): Boolean =
    item_references != null && item_references.contains(item)

  /**
   * Adds an Item to this Category.
   */

  def addItem(item: Item): Boolean =
    if (containsItem(item))
      false
    else {  
      item_references.add(item)
      item.category_references.add(this)
      true
    }

  /**
   * Removes an Item from this category.
   */

  def removeItem(item: Item): Boolean = {
    item.category_references.remove(this)
    item_references.remove(item)
  }

  /**
   *  Returns an array of items in this Category.
   */

  def items: Array[Item] =
    if (item_references == null)
      Array()
    else
      item_references.toArray map (_.asInstanceOf[Item])

  /**
   *     Returns a JSON rendering of this node.
   */

  def toJSON: JSONObject =
    if (count > 0)
      Properties(
        "class" -> classOf[Category].getCanonicalName,
        "id" -> id, "name" -> name, "children" -> children).toJSON
    else
      Properties(
        "class" -> classOf[Category].getCanonicalName,
        "id" -> id, "name" -> name
        ).toJSON

  /**
   *  Returns a string representation of this Category.
   */

  override def toString: String = toJSON.toString
}

object Category extends Storage[Category] {

  def create(name: String, children: Array[Category]) = {
    val category = new Category()
    persist(category)

    category.name = name
    category.created = new Date()
    category.updated = new Date()

    children.foreach(category.add(_))
    category
  }

  /**
   * Creates a new Category, or nested tree of Category objects.
   */

  def apply(name: String, children: Category*): Category =
    create(name, Array(children: _*))

  /**
   * Find a category with a specified Taxonomy and name.
   */

  def find(taxonomy: Taxonomy, name: String): Category = {
    val query = Category.manager.createQuery(
      "SELECT c FROM Category c " +
              "WHERE c.taxonomy = :taxonomy AND c.name = :name")
    query.setParameter("taxonomy", taxonomy)
    query.setParameter("name", name)
    try {
      query.getSingleResult.asInstanceOf[Category]
    }
    catch {
      case _: NoResultException =>
        throw new NotFoundException(
          "Missng Category: " + taxonomy + ":" + name)
    }
  }

  /**
   * Finds a category for a specified category id in the form
   * "taxonomy:category".
   */

  def find(categoryId: String): Category = {
    categoryId.split(":") match {
      case Array(taxonomyName, categoryName) =>
        find(Taxonomy.find(taxonomyName), categoryName)
      case _ =>
        throw new NotFoundException("Missing Category[2]: " + categoryId)
    }
  }

}