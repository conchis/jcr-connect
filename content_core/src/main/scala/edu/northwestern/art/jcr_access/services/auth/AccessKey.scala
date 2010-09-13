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

import java.util.Date

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
 
@Entity
class AccessKey(url: String) {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int = 0

  val date: Date = new Date
 
  def getId(): Int = {
    return id
  }
 
  def setId(id: Int) = {
    this.id = id
  }
 
  def getURL(): String = {
    return url
  }
 
  // def setURL(url: String) = {
  //   this.url = url
  // }
}
