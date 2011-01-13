/**
 * Copyright 2010 Jonathan A. Smith.
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
 * @version 3 November 2010
 */

 webact.in_package("bookmarks", function (bookmarks) {

    eval(webact.imports("observers"));

    bookmarks.makeBookmarkModel = function (options) {

        var model = makeBroadcaster();
        model.info = null;

        var reportError = function (request, status) {
			alert("Server communications failed: " + request.statusText);
		}

        model.loadUser = function (user_id, callback) {
            if (user_id == model.user_id && model.info != null)
                callback(model.info);
            else {
                model.user_id = user_id;
                model.info = null;
    			jQuery.ajax({
    				type: "GET",
    				url: "/category_marker/service/bookmark/" + user_id,
    				dataType: 'json',
    				success: function (info) {
    				    model.info = info;
    				    var categories = [];
    				    for (var index = 0; index < info.categories.length; index += 1)
    				        categories.push(info.categories[index].name);
    				    model.categories = categories;
    					callback(model);
    				},
    				error: reportError
    			});
            }
        }

        model.bookmark = function (item, category, callback) {
            if (model.info == null) {
                alert("User information not yet loaded.");
                throw new Error("User information not yet loaded.");
            }
    console.log("bookmark:", item, "cat:", category);
            var request = {path: item.path, ws: item.source,  cat: category};
			jQuery.ajax({
				type: "GET",
				url: "/category_marker/service/bookmark/" + model.user_id,
				data: request,
				dataType: 'json',
				success: function (bookmark) {
					callback(bookmark);
				},
				error: reportError
			});
        }

        model.loadBookmark = function (item, callback) {
            if (model.info == null) {
                alert("User information not yet loaded.");
                throw new Error("User information not yet loaded.");
            }
            var request = {path: item.path, ws: item.source};
			jQuery.ajax({
				type: "GET",
				url: "/category_marker/service/bookmark/" + model.user_id,
				data: request,
				dataType: 'json',
				success: function (bookmark) {
					callback(bookmark);
				},
				error: reportError
			});
        }

        return model;
    }

 });