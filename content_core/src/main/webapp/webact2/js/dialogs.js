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
 
 webact.in_package("dialogs", function (dialogs) {
 
 	eval(webact.imports("controls"));
 	eval(webact.imports("buttons"));
 	
 	dialogs.makeDialog = function (options) {
 	
 		var control = makeControl(options);
 		
 		var ok_label = options.ok_label || "Ok";
 		
 		var close = function () {
 			control.hide();
 		}
 		
 		var cancel = function () {
 			control.hide();
 		}
 		
 		var generateButtons = function (dom_element) {
 			var button_panel = jQuery("<div/>", {
 				"class": "wa_dialog_buttons"
 			});
 			dom_element.append(button_panel);
 		
 			var cancel_button = makeButton({label: "Cancel"});
 			cancel_button.create(button_panel);
 			cancel_button.addListener("changed", cancel);
 			
 			var ok_button = makeButton({label: ok_label});
 			ok_button.create(button_panel);
 			ok_button.addListener("changed", close);
 		}
 		
		 control.generate = function (container) {
		 	var dom_element = jQuery("<div/>", {
		 		"class": "wa_dialog"
		 	});
		 	container.append(dom_element);
		 	if (options.css)
		 		dom_element.addClass(options.css);
		 	dom_element.data("_control", this);
		 	
		 	generateButtons(dom_element);
		 	return dom_element;
		 }
 		
 		return control;
 	};	
 	
 });