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
 
(function (_) {
 
module("controls_test.js");

eval(webact.imports("controls"));

test("id specified in options", function () {	
	var c1 = makeControl({id: "c1"});
	equals(c1.getId(), "c1");
	equals(c1.id, "c1");	
});

test("id generation by class name", function () {
	var c1 = makeControl();
	equals(c1.getId(), "Control_0");
	equals(c1.id, "Control_0");
	
	var c2 = makeControl();
	equals(c2.getId(), "Control_1");
	equals(c2.id, "Control_1");
	
	var c3 = makeControl();
	c3.class_name = "Widget";
	equals(c3.getId(), "Widget_0");
	equals(c3.id, "Widget_0");	
	
	var c4 = makeControl();
	c4.class_name = "Widget";
	equals(c4.getId(), "Widget_1");
	equals(c4.id, "Widget_1");	
});

test("nested controls", function () {
	var c1 = makeControl({id: "c1"});
	var c2 = makeControl({id: "c2"});
	var c3 = makeControl({id: "c3"});
	
	c1.add(c2);
	ok(c2.parent === c1, "c2 parent set");
	equals(c1.contents.indexOf(c2), 0);
	
	c1.add(c3);
	ok(c3.parent === c1, "c3 parent set");
	equals(c1.contents.indexOf(c3), 1);
	
	c2.detach();
	equals(c2.parent, null);
	equals(c1.contents.indexOf(c2), -1);
	equals(c1.contents.indexOf(c3), 0);
	
	c3.detach();
	equals(c3.parent, null);
	equals(c1.contents.indexOf(c3), -1);
	equals(c1.contents.length, 0);
});

})();