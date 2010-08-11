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
 * @version 11 August 2010
 */

package edu.northwestern.art.content_core.content2

trait ContentVisitor[T] {

  def visitTaxonomy(taxonomy: Taxonomy): T = throw noMethod(taxonomy)

  def visitCategory(category: Category): T =  throw noMethod(category)

  def visitItem(item: Item): T =  throw noMethod(item)

  def visitMetadata(metadata: Metadata): T =  throw noMethod(metadata)

  def visitImageItem(item: ImageItem): T = visitItem(item)

  def visitImageSource(source: ImageSource): T =  throw noMethod(source)

  def visitImageURL(source: ImageURL): T = visitImageSource(source)

  def visitTiledImage(source: TiledImage): T = visitImageSource(source)

  private def noMethod(target: Object) =
    new ContentVisitorException("Not implemented for " + target.getClass.toString)
}