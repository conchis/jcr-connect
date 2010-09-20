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

import java.security.SecureRandom

import java.math.BigInteger

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Temporal
import javax.persistence.TemporalType

import java.util.Date

@Entity
class AccessKey() {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int = 0

  @Temporal(TemporalType.DATE) 
  val date: Date = new Date

  var url: String = ""

  val key: String = new BigInteger(130, new SecureRandom).toString(32)
 
  def getId(): Int = {
    return id
  }
 
  def setId(id: Int) = {
    this.id = id
  }
 
  def getURL(): String = {
    return url
  }
 
  def setURL(url: String) = {
    this.url = url
  }

  def getKey: String = {
    return key
  }

  def getDate: Date = {
    return date
  }
}
