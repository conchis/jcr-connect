eval(base2.tables.namespace);
eval(base2.list_models.namespace);
eval(base2.strings.namespace);
eval(base2.generator.namespace);

function attachTabs() { 
    var search_tab = jQuery("#search_tab")
    var category_tab = jQuery("#category_tab")
    var advanced_tab = jQuery("#advanced_tab")
    
    var search_panel = jQuery("#search");
    var category_panel = jQuery("#categories");
    var advanced_panel = jQuery("#advanced");    
    
    category_panel.hide();
    advanced_panel.hide();

    search_tab.click(function () {
        search_tab.addClass("tab_button_selected");
        category_tab.removeClass("tab_button_selected");
        advanced_tab.removeClass("tab_button_selected");
        
        category_panel.hide();
        advanced_panel.hide();
        search_panel.show();      
    });
    category_tab.click(function () {
        category_tab.addClass("tab_button_selected");
        search_tab.removeClass("tab_button_selected");
        advanced_tab.removeClass("tab_button_selected");
        
        search_panel.hide();
        advanced_panel.hide();
        category_panel.show(); 
    }); 
    advanced_tab.click(function () {
        advanced_tab.addClass("tab_button_selected");
        search_tab.removeClass("tab_button_selected");
        category_tab.removeClass("tab_button_selected");
        
        search_panel.hide();
        category_panel.hide();
        advanced_panel.show(); 
    });
}

var ThumbColumn = Column.extend({
    render: function (item, index) {
        var attributes = {style:'width:' + this.width + 'px'};
        if (this.css_class != null)
            attributes['class'] = this.css_class;
        //var value = this.getValue(item);
        var thumb = item.thumb;
        var html =
            item.title +
            tag("img", {src: thumb.path,
                width: thumb.width, height: thumb.height});

        return td(attributes, html);
    }
});

function makeIndexArray(data) {
    var index_data = [];
	var items = data.children;
    for (var index = 0; index < items.length; index += 1) {
        var item = items[index];
		var path = REPOSITORY_URL + item.name;
        var title = item.title;
        if (title == null) title = "";
        var thumbnail = item.thumbnail;
        var thumbnail_path =  REPOSITORY_URL + thumbnail.path;
        var index_item = {
            title: title, path: path, 
            thumb: {path: thumbnail_path, width: thumbnail.width, height: thumbnail.height},
            item: item};
        index_data.push(index_item);
    }
    return index_data;
}

function makeIndex(data) {
    var element = jQuery("#thumbs");
    element.empty();
    var table = new Table({
        model: new ListModel(data),
        height: 405,
        columns: [
            new ThumbColumn("thumbnail", {width: 160,  sortable: false, label: ""})
        ]
    });
    table.makeControl(element);
    table.addListener("select", function (index, item) {
       showImage(index, item);
    });
}

function showImage(index, item) {
    var path = item.path + "/contents.json";
    jQuery.getJSON(path, {}, function (data) {
        var metadata = data.metadata;
        
        var title_element = jQuery("#view-title");
        title_element.html(metadata.title);

        var by_element = jQuery("#view-by");
        by_element.html(metadata.creators.join(", "));

        var viewer_element = jQuery("#view-image"); 
        writeObjectTag(viewer_element, data);

        //element.append(tag("img", {src: "content/" + item.name + "/small.jpg"}));

    })
}

function writeObjectTag(element, data) {
    var url = data.sources.tiled.href;
    var html = (
          "<object classid=\"clsid:d27cdb6e-ae6d-11cf-96b8-444553540000\" codebase=\"http://fpdownload.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=7,0,0,0\" width=\"550\" height=\"340\" id=\"tree-view\" align=\"middle\">" +
          "	<param name=\"allowScriptAccess\" value=\"sameDomain\" />" +
          "	<param name=\"movie\" value=\"flash/viewer.swf?image=" + url + "&bgcolor=#FFFFFF\" />" +
          "	<param name=\"quality\" value=\"high\" />" +
          "	<param name=\"scale\" value=\"noscale\" />" +
          "	<param name=\"salign\" value=\"lt\" />" +
          "	<param name=\"bgcolor\" value=\"#ffffff\" />" +
          "	<embed src=\"flash/viewer.swf?image=" + url + "&bgcolor=#FFFFFF\" quality=\"high\" scale=\"noscale\" salign=\"lt\" bgcolor=\"#ffffff\" width=\"550\" height=\"340\" name=\"tree-view\" align=\"middle\" allowscriptaccess=\"sameDomain\" type=\"application/x-shockwave-flash\" pluginspage=\"http://www.macromedia.com/go/getflashplayer\" />" +
          "</object>");
    element.html(html);
}
