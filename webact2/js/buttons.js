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
 * @version 3 August 2010
 */
 
 webact.in_package("buttons", function (buttons) {
 
 	eval(webact.imports("controls"));
 	
 	buttons.makeButton = function (options) {
 	 
  		var control = makeControl(options);
  		
  		var is_pressed = false;
  		
  		var is_down = false;
  		
  		var is_active  = options.active;
  		if (is_active === undefined) is_active = true;
  		
  		var toggles = options.toggles || false;
  		
  		control.setPressed = function (pressed) {
            // If no change, return
            if (pressed == is_pressed) return;

            // Adjust button state, update
            is_pressed = pressed;
            is_down = pressed;
            update();

            // Broadcast changed state
            this.broadcast("changed", is_pressed);
  		}
  		
  		control.setActive = function (active) {
			if (is_active != active) {
			    is_active = active;
			    update();
			}
  		}
  		
  		var toggle = function () {
			is_down = !is_down;
			update();
  		}
  		
  		var action = function () {
            // Return if not active
            if (!is_active) return;

    		if (toggles) {
    			is_pressed = !is_pressed;
    			control.broadcast("changed", is_pressed);
    		}
    		else {
    			is_pressed = true;
    			control.broadcast("changed", is_pressed);
    			is_pressed = false;
    		}
    		is_down = is_pressed;
    		update();
  		}
  		
		var update = function () {
			var dom_element = control.dom_element;
			if (!is_active)
				dom_element.addClass('wa_button_grey');
			else {
				dom_element.removeClass('wa_button_grey');
				if (is_down)
					dom_element.addClass('wa_button_pressed');
				else
					dom_element.removeClass('wa_button_pressed');
			}
		}
		control.update = update;
  			
  		control.generate = function (container) {
  			var dom_element = jQuery("<span/>", {
  				"class": "wa_button",
  				text: options.label,
  				mousedown: toggle,
  				mouseup: action
  			});
  			container.append(dom_element);
  			dom_element.data("_control", this);
  			return dom_element;
  		}
  		
  		control.onPressed = function () {
  			console.log("pressed");
  		}
  		
  		return control;
  	}
 	
 });