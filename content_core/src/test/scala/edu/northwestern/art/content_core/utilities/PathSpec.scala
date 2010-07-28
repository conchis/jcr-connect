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
 * @author Jonathan A. Smith
 * @version 14 July 2010
 */

package edu.northwestern.art.content_core.utilities

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PathSpec extends FlatSpec with ShouldMatchers {

  "A Path" should "provide an array of path name tokens" in {
    val t1 = Path("a/b/c").tokens

    t1.length should equal(3)
    t1(0) should equal("a")
    t1(1) should equal("b")
    t1(2) should equal("c")
  }

  "A Path" should "allow other paths to be joined into a new path" in {
    val p1 = Path("/x/y/z") ++ Path("d/e/f")
    p1.toString should equal("/x/y/z/d/e/f")
  }

  "A Path" should "provide a normalized representation" in {
    Path("hello/world").normalized.toString should equal("hello/world")
    Path("hello/./world").normalized.toString should equal("hello/world")
    Path("a/x/y/../../b/c").normalized.toString should equal("a/b/c")
    Path("a//b//c/d").normalized.toString should equal("a/b/c/d")
  }

  "A Path" should "provide the name (last token)" in {
    Path("x/y/sogood").name should equal("sogood")
    Path("my/favorite/hello.mp4").name should equal("hello.mp4")
  }

  "A Path" should "provide access to the path of its parent directory" in {
    Path("a/b/c/hello").parent.toString should equal("a/b/c")
    Path("/").parent.toString should equal("/")
  }

}