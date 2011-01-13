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
 * @version 12 October 2010
 */
 
 webact.in_package("category_model", function (category_model) {
 
 	eval(webact.imports("observers"));
 		
 	category_model.makeCategoryModel = function (options) {
 		var model = {};
 		
 		model.getAllCategories = function () {
 			return [];
 		}
 		
 		model.getCategories = function () {
 			return [];
 		}
 		
		model.hasCategory = function (category) {
			return false;
		}
 		
 		model.addCategory = function (category, callback) {
 		}
 		
 		model.removeCategory = function (category, callback) {
 		}
 		
 		return model;
 	}
 	
 	category_model.loadCategoryModel = function (user, item, callback) {
 		callback();
 	}
 
 });