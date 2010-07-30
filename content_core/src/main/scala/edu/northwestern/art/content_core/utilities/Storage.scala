/**
 * Copyright 2010 Northwestern University. Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * @author Jonathan A. Smith
 * @version 13-07-2010 2:29:10 PM
 */

package edu.northwestern.art.content_core.utilities

import javax.persistence.{EntityManagerFactory, EntityManager, Persistence}

class Storage[T](implicit manifest: Manifest[T]) {

  /** Class object for class T */
  val intanceClass = manifest.erasure

  /** Returns the current EntityManager */
  def manager = Storage.manager

  /**
   * Persists an instance of class T
   *
   * @param instance instance of T to be persisted
   */

  def persist(instance: T) = manager.persist(instance)

  /**
   * Returns a Some(value): Option[T] object associated with a specified id or
   * None if no record is found with that id.
   *
   * @return Option[StatusRecord]
   */

  def find(id: Int): Option[T] = {
    manager.find(intanceClass, id).asInstanceOf[T] match {
      case null  => None
      case value => Some(value)
    }
  }

}

object Storage {

  /** EntityManager for persistant storage. */
  private var entity_manager: EntityManager = null

  /** EntityManagerFactory to create the EntityManager */
  private var entity_factory: EntityManagerFactory = null

  /** Unit name used for persistent storage. */
  private var unit_name: String = null

  /**
   * Initializes Storage to access a specified persistence unit.
   *
   * @param unit_name persistence unit
   */

  def unit(unit_name: String) {
    if (unit_name != this.unit_name) {
      if (entity_manager != null)
        throw new ApplicationException("Attempt to reset Storage unit name " + unit_name)
      this.unit_name = unit_name

      entity_factory = Persistence.createEntityManagerFactory(unit_name)
      entity_manager = entity_factory.createEntityManager()
    }
  }

  /**
   * Provides read-only access to the entity manager.
   *
   * @return EntityManager
   */

  def manager =
    if (entity_manager != null)
      entity_manager
    else
      throw new ApplicationException("Storage not initialized")

  /**
   * Executes a closure block within a transaction. Rolls back the transaction
   * if any exception is thrown during execution.
   *
   * @param work closure block to be executed during the transaction
   */

  def transaction[T] (work: => T): T = {
    try {                                                                    
      manager.getTransaction.begin
      val result = work
      manager.getTransaction.commit
      result
    }
    catch {
      case except: Throwable =>
        except.printStackTrace
        manager.getTransaction.rollback()
        throw except
    }
  }

  /**
   * Close the EntityManager for this application.
   */

  def close {
    entity_manager.close
    entity_factory.close
  }
}