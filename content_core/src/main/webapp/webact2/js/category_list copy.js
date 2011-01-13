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
 * @version 12 October 2010
 */
 
 webact.in_package("category_list", function (category_list) {
 
 	eval(webact.imports("controls"));
 	eval(webact.imports("buttons"));
 	eval(webact.imports("dialogs"));
 	
 	var makeCategoryDialog = function (options) {
 		
 		options.ok_label = "Save";
 		var dialog = makeDialog(options);
 		
 		var source_categories = options.source_categories || [];
 		var categories = options.categories || [];
 		
 		var source_list = null;
 		var category_list = null;
 		
 		var update = function () {
 			replaceListItems(category_list, categories);	 			
			replaceListItems(source_list, source_categories);
 		}
 		
 		var captureCategories = function () {	
 			categories = getListItems(category_list);
 			source_categories = getListItems(source_list);
 		}
 		
 		var addCategory = function (category) {
 			console.log("adding: " + category);
 		
 			captureCategories();
 			// If empty, return
 			if (category == "") return;
 			
 			// If there, do not add
 			for (var index = 0; index < categories.length; index += 1) {
 				if (categories[index] == category) return;
 			}
 			// If in source categories, remove
 			for (var index = 0; index < source_categories; index += 1) {
 				if (source_categories[index] == category) {
 					source_categories.splice(index, 1);
 					index -= 1;
 				}
 			}
 			// Push the new category
 			categories.push(category);
 			// Update
 			update();
 		}
 		
 		var getListItems = function (list) {
 			var found = [];
 			jQuery("li", list).each(function (index, item) {
 				found.push(jQuery(item).text());
 			});
 			console.log("found:", found);
 			return found;
 		}
	
 		var replaceListItems = function (list, items) {
 			list.empty();
 			for (var index = 0; index < items.length; index += 1) {
 				var item_element = jQuery("<li/>", {text: items[index]});
 				list.append(item_element);
 			}	
 		}
 		
		var overriden_generate = dialog.generate;
		dialog.generate = function (container) {
			var dom_element = overriden_generate(container);
			
			var status_line = jQuery("<div/>", {
				"class": "wa_clist_status",
				text: "Select categories for this item:"
			});
			dom_element.append(status_line);
			
			category_list = jQuery("<ul/>", {
				"class": "wa_clist_cats"
			});
			dom_element.append(category_list);
			category_list.sortable({
				connectWith: ".wa_clist_source",
				scroll: false, 
				helper: "clone",
				appendTo: ".wa_dialog"});
			
			var middle_panel = jQuery("<div/>", {
				"class": "wa_clist_middle"
			});
			dom_element.append(middle_panel);
			middle_panel.append("&#8593;&#8593; To add drag categories up");
			
			var remove_button = makeButton({
				label: "Remove",
				css: "wa_clist_remove_button"
			});
			remove_button.create(middle_panel);
			
			source_list = jQuery("<ul/>", {
				"class": "wa_clist_source"
			});
			dom_element.append(source_list);	
			source_list.sortable({
				connectWith: ".wa_clist_cats", // ".wa_cselect_categories"
				scroll: false, 
				helper: "clone",
				appendTo: ".wa_dialog"});
	
			var bottom_panel = jQuery("<div/>", {
				"class": "wa_clist_bottom"
			});
			dom_element.append(bottom_panel);
			
			var category_field = jQuery("<input/>", {
				"class": "wa_clist_input"
			});
			bottom_panel.append(category_field);
			
			var add_button = makeButton({
				css: "wa_clist_add_button",
				label: "New Category"
			});
			add_button.create(bottom_panel);
			add_button.addListener("changed", function () {
				addCategory(category_field.val());
				category_field.val("");
			});
			
			update();
			
			return dom_element;
		}
 		
 		return dialog;
 	}
 	
 	category_list.makeCategoryList = function (options) {
 	
 		var control = makeControl(options);
 		
 		var display_panel = null;
 		
 		var category_dialog = null;
 		
 		control.class_name = "CategoryList";
 		
 		var openDialog = function () {
 			category_dialog.show();
 		}
 		
 		var generateButtons = function (dom_element) {
 			var button_panel = jQuery("<div/>", {
 				"class": "wa_clist_buttons"
 			});
 			dom_element.append(button_panel);
 			
 			display_panel = jQuery("<div/>", {
 				"class": "wa_clist_display",
 				text: "Press \"Import...\" to import item"});
 			dom_element.append(display_panel);
 		
 			var save_button = makeButton({
 				label: "Import...",
 				"class": "wa_clist_import_button"
 			});
 			save_button.create(button_panel);
 			save_button.addListener("changed", openDialog);
 		}
 		
 		var generateDialog = function (dom_element) {
 			category_dialog = makeCategoryDialog({
 				categories: [],
 				source_categories: ["Chicago Waterfront", "Fire Fighters", "Water Tower"]
 			});
 			category_dialog.create(dom_element);
 			category_dialog.hide();
 		}
 		
		control.generate = function (container) {
		 	var dom_element = jQuery("<div/>", {"class": "wa_clist"});
		 	container.append(dom_element);
		 	if (options.css)
		 		dom_element.addClass(options.css);
		 	dom_element.data("_control", this);
		 	
		 	generateButtons(dom_element);
		 	generateDialog(dom_element);
		 	return dom_element;
		 }
 		
 		return control;
 	};	
 	
 });