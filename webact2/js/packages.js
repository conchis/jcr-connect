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
 
// Establishes the webact object, the root object for all webact packages.
 
// This file implements a very simple package system that is used to allow
// each package to create and use a separate namespace without requiring
// that all names be fully qualified.

// This file must be loaded after jQuery and before any other webact packages.

webact = this.webact || {};
(function (webact, $) {

    // The create function constructs a new object from a specified prototype.
    // This is similar to the create method from p22 of "JavaScript The 
    // Good Parts" by Douglas Crawford, O'Reilly, 2008. This version lets the 
    // caller initialize members of the new object. The initial parameter is 
    // optional.

    var create = function (proto, initial) {
        var constructor = function () {}; 
        constructor.prototype = proto; 
        var result = new constructor();
        if (initial !== undefined) {
            for (var name in initial) {
                if (initial.hasOwnProperty(name))
                    result[name] = initial[name];
            }
        }
        return result;
    };
    webact.create = create;
    
    // Package Prototype
    
    var Package = {  
        name: null,
     
        // Returns all the names exported by this package
        
        names: function () {
            var all_names = [];
            for (var name in this) {
                if (this.hasOwnProperty(name) && name != "name")
                    all_names.push(name);
            }
            return all_names;
        },
        
        // Returns an experession, when evaluated, will import all of
        // the exported names from this package into a local scope.
        
        exports: function () {
            if (this.name === null)
                throw new Error("Package name not set");
            var names = this.names();
            var result = []; 
            for (var index = 0; index < names.length; index += 1) {
                var name = names[index];
                result.push("var " + name + "=webact." + this.name + "." + name);
            }
            
            return result.join(";");
        }
    };

	// Opens a package for additions. Note that this may be done any number
	// of times to add new names. All exported names should be set in the 
	// exports argument to the body function.

	webact.in_package = function (name, body) {
	    // Find existing package, or create new from prototype
		var package_object = webact[name] || null;
		if (package_object === null) {
		    package_object = webact.create(Package, {name: name});
		    webact[name] = package_object;
		}
		
		// Execute the package body if any
		if (body !== undefined)
		    body(package_object);
		    
		// Return the package object
		return package_object;
	};
	
	// Returns an experession that, when evaluated, imports all of 
	// the exported symbols in all of the named packages into the
	// local scope. Arguments are package names.
	
	webact.imports = function () {
	    var result = [];
	    for (var index = 0; index < arguments.length; index += 1) {
	        var name = arguments[index];
	        var pack = webact[name];
	        if (pack === undefined)
	            throw new Error("Undefined package: " + name);
	        result.push(pack.exports());
	    }
	    return result.join(";");
	};
	
})(webact, jQuery);