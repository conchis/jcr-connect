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
 
(function (_) {
  
module("categories_test.js");
 
eval(webact.imports("categories"));
 
test("makeCategory", function () {
	var c1 = makeCategory({id: "C1", title: "Category 1"});
	equals(c1.getId(), "C1");
	equals(c1.getTitle(), "Category 1");
});

test("add", function () {
	var c1 = makeCategory({id: "C1", title: "Category 1"});
	var c2 = makeCategory({id: "C2", title: "Category 2"});
	var c3 = makeCategory({id: "C3", title: "Category 3"});
	
	c1.add(c2);
	c1.add(c3);
	
	ok(c2.getParent() === c1, "c2 parent");
	ok(c3.getParent() === c1, "c3 parent");
	
	var children = c1.getChildren();
	equals(children.length, 2);
	ok(children[0] === c2, "c2 child");
	ok(children[1] === c3, "c3 child");
});

test("add at index", function () {
	var c1 = makeCategory({id: "C1", title: "Category 1"});
	var c2 = makeCategory({id: "C2", title: "Category 2"});
	var c3 = makeCategory({id: "C3", title: "Category 3"});
		
	c1.add(c2);
	c1.add(c3, 0);	
	
	var children = c1.getChildren();
	equals(children.length, 2);
	ok(children[0] === c3, "c3 child");
	ok(children[1] === c2, "c2 child");	
});

test("move to new index", function () {
	var c1 = makeCategory({id: "C1", title: "Category 1"});
	var c2 = makeCategory({id: "C2", title: "Category 2"});
	var c3 = makeCategory({id: "C3", title: "Category 3"});
	var c4 = makeCategory({id: "C4", title: "Category 4"});
	var c5 = makeCategory({id: "C5", title: "Category 5"});
	
	c1.add(c2);
	c1.add(c3);
	c1.add(c4);
	c1.add(c5);
	
	c1.add(c4, 0);
	var children = c1.getChildren();
	equals(children.length, 4);
	ok(children[0] === c4, "c4 child");
	
	c1.add(c4, 3);
	children = c1.getChildren();
	equals(children.length, 4);
	ok(children[2] === c4, "c4 child 2");
});

test("index of node in parent", function () {
	var c1 = makeCategory({id: "C1", title: "Category 1"});
	var c2 = makeCategory({id: "C2", title: "Category 2"});
	var c3 = makeCategory({id: "C3", title: "Category 3"});
	
	c1.add(c2);
	c1.add(c3);

	equals(c1.indexOfChild(c2), 0);
	equals(c1.indexOfChild(c3), 1);
	
	equals(c1.getIndex(), -1);
	equals(c2.getIndex(), 0);
	equals(c3.getIndex(), 1);
});

test("make category tree", function () {
	var c1 = makeTree({
		id: "C1",
		title: "Category 1",
		children: [
			{id: "C2", title: "Category 2"},
			{id: "C3", title: "Category 3"},
		]
	});
	
	equals(c1.getId(), "C1");
	equals(c1.getTitle(), "Category 1");
	var children = c1.getChildren();
	equals(children.length, 2);
	
	var c2 = children[0];
	equals(c2.getId(), "C2");
	equals(c2.getTitle(), "Category 2");
	
	var c3 = children[1];
	equals(c3.getId(), "C3");
	equals(c3.getTitle(), "Category 3");	
});
 
})();