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
 
// Observers 

webact.in_package("observers", function (observers) {

// Borrowing the slice method for function arguments

var slice = Array.prototype.slice;

// Mixes in broadcaster parts.This implementation of the observer pattern 
// is designed to allow a listener to relay a broadcast to other listeners.

observers.mixinBroadcaster = function (that) {

    var listener_map = {};

    // Adds a listener associated with a specified selector and listener
    // method. The listener argument may be either a function to be called
    // when a broadcast is received, or an object. If the listener argument
    // is an object, a broadcast will cause the named method to be called
    // on the object. If no method_name is specified, a method with the same
    // same as the selector will be called.

    that.addListener = function (selector, listener, method_name) {
		// Get listeners array for the selector. If none found,
		// create a new one.
        var listeners = listener_map[selector];
        if (listeners === undefined) {
            listeners = [];
            listener_map[selector] = listeners;
        }

        // Add listener function
        if (typeof(listener) == "function")
            listeners.push(listener);
        else {
        	if (method_name === undefined)
        		method_name = selector;
            listeners.push(function () {
            	listener[method_name].apply(listener, arguments);
            });
        }
    }

    // Removes a specified listener. Note this only works for
    // functions.

    that.removeListener = function (selector, listener) {
    
        var listeners = listener_map[selector];
        if (listeners === undefined)
            return false;

        var index = listeners.indexOf(listener);
        if (index < 0) return false;

        listeners.splice(index, 1);
        return true;
    },

    // Tests if the broadcaster has a specified listener.

    that.hasListener = function (selector, listener) {
    	// Get listeners for selector
        var listeners = listener_map[selector];
        if (listeners === undefined)
            return false;

		// Find listener in array
        return listeners.indexOf(listener) >= 0;
    }
    
    // Broadcasts a message to all listeners

    that.sendBroadcast = function (selector, argument_list, source) {
    	// Set default source
        if (source === undefined) source = this;

        // Inform listeners for the specified selector
        var listeners = listener_map[selector];
        if (listeners === undefined) return;
        
        var listener_arguments = argument_list.slice();
        listener_arguments.push(source);
        for (var index = 0; index < listeners.length; index += 1)
            listeners[index].apply(null, listener_arguments);
    }

    // Broadcasts a message to all listeners

    that.broadcast = function (selector) { // Other arguments            
        that.sendBroadcast(selector, slice.call(arguments, 1));
    }

};

// Make an object that functions as a broadcaster.

observers.makeBroadcaster = function () {
    var that = {};
    observers.mixinBroadcaster(that);
    return that;
}
    
});