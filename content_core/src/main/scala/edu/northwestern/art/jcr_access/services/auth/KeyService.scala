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
 * @author Xin Xiang
 */

package edu.northwestern.art.jcr_access.services.auth

import java.io.PrintWriter

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence
import javax.persistence.EntityTransaction
import javax.persistence.RollbackException
import javax.persistence.Query


class KeyService extends HttpServlet {

  override def doGet(req: HttpServletRequest, res: HttpServletResponse) = {
    val out: PrintWriter = res.getWriter

    val emf: EntityManagerFactory = Persistence.createEntityManagerFactory("key")
    val em: EntityManager = emf.createEntityManager
    val tx: EntityTransaction = em.getTransaction

    val key = new AccessKey

    try {
      // tx = em.getTransaction
      tx.begin

      // Persist the entity
      em.persist(key)	
      tx.commit
    } catch {
      case e: RollbackException =>
        if (tx != null)
          tx.rollback
    }

    out.println(key.getId + ":" + key.getKey)
    out.close
  }
}
