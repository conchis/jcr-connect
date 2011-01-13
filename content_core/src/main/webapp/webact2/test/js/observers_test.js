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

(function () {

module("observers_test.js");

eval(webact.imports("observers"));

test("make broadcaster", function () {
	var b = makeBroadcaster();
	var caught = false;
	b.addListener("onChange", function () { caught = true; });
	ok(!caught, "!caught");
	b.broadcast("onChange");
	ok(caught, "caught");
});

test("method_call", function () {
	var caught = false;
	
	var listener = {
	    caught: false,
	    
	    onChange: function () {
		    this.caught = true;
	    }
	};

	var b = makeBroadcaster();
	b.addListener("onChange", listener);

	ok(!listener.caught, "!caught");

	b.broadcast("onChange");
	ok(listener.caught, "caught");
});

test("named_method_call", function () {
    var listener = {
        caught: false,
        
        respond: function () {
    	    this.caught = true;
        }
    };
	var b = makeBroadcaster();
	b.addListener("onChange", listener, "respond");

	ok(!listener.caught, "!caught");

	b.broadcast("onChange");
	ok(listener.caught, "caught");
});

test("has listener / remove listener", function () {
	var caught = false;
	var fn = function () { caught = true; };

	var b = makeBroadcaster();
	b.addListener("onChange", fn);

	ok(b.hasListener("onChange", fn), "has");

	b.removeListener("onChange", fn);

	ok(!b.hasListener("onChange", fn), "!has");
});

})();