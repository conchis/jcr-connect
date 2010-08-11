/** 
 *Copyright 2010 Northwestern University.
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
 * @version 11 [08] 2010
 */

package edu.northwestern.art.content_core.content2

import edu.northwestern.art.content_core.properties.Properties
import org.json.JSONObject

object JSONVisitor extends ContentVisitor[JSONObject] {

  override def visitTaxonomy(taxonomy: Taxonomy) =
    Properties(
      "name" -> taxonomy.name,
      "root" -> taxonomy.root
    ).toJSON

  override def visitCategory(category: Category) =
    Properties(
      "name"        -> category.name,
      "description" -> category.description,
      "children"    -> category.children
    ).toJSON

  override def visitItem(item: Item) =
    Properties(
      "name"        -> item.name,
      "metadata"    -> item.metadata,
      "categories"  -> item.categories,
      "created"     -> item.created,
      "modified"    -> item.modified
    ).toJSON

  override def visitMetadata(metadata: Metadata) =
    Properties(
      "title"       -> metadata.title,
      "description" -> metadata.description,
      "creators"    -> metadata.creators,
      "rights"      -> metadata.rights,
      "date"        -> metadata.date
    ).toJSON

  override def visitImageURL(source: ImageURL) =
    if (source.width == 0 && source.height == 0)
      Properties(
        "name"      -> source.name,
        "format"    -> source.format,
        "url"       -> source.url
      ).toJSON
    else   
      Properties(
        "name"      -> source.name,
        "format"    -> source.format,
        "width"     -> source.width,
        "height"    -> source.height,
        "url"       -> source.url
      ).toJSON

  override def visitTiledImage(source: TiledImage) =
    if (source.width == 0 && source.height == 0)
      Properties(
        "name"      -> source.name,
        "format"    -> source.format,
        "url"       -> source.url
      ).toJSON
    else
      Properties(
        "name"      -> source.name,
        "format"    -> source.format,
        "width"     -> source.width,
        "height"    -> source.height,
        "url"       -> source.url
      ).toJSON

}