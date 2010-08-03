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

module("paths_test.js");

var paths = webact.paths;

test("join1", function () {
    equals(paths.join("one", "two", "three"), "one/two/three");
    equals(paths.join("one/", "two/", "three"), "one/two/three");
    equals(paths.join("/one", "two/", "three"), "/one/two/three");
    equals(paths.join("/", "one", "two/", "three"), "/one/two/three");
    equals(paths.join("", "one", "two/", "three"), "/one/two/three");
});

test("split1", function () {
    equals(paths.split("one/two/three"), "one,two,three");
    equals(paths.split("one/two/three").length, 3);
    equals(paths.split("/one/two/three"), ",one,two,three");
    equals(paths.split("/one/two/three").length, 4);
});

test("normalize", function () {
    equals(paths.normalize("/one/two/three"), "/one/two/three");
    equals(paths.normalize("/one//two/three"), "/one/two/three");
    equals(paths.normalize("//one//two//three"), "//one/two/three");
    equals(paths.normalize("one/two/three"), "one/two/three");
    equals(paths.normalize("one/two/three/"), "one/two/three");
});

test("parent", function () {
	equals(paths.parent("/one/two/three"), "/one/two");
	equals(paths.parent("/one/two"), "/one");
	equals(paths.parent("/one"), "/");
	
	equals(paths.parent("one/two/three"), "one/two");
	equals(paths.parent("one/two"), "one");
	equals(paths.parent("one"), "");
	
	try {
		paths.parent("/");
		ok(false, "no error caught 1");
	}
	catch (e) {
		equals(e.name, "InvalidPath");
	}
	
	try {
		paths.parent("");
		ok(false, "no error caught 2");
	}
	catch (e) {
		equals(e.name, "InvalidPath");
	}
});

})();
