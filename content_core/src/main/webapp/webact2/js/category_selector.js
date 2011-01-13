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
 
 webact.in_package("category_selector", function (category_selector) {
 
 	eval(webact.imports("controls"));
 	eval(webact.imports("category_tree"));
 	eval(webact.imports("buttons"));
 
	category_selector.makeCategorySelector = function (options) {
	
		var control = makeControl(options);
		
		var category_tree = null;
		
		var selection_list = null;
		
		var remove_button = null;
		
		var add_button = null;
		
		var cancel_button = null;
		
		var save_button = null;
	
		control.class_name = "CategorySelector";
			
		control.generate = function (container) {
			var dom_element = jQuery("<div/>", {
				id: this.getId(),
				"class": "wa_cselect"
			});
			container.append(dom_element);
			
			category_tree = makeCategoryTree({model: options.model, dest: "wa_cselect_categories"});
			category_tree.create(dom_element);
			
			dom_element.append(
				"<span class=\"wa_cselect_label\">&#8595;&#8595; Drop categories below</span>");
			
			selection_list = jQuery("<ul>", {
				"class": "wa_cselect_categories"
			});
			dom_element.append(selection_list);
			selection_list.sortable();
			
			var button_panel = jQuery("<div/>", {"class": "wa_cselect_buttons"});
			dom_element.append(button_panel);
			
			remove_button = makeButton({label: "Remove", css: "wa_cselect_remove"});
			remove_button.create(button_panel);
			
			add_button = makeButton({label: " + New Category", css: "wa_cselect_new"});
			add_button.create(button_panel);
			
			cancel_button = makeButton({label: "Cancel", css: "wa_cselect_cancel"});
			cancel_button.create(button_panel);
			
			save_button = makeButton({label: "Save", css: "wa_cselect_save"});
			save_button.create(button_panel);
			
			return dom_element;
		}
		
		return control;
	}
});