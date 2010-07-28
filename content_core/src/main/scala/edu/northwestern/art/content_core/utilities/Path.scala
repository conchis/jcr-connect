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


class Path(val tokens: Array[String] = Array()) {

  def isRoot = tokens.length match {
    case 1 => tokens(0) == ""
    case 2 => tokens(0) == "" && tokens(1) == ""
    case _ => false
  }

  /**
   * Returns a new path appending two paths.
   *
   * @param path path to be appended
   */

  def ++(path: Path): Path = new Path(tokens ++ path.tokens)

  /**
   *  Returns the last token of the path.
   */

  def name = tokens.length match {
    case 0 => throw new EmptyPathException
    case _ => tokens(tokens.length - 1)
  }

  /**
   * Returns a new path without the last token of the path.
   */

  def parent = {
    val normalized_path = normalized
    if (normalized_path.isRoot)
      normalized_path
    else {
      val normal_tokens = normalized_path.tokens
      new NormalizedPath(normal_tokens.slice(0, normal_tokens.length - 1))
    }
  }

  /**
   * Returns a new path where any "." token is removed, and any ".." token
   * will cause the prior token to be ignored. Note that an
   * InvalidPathException is thrown if there is no (non "." and "..") token
   * remaining to be removed before the ".." token.
   *
   * @return normalized path
   */

  def normalized = {

    def scan(remaining: List[String], seen: List[String]): List[String] =
      remaining match {
        case "." :: others =>
          scan(others, seen)
        case ".." :: others =>
          if (seen.isEmpty)
            throw new InvalidPathException("Invalid \"..\" in path: " + toString)
          else
            scan(others, seen.tail)
        case "" :: others if !seen.isEmpty =>
          scan(others, seen)
        case token :: others =>
          scan(others, token :: seen)
        case _ =>
          seen.reverse
      }

    new NormalizedPath(scan(tokens.toList, List()).toArray)
  }

  /**
   * Determines if this path indicates the same or a location inside
   * another path.
   *
   * @param other Other path for comparison
   * @return true only if this path is the same as or within other
   */

  def in(other: Path): Boolean = {
    val this_tokens = normalized.tokens
    val other_tokens = other.normalized.tokens

    this_tokens.length <= other_tokens.length &&
      other_tokens.slice(0, this_tokens.length) == this_tokens
  }

  override def toString =
    if (isRoot)
      "/"
    else
      tokens.mkString("/")
}

object Path {

  val PathPattern = """^(/)?([\.\w]*/)*([\.\w]*)$""".r

  implicit def stringToPath(path_string: String): Path =
    PathPattern.findFirstIn(path_string) match {
      case Some(_) =>
        if (path_string == "/")
          new Path(Array(""))
        else
          new Path(path_string.split('/'))
      case None =>
        throw new InvalidPathException(path_string)
    }

  implicit def pathToString(path: Path): String =
    path.toString

  def apply(path_string: String) = stringToPath(path_string)
}

class NormalizedPath(tokens: Array[String]) extends Path(tokens) {

  /**
   * Returns a normalized version of the path. Since this path is already
   * normalized, this method returns this.
   *
   * @return this Path
   */

  override def normalized = this
}

class InvalidPathException(message: String)
        extends RuntimeException(message)

class EmptyPathException extends RuntimeException