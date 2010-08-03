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
 
 // Package of functions for generating HTML strings.

webact.in_package("generator", function (generator) {

    // Given a map from attribue names to values, returns a string in HTML
    // format " name1=value1 name2=value2..". If attribute_map has no attribute_map,
    // returns an empty string. Note that the special name 'style' may have
    // an object value, if so it is processed as specified in the 'style'
    // function below.

    var attributes = function (attribute_map) {
        if (attribute_map === undefined || attribute_map == null)
            return "";
        var results = new Array();
        for (var name in attribute_map) {
        	if (name == 'style')
        		results.push("style=\"" + style(attribute_map[name]) + "\"");
        	else if (name == "css")
        		results.push("class=\"" + attribute_map[name].toString() + "\"");
        	else
            	results.push(name + "=\"" + attribute_map[name].toString() + "\"");
        }
        if (results.length == 0)
            return "";
        return " " + results.join(" ");
    }
    
    // Converts a CSS attribute value to a string. Numbers are assumed to be in
    // pixels ('px'). Each value in an array is converted and the result joined
    // with a blank character.
    
    var styleValue = function (value) {
    	if (typeof value == 'number')
    		return value + 'px';
    	else if (typeof(value) == 'object' && typeof(value.length) == 'number') {
    		var collect = [];
    		for (var index = 0; index < value.length; index += 1)
    			collect.push(styleValue(value[index]));
    		return collect.join(' ');
    	}
    	else
    		return value.toString();
    }
    
    // Converts an identified to a style name by replacing all '_' 
    // characters with a dash '-'.
    
    var styleName = function (name) {
    	return name.replace(/_/g, '-');
    }
    
    // Handle object representation of 'style' attribute by creating a CSS
    // string. In general fields become css attributes. Numeric values will
    // have 'px' appended. Array values are joined with a ' ' separator.
    
    var style = function (style_object) {
    	if (typeof(style_object) == 'string')
    		return style_object;
    	var out = [];
    	for (var field in style_object) {
    		var value = styleValue(style_object[field]);
    		out.push(styleName(field) + ':' + value);
    	}
    	return out.join(';');
    }
    generator.style = style;

    // Flatten a nested array, skipping null elements. If the argument
    // is undefined or null, returns an empty array.

    var flatten = function (items) {

        var flattenInto = function (items, results) {
            for (var index = 0; index < items.length; index += 1) {
                var an_item = items[index];
                if (an_item instanceof Array)
                    flattenInto(an_item, results);
                else if (an_item != null)
                    results.push(an_item);
            }
        }

        if (items === undefined || items == null)
            return;

        var results = new Array();
        flattenInto(items, results);
        return results;
    }
    
    // Constructs an HTML element <name attribute1=value1..></name> inserting
    // all strings in contents into the the element.

    var element = function (name, attribute_map, contents) {
        if (contents === undefined || contents.length == 0)
            return "<" + name +  attributes(attribute_map) + "/>";

        var results = new Array();
        results.push("<" + name + attributes(attribute_map) + ">");
        contents = flatten(contents);
        for (var index = 0; index <= contents.length; index += 1)
            results.push(contents[index]);
        results.push("</" + name + ">");
        return results.join("");
    }
    generator.element = element;
    
    // Borrowing the slice method for function arguments
    
    var slice = Array.prototype.slice;

    // Constructs an HTML element <name attribute1=value1..></name> inserting
    // any additional arguments as contents of the element.

    generator.tag = function (name, attribute_map) {
        return element(name, attribute_map, slice.call(arguments, 2));
    }
    
    // **** Containers

    // Constructs a div HTML element <div attribute1=value1..></div> inserting
    // any additional arguments as contents of the element.

    generator.div = function (attribute_map) {
        return element("div", attribute_map, slice.call(arguments, 1));
    }

    // Constructs a span HTML element <span attribute1=value1..></span>
    // inserting any additional arguments as contents of the element.

    generator.span = function (attribute_map) {
        return element("span", attribute_map, slice.call(arguments, 1));
    }
    
    // **** Text & Images

    // Constructs a p HTML element <p attribute1=value1..></p> inserting
    // any additional arguments as contents of the element.

    generator.p = function (attribute_map) {
        return element("p", attribute_map, slice.call(arguments, 1));
    }

    // Constructs a 'a' HTML element <a attribute1=value1..></a> inserting
    // any additional arguments as contents of the element.

    generator.a = function (attribute_map) {
        return element("a", attribute_map, slice.call(arguments, 1));
    }

    // Constructs a br HTML element <br/>.

    generator.br = function () {
        if (arguments.length > 0) throw new Error('Invalid <br/>');
        return element("br");
    }

    // Constructs an img HTML element <img attribute1=value1../>

    generator.img = function (attribute_map) {
        return element("img", attribute_map);
    }
    
    // **** Lists
    
    // Constructs a ul HTML element <ul attribute1=value1..></ul>
    // inserting any additional arguments as contents of the element.
    
    generator.ul = function (attribute_map) {
        return element("ul", attribute_map, slice.call(arguments, 1));
    }
    
    // Constructs an ol HTML element <ol attribute1=value1..></ol>
    // inserting any additional arguments as contents of the element.
    
    generator.ol = function (attribute_map) {
        return element("ol", attribute_map, slice.call(arguments, 1));
    }
    
    // Constructs an li HTML element <li attribute1=value1..></li>
    // inserting any additional arguments as contents of the element.
    
    generator.li = function (attribute_map) {
        return element("li", attribute_map, slice.call(arguments, 1));
    }
    
    // **** Tables

    // Constructs a table HTML element <table attribute1=value1..></table> inserting
    // any additional arguments as contents of the element.

    generator.table = function (attribute_map) {
        return element("table", attribute_map, slice.call(arguments, 1));
    }

    // Constructs a tr HTML element <tr attribute1=value1..></tr> inserting
    // any additional arguments as contents of the element.

    generator.tr = function (attribute_map) {
        return element("tr", attribute_map, slice.call(arguments, 1));
    }

    // Constructs a th HTML element <th attribute1=value1..></th> inserting
    // any additional arguments as contents of the element.

    generator.th = function (attribute_map) {
        return element("th", attribute_map, slice.call(arguments, 1));
    }

    // Constructs a td HTML element <td attribute1=value1..></td> inserting
    // any additional arguments as contents of the element.

    generator.td = function (attribute_map) {
        return element("td", attribute_map, slice.call(arguments, 1));
    }
    
    // Constructs a col HTML element <col attribute1=value1..></col> inserting
    // any additional arguments as contents of the element.

    generator.col = function (attribute_map) {
        return element("col", attribute_map, slice.call(arguments, 1));
    }
    
    // **** Forms

    // Constructs a form HTML element <form attribute1=value1..></form> inserting
    // any additional arguments as contents of the element.

    generator.form = function (attribute_map) {
        return element("form", attribute_map, slice.call(arguments, 1));
    }

    // Constructs a input HTML element <input attribute1=value1..></input> inserting
    // any additional arguments as contents of the element.

    generator.input = function (attribute_map) {
        return element("input", attribute_map, slice.call(arguments, 1));
    }

    // Constructs a textarea HTML element <textarea attribute1=value1..></textarea> inserting
    // any additional arguments as contents of the element.

    generator.textarea = function (attribute_map) {
        return element("textarea", attribute_map, slice.call(arguments, 1));
    }

    // Constructs a select HTML element <select attribute1=value1..></select>
    // inserting any additional arguments as contents of the element.

    generator.select = function (attribute_map) {
        return element("select", attribute_map, slice.call(arguments, 1));
    }

    // Constructs an option HTML element <choice attribute1=value1..></choice>
    // inserting any additional arguments as contents of the element.

    generator.option = function (attribute_map) {
        return element("option", attribute_map, slice.call(arguments, 1));
    }

});