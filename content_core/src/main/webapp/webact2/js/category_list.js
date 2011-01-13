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
 	 	
 	category_list.makeCategoryList = function (options) {
 	
 		var control = makeControl(options);
 		
 		var import_panel = null;
 		
 		var category_panel = null;
 		
 		control.class_name = "CategoryList";

		var categoryMode = function () {
			import_panel.hide();
			category_panel.show();
		}
		
		var importMode = function () {
			category_panel.hide();
			import_panel.show();
		}
 		
 		var generateImportMode = function (dom_element) {
 			import_panel = jQuery("<div/>", {"class":"wa_clist_import"});
 			dom_element.append(import_panel);
 			 			
 			var import_button = makeButton({label: "Import...", css: "wa_clist_import_button"});
 			import_button.create(import_panel);
 			import_button.addListener("changed", categoryMode);
 		};
 		
 		var generateCategoryMode = function (dom_element) {
			category_panel = jQuery("<div/>", {"class":"wa_clist_categories"});
			dom_element.append(category_panel);
						
			var no_import_button = makeButton({label: "No Import", css: "wa_clist_no_import_button"});
			no_import_button.create(category_panel);
			no_import_button.addListener("changed", importMode);
			
			var label = jQuery("<span/>", {
				text: "Import As:", 
				"class": "wa_clist_label"
			});
			category_panel.append(label);
			
			var display = jQuery("<div/>", {
				"class": "wa_clist_display",
				text: "Imported Items"
			});
			category_panel.append(display);
			
			var input = jQuery("<input/>", {
				"type": "text",
				"class": "wa_clist_input"
			});
			category_panel.append(input);
			
			var add_button = makeButton({label: "Add", css: "wa_clist_add_button"});
			add_button.create(category_panel);
			
			category_panel.hide();
 		};
 		
		control.generate = function (container) {
		 	var dom_element = jQuery("<div/>", {"class": "wa_clist"});
		 	container.append(dom_element);
		 	if (options.css)
		 		dom_element.addClass(options.css);
		 	dom_element.data("_control", this);
		 	
		 	generateImportMode(dom_element);
		 	generateCategoryMode(dom_element);
		 	
		 	return dom_element;
		 }
 		
 		return control;
 	};	
 	
 });