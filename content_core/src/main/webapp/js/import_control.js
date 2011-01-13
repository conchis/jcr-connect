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

 webact.in_package("import_control", function (import_control) {

    eval(webact.imports("controls"));
    eval(webact.imports("buttons"));
    eval(webact.imports("bookmarks"));

    var category_map = {};

    var setImport = function (id, folder) {
        if (typeof(folder) == "undefined")
            folder = "Bookmarks";

    }

    import_control.makeImportControl = function (options) {

        var control = makeControl(options);
        control.class_name = "ImportControl";

        var user_id = options.user;
        var item = options.item;

        var item_path = item.item_path;
        var workspace = item.source;
        if (workspace == "default") workspace = "local";

        item = {path: item_path, source: workspace};

        var import_checkbox = null;
        var folder_span = null;
        var folder_field = null;
        var select_list = null;

        var is_imported = false;
        var is_focused = false;

        var folders = [];
        var mark = null;
        var bookmarks = makeBookmarkModel({});
        bookmarks.loadUser(user_id, function () {
            folders = bookmarks.categories;
            updateSelectList();

            bookmarks.loadBookmark(item, function (bookmark) {
                mark = bookmark;
                refresh();
            });

        });

        var refresh = function () {
            if (mark == null) {
                folder_span.hide();
                select_list.hide();
                import_checkbox.attr("checked", false);
            }
            else {
                folder_span.show();
                folder_field.val(mark.categories[0]);
                import_checkbox.attr("checked", true);
            }
        }

        var bookmark = function () {
            var folder = folder_field.val();
            bookmarks.bookmark(item, folder, function (new_mark) {
                mark = new_mark;
                refresh();
            });
        }

        var update = function () {
            is_imported = import_checkbox.attr("checked");
            if (is_imported)
                folder_span.show();
            else {
                folder_span.hide();
                select_list.hide();
            }
        }

        var generateSelectList = function (dom_element) {
            select_list = jQuery("<ol/>", {
                "class": "wa_import_list"
            });
            dom_element.append(select_list);
            select_list.hide();
            updateSelectList();

            select_list.scroll(function () {
                console.log("scroll!");
            });
        }

        var updateSelectList = function () {
            select_list.empty();
            for (var index = 0; index < folders.length; index += 1) {
                var item = jQuery("<li/>");
                select_list.append(item);
                var link = jQuery("<a/>", {
                    href: "#",
                    text: folders[index]
                });
                item.append(link);
                link.bind("click", {folder: folders[index]}, function (event) {
                    var selected = event.data.folder;
                    folder_field.val(selected);
                    bookmark();
                    select_list.hide();
                });
            }
        }

        var generateFolderField = function (dom_element) {
            folder_span = jQuery("<span/>", {"class": "wa_folder_span"});
            dom_element.append(folder_span);

            folder_span.append("into folder:");

            folder_field = jQuery("<input/>", {
                type: "text",
                "class": "wa_import_field"
            });
            folder_span.append(folder_field);

            folder_field.focus(function () {
                select_list.show();
                is_focused = true;
            });
            folder_field.blur(function () {
                bookmark();
                is_focused = false;
                //select_list.hide();
            });
        }

        control.generate = function (container) {
            var dom_element = jQuery("<div/>", {"class": "wa_import"});
            container.append(dom_element);
            dom_element.mouseleave(function () {
                if (!is_focused)
                    select_list.hide();
            });

            dom_element.append("Import");

            import_checkbox = jQuery("<input/>", {
                type: "checkbox",
                name: "import"
            });
            import_checkbox.change(update);
            dom_element.append(import_checkbox);

            generateFolderField(dom_element);
            generateSelectList(dom_element);
            update();

            return dom_element;
        }

        return control;
    }

});