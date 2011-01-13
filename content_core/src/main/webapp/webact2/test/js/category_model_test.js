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
  
module("categories_model_test.js");

eval(webact.imports("category_model"));

test("test load bookmarks", function () {
	expect(0);
	stop();
	
	loadCategoryModel("jsm712", "i1", function () {
		start();
	});
});

 
})();