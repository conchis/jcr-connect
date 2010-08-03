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

// Provides a base class of visible controls.

webact.in_package("controls", function (controls) {

	eval(webact.imports("observers"));
	
	// Counters by class name, used to generate ids
	var id_counters = {};
	
	// A control provides a convienent base for visual
	// controls. controls:
	//
	// 1. Correspond to a jQuery object called dom_element
	// that is used to quickly find the DOM elements that
	// make up the control.
	//
	// 2. Provide a means for generating ids based on a
	// class identifier and counter.
	//
	// 3. Are broadcasters, so they can broadcast changes
	// to listeners.
	//
	// 4. May contain other controls, possibly in a
	// designated child control.
	
	controls.makeControl = function (options) {
	
		options = options || {};
		var that = makeBroadcaster();
		
		// Class name used to generate ids
		that.class_name = "Control";
		
		// jQuery for finding the dom element corresponding to this control. 
		// Must be returned by the generate method.
		that.dom_element = null;
		
		// jQuery for finding the dom element that contains the generated
		// DOM elements for all contained controls. If null, defaults to
		// dom_element.
		that.dom_contents = null;
		
		// Parent control (if any)
		that.parent = null;
		
		// Array of nested controls (if any)
		that.contents = null;
		
		// *** Id Generation

		// Returns an id for this control, often used to
		// create unique ids for DOM elements. If no id is
		// specified in the control options, and a class
		// name is provided, the value will be the class
		// name followed by a counter.
		
		that.getId = function (suffix) {
			// If an id has been generated, use it
			if (this.id) 
				return this.id;
		
			// If id specified in option, use specified value
			if (options.id) {
				this.id = options.id;
				return this.id;
			}
			
			// Use the class name  counters to generate a new id	
			var class_name = this.class_name;
			var count = id_counters[class_name] || 0;
			id_counters[class_name] = count + 1;
			this.id = this.class_name + "_" + count;			
			return this.id;
		}
		
		// *** Contained Controls
		
		// Adds a child control to this control
		that.add = function (child) {
			if (child.parent != null)
				throw new Error("control added twice");
			this.contents = this.contents || [];
			this.contents.push(child);
			child.parent = this;
			child.addedTo(this);
			
			if (this.dom_element)
				contained.create(this.dom_contents)				
		}
		
		// Called when this control is added to a parent. Override.
		that.addedTo = function (parent) {
		}
		
		// Detaches a control from its parent. If passed a child control
		// detaches it from this control, otherwise removes this from
		// its parent.
		that.detach = function (child) {
			if (child) {
				// Remove HTML if generated
				if (child.dom_element)
					child.remove();
					
				// Remove reference between parent and child
				var children = this.contents;
				for (var index = 0; index < children.length; index += 1) {
					if (children[index] === child) {
						children.splice(index, 1);
						break;
					}
				}
				child.parent = null;
				
				// Tell child it has been removed
				child.detachedFrom(this);
			}
			else
				this.parent.detach(this);
		}
		
		// Called when this control is removed from a parent.
		that.detachedFrom = function (parent) {
			if (this.dom_element)
				this.dom_element.remove();
			this.dom_element = null;
			this.parent = null;
		}
		
		// Should be overriden to add control
		
		// *** HTML Generation
		
		// Generates HTML for the control, returning a new
		// element. It may also set this.dom_contents to another
		// dom element that will contain the HTML for all child
		// controls.
		that.generate = function (container) {
			container.append(dom({id: this.getId()}));
		}
		
		// Override to update control apearance
		that.update = function () {
		}
		
		// Generates HTML for all contained controls. Generated HTML 
		// is appended to this control's dom element.
		var generateContents = function () {
			var contents = this.contents || [];
			var container = this.dom_contents;
			for (var index = 0; index < contents.length; index += 1)
				contents[index].create(container);
		}
		
		// Creates the HTML for this control and all contained controls
		// inside a specified container. Container should be a jQuery.	
		that.create = function (container) {
			if (this.dom_element != null)
				throw new Error("Control already generated: " + this.getId());
			
			this.dom_contents = null;	
			this.dom_element = this.generate(container);
			this.dom_contents = this.dom_contents || this.dom_element;
			generateContents();
			
			this.update();
		}
		
		// The opposite of generate -- this removes all generated HTML
		// from child and this component. Generate may be called at a
		// later time to re-generate HTML for this component.
		that.remove = function () {
			var dom_element = this.dom_element;
			if (dom_element) {
				dom_element.remove();
				this.dom_element = null;
				this.dom_contents = null;
			}
		
		}
		
		// *** Visibility
		
		// Hide this control's element if any
		that.hide = function () {
			if (this.dom_element) this.dom_element.hide();
		}
		
		// Show this control's element if any
		that.show = function () {
			if (this.dom_element) this.dom_element.show();
		}
			
		return that;
	}
});