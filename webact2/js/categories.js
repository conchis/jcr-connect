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
 * @version 1 August 2010
 */
 
// An category provides a way of identifying zero or more items. Category objects 
// form a tree with parents representing super categories, and children representing
// subcategories.

webact.in_package("categories", function (categories) {
	
	// Create a category tree from free form data	
	var makeTree = function (data) {
		var new_category = categories.makeCategory({
			id: data.id || null,
			title: data.title
		});
		var children = data.children || [];
		for (var index = 0; index < children.length; index += 1)
			new_category.add(makeTree(children[index]));
		return new_category;
	}
	categories.makeTree = makeTree;
	
	// Creates a category node. 
	categories.makeCategory = function (options) {
	
		var id = options.id || null;
		var parent = null;
		var children = [];
		var title = options.title;
		
		var that = {};
		
		that.getId       = function () { return id;			}
		that.getParent   = function () { return parent;		}		
		that.getTitle    = function () { return title;		}
		that.getChildren = function () { return children;	}
		
		// Adds this category to a parent. The category is removed from
		// any existing parent, then inserted at the specified index (or
		// at the end of the array of children if no index is specified.	
		that.add = function (child, index) {
			if (index === undefined)
				index = children.length;
			
			var prior_parent = child.getParent();
			if (prior_parent) {
				if (prior_parent === this) {
					var prior_index = indexOfChild(child);
					if (prior_index < index)
						index -= 1;
				}
				prior_parent.remove(child);	
			}	
			
			children.splice(index, 0, child);
			child.addedTo(this);
		}
		
		// Called when this category is added to another category
		that.addedTo = function (new_parent) {
			parent = new_parent;
		}
		
		// Remove a child category, or if child is not specified, removes
		// this node from its parent.                                                                                                                                                         
		that.remove = function (child) {
			if (child) {
				var index = indexOfChild(child);
				if (index >= 0) {
					children.splice(index, 1);
					child.removedFrom(this);
				}
			}
			else
				parent.remove(this);
		}
		
		// Called when a category is removed from its parent.
		that.removedFrom = function (old_parent) {
			if (old_parent !== parent)
				throw new Error("Removed from non-parent");
			parent = null;
		}
		
		// Returns the index of this node in the parent node, or -1 if
		// this is a root node.
		that.getIndex = function () {
			if (parent)
				return parent.indexOfChild(this);
			else
				return -1;
		}
		
		// Returns the count of child nodes under this outline.	
		that.count = function () {
			return children.length;
		};
	
		// Iterates over child categories.
		
		that.each = function (callback, bindings) {
			var children = this.children;
			for (var index = 0; index < children.length; index += 1)
				callback.call(bindings, children[index], index); 
		};
		
		// Returns the index of a child, or -1 if not found
		var indexOfChild = function (child) {
			for (var index = 0; index < children.length; index += 1) {
				if (child === children[index])
					return index;
			}
			return -1;	
		}
		that.indexOfChild = indexOfChild;
				
		return that;
	};
	
});

