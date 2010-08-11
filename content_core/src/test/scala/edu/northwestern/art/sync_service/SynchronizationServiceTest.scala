/**
 *   Copyright 2010 Northwestern University.
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

package edu.northwestern.art.sync_service

import java.util.Date
import java.text.SimpleDateFormat

import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.header.MediaTypes
import com.sun.jersey.test.framework.JerseyTest
import com.sun.jersey.test.framework.WebAppDescriptor
import org.junit.Assert
import org.junit.Test

class SynchronizationServiceTest() extends JerseyTest (new WebAppDescriptor.Builder("edu.northwestern.art.sync_service").contextPath("cateogry_marker").build()) {

  /**
   * Test that the expected response is sent back.
   * @throws java.lang.Exception
   */
  @Test
  def testSynchronization() = {
    val webResource = resource()
    val responseMsg = webResource.path("sync").get(classOf[String])
    
    var currentTimeMillis = "" + System.currentTimeMillis

    var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    var date = new Date(currentTimeMillis.toLong)
    var date1 = dateFormat.format(date)
    date = new Date(responseMsg.toLong)
    var date2 = dateFormat.format(date)

    Assert.assertEquals(date1, date2)
  }

  @Test
  def testApplicationWadl() = {
    val webResource = resource()
    val serviceWadl = webResource.path("application.wadl").accept(MediaTypes.WADL).get(classOf[String])

    Assert.assertTrue(serviceWadl.length() > 0)
  }
}
