/**
 * Copyright 2009, 2010 Northwestern University, Jonathan A. Smith
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

// core/options.js
new function(_){var options=new base2.Package(this,{name:"options",version:"0.1",imports:"",exports:"OptionsMixin,Options"});eval(this.imports);var OptionsMixin=base2.Module.extend({setOptions:function(self,new_options){var options=self.options;if(typeof(options)=="undefined"){options={};self.options=options;}
for(var name in new_options)
options[name]=new_options[name];},option:function(self,name,default_value){if(typeof self.options=="undefined"){if(typeof default_value!="undefined")
return default_value;return null;}
var value=self.options[name];if(typeof value!="undefined")
return value;if(typeof default_value!="undefined")
return default_value;return null;}});var Options=base2.Base.extend({constructor:function(options,defaults){this.setOptions(options,defaults);}});Options.implement(OptionsMixin);eval(this.exports);};

// core/observers.js
new function(_){var observers=new base2.Package(this,{name:"observers",version:"0.1",imports:"",exports:"Broadcaster, BroadcasterMixin"});eval(this.imports);var BroadcasterMixin=base2.Module.extend({addListener:function(broadcaster,selector,listener,method_name){var listener_map=broadcaster.listener_map;if(typeof(listener_map)=="undefined"){listener_map=new Object();broadcaster.listener_map=listener_map;}
var listeners=listener_map[selector];if(typeof(listeners)=="undefined"){listeners=new Array();listener_map[selector]=listeners;}
if(typeof(listener)=="function")
listeners.push(listener);else{if(typeof(method_name)=="undefined")
method_name=selector;listeners.push(function(){listener[method_name].apply(listener,arguments);});}},removeListener:function(broadcaster,selector,listener){var listener_map=broadcaster.listener_map;if(typeof(listener_map)=="undefined")
return false;var listeners=listener_map[selector];if(listeners==null)
return false;var index=listeners.indexOf(listener);if(index<0)return false;listeners.splice(index,1);return true;},hasListener:function(broadcaster,selector,listener){var listener_map=broadcaster.listener_map;if(typeof(listener_map)=="undefined")
return false;var listeners=listener_map[selector];if(listeners==null)
return false;return listeners.indexOf(listener)>=0;},broadcast:function(broadcaster,selector){var argument_list=new Array();for(var index=2;index<arguments.length;index+=1)
argument_list.push(arguments[index]);argument_list.push(broadcaster);broadcaster.sendBroadcast(selector,argument_list);},sendBroadcast:function(broadcaster,selector,argument_list,source){var listener_map=broadcaster.listener_map;if(typeof(listener_map)=="undefined")return;if(typeof(source)=="undefined")
source=broadcaster;var listeners=listener_map[selector];if(listeners==null)return;for(var index in listeners)
listeners[index].apply(null,argument_list);}});var Broadcaster=base2.Base.extend({});Broadcaster.implement(BroadcasterMixin);eval(this.exports);};

// core/collections.js
new function(_){var collections=new base2.Package(this,{name:"collections",version:"0.1",imports:"",exports:"List,Set,Map,IndexError"});eval(this.imports);var IndexError=new Error("Index out of bounds");var List=base2.Base.extend({constructor:function(new_items){new_items=asArray(new_items);this.items=new_items.slice(0);},get:function(index){var items=this.items;if(index<0||index>=items.length)
throw IndexError;return items[index];},set:function(index,value){var items=this.items;if(index<0||index>items.length)
throw IndexError;items[index]=value;return this;},add:function(value,index){var items=this.items;if(typeof index=="undefined")
index=items.length;else if(index<0||index>items.length)
throw IndexError;items.splice(index,0,value);return this;},remove:function(index){var items=this.items;if(index<0||index>=items.length)
throw IndexError;items.splice(index,1);},size:function(){return this.items.length;},indexOf:function(value,start){var items=this.items;start=start||0;for(var index=start;index<items.length;index+=1){if(items[index]==value)return index;}
return-1;},contains:function(value){var items=this.items;for(var index=0;index<items.length;index+=1){if(items[index]==value)return true;}
return false;},each:function(closure){var items=this.items;for(var index=0;index<items.length;index+=1){closure(items[index],index);}},collect:function(closure){var result=new Array();var items=this.items;for(var index=0;index<items.length;index+=1){var value=closure(items[index],index);if(typeof value!="undefined")
result.push(value);}
return new List(result);},select:function(test_function){var items=this.items;var result=[];for(var index=0;index<items.length;index+=1){var item=items[index];if(test_function(item,index))
result.push(item);}
return new List(result);},find:function(test_function){var items=this.items;for(var index=0;index<items.length;index+=1){var item=items[index];if(test_function(item,index))
return item;}
return null;},slice:function(start,end){if(typeof start=="undefined")
start=0;if(typeof end=="undefined")
end=this.items.length;var new_items=this.items.slice(start,end);return new List(new_items);},append:function(new_items){new_items=asArray(new_items);var new_list=this.slice();for(var index in new_items)
new_list.add(new_items[index]);return new_list;},sort:function(compare){this.items=this.items.sort(compare);return this;},toString:function(){var result=[];this.each(function(value,index){if(typeof(value)!="string")
result.push(value);else
result.push('"'+value+'"');})
return"new List(["+result.join(", ")+"])";}});var Set=base2.Base.extend({constructor:function(new_items){var map=new Object();this.map=map;new_items=asArray(new_items);for(var index in new_items)
this.add(new_items[index]);},add:function(value){this.map[value]=value;},remove:function(value){delete this.map[value];},contains:function(value){return(typeof(this.map[value])!="undefined");},each:function(closure){var map=this.map;for(var key in map)
closure(map[key]);},collect:function(closure){var map=this.map;var result=new Set();for(var key in map)
result.add(closure(map[key]));return result;},select:function(test_function){var map=this.map;var result=new Set();for(var key in map){var value=map[key];if(test_function(value))
result.add(value);}
return result;},find:function(test_function){var map=this.map;for(var key in map){var value=map[key];if(test_function(value))
return value;}},union:function(new_items){new_items=asArray(new_items);var result=new Set(this);for(var index in new_items)
result.add(new_items[index]);return result;},intersection:function(other_items){if(!(other_items instanceof Set))
other_items=new Set(asArray(other_items));var result=new Set();this.each(function(item){if(other_items.contains(item))
result.add(item);});return result;},difference:function(other_items){if(!(other_items instanceof Set))
other_items=new Set(asArray(other_items));var result=new Set();this.each(function(item){if(!other_items.contains(item))
result.add(item);});return result;},isEmpty:function(){var count=0;for(var value in this.map)
count+=1;return count==0;},size:function(){var count=0;for(var value in this.map)
count+=1;return count;},values:function(){var map=this.map;var result=new Array();for(var key in map)
result.push(map[key]);return new List(result);},toString:function(){var result=[];this.each(function(item){if(typeof item!="string")
result.push(item.toString());else
result.push('"'+item+'"');});return"new Set(["+result.join(", ")+"])";}});function asArray(things){if(things instanceof List)
return things.items;if(things instanceof Set)
return things.values().items;var things_type=typeof(things);if(things_type=="object"&&typeof things.length=="number")
return things;if(things_type=="undefined")
return new Array();throw new Error("Must be array");}
var Map=base2.Base.extend({constructor:function(new_items){this.map=new Object();;if(new_items instanceof Map){var me=this;new_items.each(function(value,key){me.set(key,value);})}
else if(typeof new_items=="object"){for(var key in new_items)
this.set(key,new_items[key]);}
else if(!(typeof new_items=="undefined"))
throw new Error("Argument must be Map or object");},get:function(key){var pair=this.map[key]||null;if(pair==null)return null;return pair.value;},set:function(key,value){this.map[key]={key:key,value:value};return this;},remove:function(key){delete this.map[key];return this;},contains:function(key){return(typeof(this.map[key])!="undefined");},each:function(closure){var map=this.map;for(var key in map){var pair=map[key];closure(pair.value,pair.key);}},collect:function(closure){var result=new Map();var map=this.map;for(var key in map){var pair=map[key];result.set(pair.key,closure(pair.value,pair.key));}
return result;},select:function(test_function){var map=this.map;var result=new Map();for(var key in map){var pair=map[key];if(test_function(pair.value,pair.key))
result.set(pair.key,pair.value);}
return result;},find:function(test_function){var map=this.map;for(var key in map){var pair=map[key];if(test_function(pair.value,pair.key))
return pair.value;}},isEmpty:function(){var count=0;for(var key in this.map)
count+=1;return count==0;},size:function(){var count=0;for(var key in this.map)
count+=1;return count;},keys:function(){var map=this.map;var result=[];for(var key in map)
result.push(map[key].key);return new Set(result);},values:function(){var map=this.map;var result=[];for(var key in map)
result.push(map[key].value);return new Set(result);},associations:function(){var map=this.map;var result=[];for(var key in map)
result.push(map[key]);return new List(result);},toString:function(){var result=[];this.each(function(value,key){if(typeof value!="string")
result.push(key+": "+value.toString());else
result.push(key+': "'+value+'"');});return"new Map({"+result.join(", ")+"})";}});eval(this.exports);};

// core/strings.js
new function(_){var strings=new base2.Package(this,{name:"strings",version:"0.1",imports:"",exports:"trim,pad,padLeft,repeated"});eval(this.imports);function trim(text){matches=text.match(/\W*[^\W]+/g)
if(matches==null)
return"";var left_match=matches[0].match(/\W*(.*)/);if(left_match!=null)
matches[1]=left_match[1];return matches.join("");}
function pad(text,width,pad_char){if(typeof pad_char=="undefined")
pad_char=' ';var parts=[text];for(var count=text.length;count<width;count+=1)
parts.push(pad_char);return parts.join("");}
function padLeft(text,width,pad_char){if(typeof pad_char=="undefined")
pad_char=' ';var parts=new Array();for(var count=text.length;count<width;count+=1)
parts.push(pad_char);parts.push(text);return parts.join("");}
function repeated(text,count){var parts=new Array();for(var index=0;index<count;index+=1)
parts.push(text);return parts.join("");}
eval(this.exports);};

// core/cookies.js
new function(_){var cookies=new base2.Package(this,{name:"cookies",version:"0.1",imports:"",exports:"putCookie,getCookie,deleteCookie"});eval(this.imports);function putCookie(key,value){var json_string=encodeURIComponent(JSONstring.make(value));document.cookie=key+"="+json_string+";path=/";}
function getCookie(key){var cookies=document.cookie.split(';');for(var i=0;i<cookies.length;i++){var cookie=jQuery.trim(cookies[i]);if(cookie.substring(0,key.length+1)==(key+'=')){var json_string=decodeURIComponent(cookie.substring(key.length+1));return eval("("+json_string+")");}}
return null;}
function deleteCookie(key){var cookies=document.cookie.split(';');var now=(new Date()).toGMTString();for(var i=0;i<cookies.length;i++){var cookie=jQuery.trim(cookies[i]);if(cookie.substring(0,key.length+1)==(key+'='))
document.cookie=cookie+";expires="+now+";path=/";}}
eval(this.exports);};

// core/generator.js
new function(_){var generator=new base2.Package(this,{name:"generator",version:"0.1",imports:"",exports:"element,tag,div,span,ul,ol,li,table,col,tr,th,td,p,a,br,form,"
+"input,textarea,select,option,img,style"});eval(this.imports);function attributes(attribute_map){if(typeof attribute_map=="undefined"||attribute_map==null)
return"";var results=new Array();for(var name in attribute_map){if(name=='style')
results.push("style=\""+style(attribute_map[name])+"\"");else
results.push(name+"=\""+attribute_map[name].toString()+"\"");}
if(results.length==0)
return"";return" "+results.join(" ");}
function styleValue(value){if(typeof value=='number')
return value+'px';else if(typeof(value)=='object'&&typeof(value.length)=='number'){var collect=[];for(var index=0;index<value.length;index+=1)
collect.push(styleValue(value[index]));return collect.join(' ');}
else
return value.toString();}
function styleName(name){return name.replace(/_/g,'-');}
function style(style_object){if(typeof(style_object)=='string')
return style_object;var out=[];for(var field in style_object){var value=styleValue(style_object[field]);out.push(styleName(field)+':'+value);}
return out.join(';');}
function flatten(items){function flattenInto(items,results){for(var index=0;index<items.length;index+=1){var an_item=items[index];if(an_item instanceof Array)
flattenInto(an_item,results);else if(an_item!=null)
results.push(an_item);}}
if(typeof items=="undefined"||items==null)
return;var results=new Array();flattenInto(items,results);return results;}
function element(name,attribute_map,contents){if(typeof contents=="undefined"||contents.length==0)
return"<"+name+attributes(attribute_map)+"/>";var results=new Array();results.push("<"+name+attributes(attribute_map)+">");contents=flatten(contents);for(var index=0;index<=contents.length;index+=1)
results.push(contents[index]);results.push("</"+name+">");return results.join("");}
function tag(name,attribute_map){var contents=new Array();for(var index=2;index<arguments.length;index+=1)
contents.push(arguments[index]);return element(name,attribute_map,contents);}
function div(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("div",attribute_map,contents);}
function span(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("span",attribute_map,contents);}
function p(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("p",attribute_map,contents);}
function a(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("a",attribute_map,contents);}
function br(){if(arguments.length>0)
throw new Error('Invalid <br/>');return element("br");}
function img(attribute_map){var contents=new Array();return element("img",attribute_map);}
function ul(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("ul",attribute_map,contents);}
function ol(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("ol",attribute_map,contents);}
function li(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("li",attribute_map,contents);}
function table(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("table",attribute_map,contents);}
function tr(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("tr",attribute_map,contents);}
function th(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("th",attribute_map,contents);}
function td(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("td",attribute_map,contents);}
function col(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("col",attribute_map,contents);}
function form(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("form",attribute_map,contents);}
function input(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("input",attribute_map,contents);}
function textarea(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("textarea",attribute_map,contents);}
function select(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("select",attribute_map,contents);}
function option(attribute_map){var contents=new Array();for(var index=1;index<arguments.length;index+=1)
contents.push(arguments[index]);return element("option",attribute_map,contents);}
eval(this.exports);};

// core/controls.js
new function(_){var controls=new base2.Package(this,{name:"controls",version:"0.1",imports:"options,observers,generator",exports:"Control"});eval(this.imports);var id_counter=0;var Control=Broadcaster.extend({class_name:"Control",constructor:function(options,defaults){this.base();if(typeof defaults!="undefined")
this.setOptions(defaults);this.setOptions(options);this.initializeParameters();this.initializeContents();this.dom_element=null;this.contents_element=null;},initializeParameters:function(){this.id=this.option("id");if(this.id==null)this.getId();this.width=this.option('width');this.height=this.option('height');this.position=this.option('position');this.left=this.option('left');this.top=this.option('top');this.style=this.option('style');this.css_class=this.option('css_class');this.title=this.option("title");},initializeContents:function(){this.parent=null;this.contents=null;var contents=this.option("contents",[]);for(var index=0;index<contents.length;index+=1)
this.add(contents[index]);},getId:function(){var id=this.id;if(typeof id!="undefined"&&id!=null)
return id;id_counter+=1;this.id=this.class_name+"_"+id_counter;return this.id;},makeId:function(postfix){if(typeof postfix=="undefined")
postfix="";return this.getId()+"_"+postfix;},add:function(control){var contents=this.contents;if(contents==null){contents=[];this.contents=contents;}
contents.push(control);control.addedTo(this);var contents_element=this.contents_element;if(contents_element!=null)
control.makeControl(contents_element);},addedTo:function(parent){this.parent=parent;},remove:function(control){if(typeof(control)!="undefined"){var contents=this.contents;var index=0;while(index<contents.length){if(contents[index]===control)
break;index+=1;}
if(index==contents.length)
index=-1;if(index>=0)
contents.splice(index,1);control.removedFrom(this);}
else if(this.parent!=null)
this.parent.remove(this);},removedFrom:function(parent){if(this.dom_element!=null){this.dom_element.remove();}
this.parent=null;},size:function(){var contents=this.contents;if(contents==null)
return 0;else
return contents.length;},eachChild:function(callback){var contents=this.contents;if(contents!=null){for(var index=0;index<contents.length;index+=1)
callback(contents[index],index);}},findChild:function(predicate){var contents=this.contents;if(contents==null)return null;for(var index=0;index<contents.length;index+=1){var child=contents[index];if(predicate(child,index))
return child;}
return null;},each:function(callback){var contents=this.contents;if(contents==null)return;for(var index=0;index<contents.length;index+=1){var child=contents[index];callback(child);child.each(callback);}},find:function(predicate){var contents=this.contents;if(contents==null)return null;for(var index=0;index<contents.length;index+=1){var child=contents[index];if(predicate(child))return child;var found=child.find(predicate);if(found!=null)return found;}
return null;},element_tag:'div',createDomElement:function(parent_element){var id=this.getId();parent_element.append(tag(this.element_tag,{id:id}));var dom_element=jQuery("#"+id);var css_class=this.css_class;if(css_class!=null)
dom_element.addClass(css_class);var style=this.makeStyle();if(style!={})
dom_element.css(style);var title=this.title;if(title!=null)
dom_element.attr('title',title);return dom_element;},makeStyle:function(){var style=this.style||{};if(this.width!=null)style.width=this.width;if(this.height!=null)style.height=this.height;if(this.position!=null)style.position=this.position;if(this.top!=null)style.top=this.top;if(this.left!=null)style.left=this.left;return style;},generateContents:function(){var contents=this.contents;var contents_element=this.contents_element;if(contents==null)return;for(var index=0;index<contents.length;index+=1){var control=contents[index];if(control.dom_element==null)
control.makeControl(contents_element);}},generate:function(){throw new Error("Implement generate in subclasses");},setContentsElement:function(contents_element){this.contents_element=contents_element;},makeControl:function(container){container=this.makeQuery(container);this.dom_element=this.createDomElement(container);this.contents_element=this.dom_element;this.generate(this.dom_element);this.generateContents();this.update();},makeQuery:function(selector){if(typeof(selector)!="string")
return selector;var first_char=selector.charAt(0);if(first_char!="#"&&first_char!=".")
selector="#"+selector;return jQuery(selector);},initializeFrom:function(query){throw new Error("Implement initializeFrom in subclasses");},update:function(){},hide:function(option){this.dom_element.hide(option);},show:function(option){this.dom_element.show(option);}});Control.implement(OptionsMixin);jQuery.fn.makeControl=function(control){control.makeControl(this);}
eval(this.exports);};

// core/list_models.js
new function(_){var list_models=new base2.Package(this,{name:"list_models",version:"0.1",imports:"observers,collections",exports:"ListModel,ListFilter"});eval(this.imports);var ListModel=List.extend({set:function(index,value){var prior_value=this.get(index);if(value==prior_value)return;this.base(index,value);this.broadcast("changed",index,value,prior_value);},add:function(value,index){index=index||this.size();this.base(value,index);this.broadcast("added",index,value);},remove:function(index){var prior_value=this.get(index);this.broadcast("will_remove",index,prior_value);this.base(index);this.broadcast("removed",index,prior_value);},changed:function(index,prior_value){if(typeof prior_value=="undefined")
prior_value=null;this.broadcast("changed",index,this.get(index),prior_value);},getItemIndex:function(index){return index;},getBackIndex:function(original_index){return original_index;}});ListModel.implement(BroadcasterMixin);var ListFilter=Broadcaster.extend({constructor:function(list){this.index_map=new Array();this.back_index=new Array();this.changed=false;this.compare=function(item_a,item_b){return 0;}
this.filter=function(item){return true};if(typeof list!="undefinded")
this.setList(list);else
this.setList(new ListModel());},setList:function(list){this.list=list;list.addListener("changed",this,'onChanged');list.addListener("added",this,'onAdded');list.addListener("removed",this,'onRemoved');list.addListener("filtered",this,'onFiltered');var self=this;list.addListener("will_remove",function(index,prior_value){self.update();self.removed_index=self.back_index[index];});this.changed=true;},filterBy:function(filter){this.filter=filter;this.changed=true;this.broadcast("filtered");},orderBy:function(compare_function){this.compare=compare_function;this.changed=true;this.broadcast("filtered");},update:function(changed){if(typeof changed!="undefined")
this.changed=changed;if(!this.changed)
return;this.applyFilter();this.applySort();this.backIndex();this.changed=false;},applyFilter:function(){var index_map=new Array();var filter=this.filter;this.list.each(function(item,index){if(filter(item,index))
index_map.push(index);});this.index_map=index_map;},applySort:function(){var compare=this.compare;var list=this.list;var index_map=this.index_map;index_map.sort(function(index_a,index_b){return compare(list.get(index_a),list.get(index_b));});},backIndex:function(){var index_map=this.index_map
var back_index=new Array();for(var index=0;index<index_map.length;index+=1)
back_index[index_map[index]]=index;this.back_index=back_index;},get:function(index){if(this.changed)this.update();return this.list.get(this.index_map[index]);},size:function(){if(this.changed)this.update();return this.index_map.length;},indexOf:function(value,start){if(typeof start=="undefined")start=0;var size=this.size();for(var index=start;index<size;index+=1){if(this.get(index)==value)
return index;}
return-1;},contains:function(value){var size=this.size();for(var index=0;index<size;index+=1){if(this.get(index)==value)return true;}
return false;},getItemIndex:function(index){if(this.changed)this.update();return this.list.getItemIndex(this.index_map[index]);},getBackIndex:function(original_index){if(this.changed)this.update();return this.back_index[this.list.getBackIndex(original_index)];},each:function(closure){var size=this.size();for(var index=0;index<size;index+=1)
closure(this.get(index),index);},collect:function(closure){var items=new Array();var size=this.size();for(var index=0;index<size;index+=1)
items.push(closure(this.get(index),index));return new List(items);},select:function(test_function){var items=new Array();var size=this.size();for(var index=0;index<size;index+=1){var value=this.get(index);if(test_function(value,index))
items.push(value);}
return new List(items);},find:function(test_function){var size=this.size();for(var index=0;index<size;index+=1){var value=this.get(index);if(test_function(value,index))
return value;}
return null;},onChanged:function(index,value,prior_value){this.update(true);var mapped_index=this.back_index[index];if(typeof mapped_index!="undefined")
this.broadcast("changed",mapped_index,value,prior_value);},onAdded:function(index,value){this.update(true);var mapped_index=this.back_index[index];if(typeof mapped_index!="undefined")
this.broadcast("added",mapped_index,value);},onRemoved:function(index,prior_value){this.update(true);var removed_index=this.removed_index;if(typeof removed_index!="undefined")
this.broadcast("removed",removed_index,prior_value);},onFiltered:function(){this.update(true);this.broadcast("filtered");},toString:function(){var size=this.size();var out=new Array();for(var index=0;index<size;index+=1)
out.push(this.get(index).toString());return"["+out.join(", ")+"]";}});eval(this.exports);};

// core/tag_index.js
new function(_){var tag_index=new base2.Package(this,{name:"tag_index",version:"0.1",imports:"observers,options",exports:"TagIndex"});eval(this.imports);var TagIndex=Broadcaster.extend({constructor:function(options){this.base();this.setOptions(options);this.list=null;this.tag_table={};this.item_tags={};var tags=this.option("tags");if(typeof(tags)=="function")
this.getItemTags=tags;else if(typeof(tags)=="string")
this.getItemTags=function(item){return item[tags];};else
this.getItemTags=function(item){return item.tags;};var list=this.option("list");if(list!=null)
this.setList(list);},setList:function(list){this.list=list;list.addListener("changed",this,"indexItem");list.addListener("added",this,"indexAll");list.addListener("removed",this,"indexAll");this.indexAll();},indexAll:function(){this.tag_table={};this.item_tags={};var size=this.list.size();for(var item_index=0;item_index<size;item_index+=1)
this.indexItem(item_index);},indexItem:function(item_index){this.removeItem(item_index);var item=this.list.get(item_index);var tags=this.getItemTags(item);this.item_tags[item_index]=tags.slice(0);for(var index=0;index<tags.length;index+=1){var tag=tags[index];var tag_record=this.makeTagRecord(tag);tag_record.locations.push(item_index);}},removeItem:function(item_index){var tags=this.item_tags[item_index];if(typeof(tags)=="undefined")return;var tag_table=this.tag_table;for(var index=0;index<tags.length;index+=1){var tag=tags[index];var tag_record=tag_table[tag];var locations=tag_record.locations;var position=locations.indexOf(item_index);if(position>=0)
locations.splice(position,1);}
delete this.item_tags[item_index];},makeTagRecord:function(tag){var tag_table=this.tag_table;var tag_record=tag_table[tag];if(typeof(tag_record)!="undefined")
return tag_record;tag_record={tag:tag,count:0,locations:[]}
tag_table[tag]=tag_record;return tag_record;},getTags:function(){var tags=[];for(var tag in this.tag_table)
tags.push(tag);tags.sort();return tags;},getLocations:function(tag){var tag_record=this.tag_table[tag];if(tag_record==null)
return[];return tag_record.locations;},getItems:function(tag){var locations=this.getLocations(tag);var list=this.list;var items=[];for(var index=0;index<locations.length;index+=1)
items.push(list.get(locations[index]));return items;},frequency:function(tag){var tag_record=this.tag_table[tag];if(typeof(tag_record)=="undefined")
return 0;return tag_record.locations.length/this.list.size();},collectItems:function(tags,intersection){var locations=this.collectLocations(tags,intersection);var list=this.list;var items=[];for(var index=0;index<locations.length;index+=1)
items.push(list.get(locations[index]));return items;},collectLocations:function(tags,intersection){if(typeof(tags)=="undefined"||tags.length==0)
return this.collectAllLocations();if(typeof(intersection)=="undefined"||!intersection)
return this.collectTagLocations(tags,{});else
return this.collectIntersect(tags);},collectIntersect:function(tags)
{var markers={};var locations=this.collectTagLocations(tags,markers);var result=[];for(var index=0;index<locations.length;index+=1)
{var location=locations[index];if(markers[location]>=tags.length)
result.push(location);}
return result;},collectTagLocations:function(tags,markers){var result=[];for(var tag_index=0;tag_index<tags.length;tag_index+=1){var tag=tags[tag_index];var locations=this.tag_table[tag].locations;for(var index=0;index<locations.length;index+=1){var location=locations[index];var count=markers[location];if(typeof(count)=="undefined")count=0;if(count==0)
result.push(locations[index]);markers[location]=count+1;}}
result.sort();return result;},collectAllLocations:function(){var result=[];var size=this.list.size();for(var index=0;index<size;index+=1)
result.push(index);return result;},findBestSelectors:function(count){var tag_table=this.tag_table;var tags=[];var half=this.list.size()/2;for(var tag in tag_table){var tag_record=tag_table[tag];tags.push({tag:tag_record.tag,weight:Math.abs(tag_record.locations.length-half)});}
tags.sort(function(first,second){return first.weight-second.weight;});tags=tags.slice(0,count);var result=[];for(var index=0;index<tags.length;index+=1)
result.push(tags[index].tag);result.sort();return result;},getNormalizedFrequencies:function(tags){result={};var maximum=0.0;var minimum=-1.0;for(var index=0;index<tags.length;index+=1){var tag=tags[index];var frequency=this.frequency(tag);result[tag]=frequency;if(frequency>maximum)
maximum=frequency;if(frequency<minimum||minimum==-1)
minimum=frequency;}
for(var tag in result){if(result[tag]>minimum)
result[tag]=(result[tag]-minimum)/(maximum-minimum);else
result[tag]=0;}
return result;},toString:function(){var tags=this.getTags();var parts=[];for(var index=0;index<tags.length;index+=1){var tag=tags[index];var locations=this.getLocations(tag);parts.push(tag+": "+locations.length);}
return parts.join("\n");}});TagIndex.implement(OptionsMixin);eval(this.exports);};

// core/paths.js
new function(_){var paths=new base2.Package(this,{name:"paths",version:"0.1",imports:"",exports:"split,append,join,normalize"});eval(this.imports);function split(path)
{var tokens=path.split("/");var result=[];for(var index=0;index<tokens.length&&tokens[index]=="";index+=1)
result.push(tokens[index]);for(;index<tokens.length;index+=1)
{var token=tokens[index];if(token!="")
result.push(token);}
return result;}
function append(tokens,others)
{return tokens;}
function join()
{var segments=[];for(var index=0;index<arguments.length;index+=1)
{var argument=arguments[index]
if(argument=='/')argument='';var tokens=split(argument);for(var token_index=0;token_index<tokens.length;token_index+=1)
segments.push(tokens[token_index]);}
return normalize(segments.join("/"));}
function normalize(path)
{return split(path).join("/");}
eval(this.exports);};

// core/trees.js
new function(_){var trees=new base2.Package(this,{name:"trees",version:"0.1",imports:"paths,observers",exports:"TreeNode"});eval(this.imports);var TreeNode=Broadcaster.extend({constructor:function(name,properties){this.base();this.name=name;this.child_map={};this.parent=null;this.children=[];this.index=-1;this.properties={};this.visible={};if(typeof(properties)=="object")
this.reset(properties);for(var index=2;index<arguments.length;index+=1)
this.addChild(arguments[index]);},addChild:function(child,index){if(typeof child.name!="string")
throw new Error("Category must be named");var children=this.children;if(typeof index=="undefined")
index=children.length;var child_map=this.child_map;var name=child.name;var prior_child=child_map[name];if(prior_child&&prior_child!==child)
prior_child.remove();if(child.parent===this&&child.index<index)
index-=1;if(child.parent!=null)
child.remove();children.splice(index,0,child);child_map[name]=child;for(var child_index=0;child_index<children.length;child_index+=1)
children[child_index].index=child_index;child.addedTo(this);this.broadcast("added",child.index,child);},addedTo:function(parent){this.parent=parent;},addNode:function(path){var tokens=paths.split(path);var examine=this;for(var index=0;index<tokens.length;index+=1){var name=tokens[index];if(name=="")
examine=examine.getRoot();else{var next=examine.getChildNamed(name);if(next==null){next=this.makeNode(name);examine.addChild(next);}
examine=next;}}
return examine;},makeNode:function(name){return new TreeNode(name);},removeChild:function(child){var children=this.children;var index=children.indexOf(child);if(index>=0){children.splice(index,1);delete this.child_map[child.name];for(var child_index=index;child_index<children.length;child_index+=1)
children[child_index].index=child_index;child.removedFrom(this);this.broadcast("removed",child.index,child,this);}},removedFrom:function(parent){if(this.parent==parent){this.parent=null;this.index=-1;}},remove:function(){if(this.parent!=null)
this.parent.removeChild(this);},getRoot:function()
{var examine=this;while(examine.parent)
examine=examine.parent;return examine;},getChildNamed:function(name){var node=this.child_map[name];if(typeof node=="undefined")
return null;return node;},getNode:function(path){var tokens=paths.split(path);var examine=this;for(var index=0;index<tokens.length;index+=1){var name=tokens[index];if(name=="")
examine=this.getRoot();else
examine=examine.getChildNamed(name);if(examine==null)
return null;}
return examine;},get:function(index){return this.children[index];},size:function(){return this.children.length;},getItemIndex:function(index){return index;},each:function(closure){var children=this.children;for(var index=0;index<children.length;index+=1){closure(children[index],index);}},eachNode:function(closure){var examine=this;while(examine!=null){closure(examine);examine=examine.next();}},getPath:function(){var examine=this;var names=[];for(;examine!=null;examine=examine.parent)
names.push(examine.name);return paths.join.apply(null,names.reverse());},setBasePath:function(base_path){this.base_path=base_path;},getAbsolutePath:function(){var base_path=this.base_path;if(typeof base_path=="string")
return base_path;var parent=this.parent;if(parent!=null)
return paths.join(parent.getAbsolutePath(),this.name);return this.name;},next:function(){var children=this.children;if(children.length>0)
return children[0];var node=this;var ancestor=this.parent;while(ancestor!=null){var after=ancestor.childAfter(node);if(after!=null)
return after;node=ancestor;ancestor=ancestor.parent;}
return null;},previous:function(){var parent=this.parent;if(parent!=null){var before=parent.childBefore(this);if(before!=null)
return before.lastDescendant();}
return this.parent;},childAfter:function(sibling){var children=this.children;var index=sibling.index;if(index==children.length-1)
return null;return children[index+1];},childBefore:function(sibling){var children=this.children;var index=sibling.index;if(index==0)
return null;return children[index-1];},lastDescendant:function(){var children=this.children;if(children.length==0)
return this;var last_child=children[children.length-1];var descendant=last_child.lastDescendant();if(descendant!=null)
return descendant;return last_child;},sendBroadcast:function(selector,argument_list,source)
{this.base(selector,argument_list,source);if(this.parent!=null)
this.parent.sendBroadcast(selector,argument_list,source);},setProperty:function(name,value){var prior_value=this.properties[name];if(typeof(prior_value)!="undefined"&&prior_value==value)
return;this.properties[name]=value;if(typeof(this[name])=="undefined"||this.visible[name]){this.visible[name]=true;this[name]=value;}
this.broadcast("changed",this.index,name,value,prior_value);},getProperty:function(name,default_value){var value=this.properties[name];if((typeof value=="undefined"||value==null)&&typeof default_value!="undefined")
value=default_value;return value;},revert:function(){var properties=this.properties;for(var name in properties){if(this.visible[name])
this[name]=properties[name];}},commit:function(names){var properties=this.properties;var visible=this.visible;if(typeof names=="undefined"){names=[];for(name in this.visible)
names.push(name);}
for(var index=0;index<names.length;index+=1){var name=names[index];visible[name]=true;this.setProperty(name,this[name]);}
this.revert();},reset:function(properties){var visible=this.visible;for(var name in visible)
delete this[name];this.properties={};this.visible={};for(var name in properties){var value=properties[name];this.properties[name]=value;if(typeof this[name]=="undefined"){this.visible[name]=true;this[name]=value;}}},isChanged:function(){var properties=this.properties;var visible=this.visible;for(name in visible){if(properties[name]!=this[name])
return true;}
return false;},getChanges:function(){var changes=new Object();var properties=this.properties;var visible=this.visible;for(name in visible){if(properties[name]!=this[name])
changes[name]=this[name];}
return changes;},class_name:'TreeNode',toString:function(){var parts=[this.name];var children=this.children;for(var index=0;index<children.length;index+=1)
parts.push(children[index].toString());return this.class_name+"("+parts.join(', ')+")";}});eval(this.exports);};

// core/connectors.js
new function(_){var connectors=new base2.Package(this,{name:"connectors",version:"0.1",imports:"paths,trees,options,observers",exports:"Connector,Category"});eval(this.imports);var Connector=base2.Base.extend({constructor:function(url,options){this.base(options);this.requests=[];this.server_url=url;var root_node=new Node('/');root_node.setConnector(this);this.root_node=root_node;this.user=null;this.loadUser();},request:function(options){var request=new Request(options);request.addedTo(this);this.requests.push(request);this.next();},next:function(){var requests=this.requests;if(requests.length==0)return;var next_request=requests[0];requests.splice(0,1);next_request.start();},loadUser:function(continuation){if(this.user!=null&&continuation)
continuation(this.user);var url=paths.join(this.server_url,'whoami');var self=this;this.request({url:url,success:function(user){self.user=user;if(continuation)
continuation(user);}});},load:function(path,continuation){var node=this.root_node.addNode(path);if(node.is_loaded)
continuation(node);else{var url=paths.join(this.server_url,path);jQuery.get(url,{},function(json){var node_data=eval('('+json+')');node.initializeFrom(node_data);continuation(node);});}},find:function(path,depth,continuation){},create:function(path){var node=this.root_node.addNode(path);node.is_new=true;return node;},save:function(continuation){var self=this;this.root_node.eachNode(function(node){if(node.isChanged())
self.update(node);});},update:function(node){var url=paths.join(this.server_url,node.getPath());var changes=JSONstring.make(node.getChanges());jQuery.ajax({type:"PUT",url:url,data:{properties:changes}});node.commit();}});var Node=TreeNode.extend({class_name:"Node",constructor:function(name,properties){this.base(name,properties);this.is_loaded=false;this.is_new=false;for(var index=2;index<arguments.length;index+=1)
this.addChild(arguments[index]);},setConnector:function(connector){this.eachNode(function(node){node.connector=connector;});},makeNode:function(name){return new Node(name);},addedTo:function(parent){this.base(parent);var connector=parent.connector;if(connector)
this.setConnector(connector);},initializeFrom:function(node_data){this.class_name=node_data['class'];this.uuid=node_data.uuid;this.tags=node_data.tags;this.permission=node_data.permission;this.setProperty('title',node_data.title);var content=node_data.content;for(var name in content)
this.setProperty(name,content[name]);this.initializeChildren(node_data.children);this.is_loaded=true;},initializeChildren:function(children){for(var index=0;index<children.length;index+=1){var child_data=children[index];var child=this.addNode(child_data.name);if(child.is_loaded)
continue;child.class_name=child_data['class'];child.uuid=child_data.uuid;child.tags=child_data.tags;child.count=child_data.count;child.setProperty('title',child_data.title);var content=child_data.content;for(var name in content)
child.setProperty(name,content[name]);}}});var Request=Options.extend({constructor:function(options){this.base(options);options.type=this.option("type","GET");options.dataType=this.option("dataType","json");this.on_success=this.option('success',null);this.on_error=this.option('error',null);this.is_started=false;this.is_complete=false;},addedTo:function(connector){this.connector=connector;},start:function(){var self=this;this.options.success=function(result,status){self.onSuccess(result,status);};this.options.error=function(request,status){self.onError(request,status);};this.is_started=true;jQuery.ajax(this.options);},onSuccess:function(result,status){this.is_complete=true;this.connector.next();var on_success=this.on_success;if(on_success!=null)
on_success(result,status);},onError:function(request,status){this.is_complete=true;this.connector.next();var on_error=this.on_error;if(on_error!=null)
on_error(request,status);}});Request.implement(OptionsMixin);eval(this.exports);};

// core/jsonStringify.js
JSONstring={compactOutput:false,includeProtos:false,includeFunctions:false,detectCirculars:true,restoreCirculars:true,make:function(arg,restore){this.restore=restore;this.mem=[];this.pathMem=[];return this.toJsonStringArray(arg).join('');},toObject:function(x){if(!this.cleaner){try{this.cleaner=new RegExp('^("(\\\\.|[^"\\\\\\n\\r])*?"|[,:{}\\[\\]0-9.\\-+Eaeflnr-u \\n\\r\\t])+?$')}
catch(a){this.cleaner=/^(true|false|null|\[.*\]|\{.*\}|".*"|\d+|\d+\.\d+)$/}};if(!this.cleaner.test(x)){return{}};eval("this.myObj="+x);if(!this.restoreCirculars||!alert){return this.myObj};if(this.includeFunctions){var x=this.myObj;for(var i in x){if(typeof x[i]=="string"&&!x[i].indexOf("JSONincludedFunc:")){x[i]=x[i].substring(17);eval("x[i]="+x[i])}}};this.restoreCode=[];this.make(this.myObj,true);var r=this.restoreCode.join(";")+";";eval('r=r.replace(/\\W([0-9]{1,})(\\W)/g,"[$1]$2").replace(/\\.\\;/g,";")');eval(r);return this.myObj},toJsonStringArray:function(arg,out){if(!out){this.path=[]};out=out||[];var u;switch(typeof arg){case'object':this.lastObj=arg;if(this.detectCirculars){var m=this.mem;var n=this.pathMem;for(var i=0;i<m.length;i++){if(arg===m[i]){out.push('"JSONcircRef:'+n[i]+'"');return out}};m.push(arg);n.push(this.path.join("."));};if(arg){if(arg.constructor==Array){out.push('[');for(var i=0;i<arg.length;++i){this.path.push(i);if(i>0)
out.push(',\n');this.toJsonStringArray(arg[i],out);this.path.pop();}
out.push(']');return out;}else if(typeof arg.toString!='undefined'){out.push('{');var first=true;for(var i in arg){if(!this.includeProtos&&arg[i]===arg.constructor.prototype[i]){continue};this.path.push(i);var curr=out.length;if(!first)
out.push(this.compactOutput?',':',\n');this.toJsonStringArray(i,out);out.push(':');this.toJsonStringArray(arg[i],out);if(out[out.length-1]==u)
out.splice(curr,out.length-curr);else
first=false;this.path.pop();}
out.push('}');return out;}
return out;}
out.push('null');return out;case'unknown':case'undefined':case'function':if(!this.includeFunctions){out.push(u);return out};arg="JSONincludedFunc:"+arg;out.push('"');var a=['\n','\\n','\r','\\r','"','\\"'];arg+="";for(var i=0;i<6;i+=2){arg=arg.split(a[i]).join(a[i+1])};out.push(arg);out.push('"');return out;case'string':if(this.restore&&arg.indexOf("JSONcircRef:")==0){this.restoreCode.push('this.myObj.'+this.path.join(".")+"="+arg.split("JSONcircRef:").join("this.myObj."));};out.push('"');var a=['\n','\\n','\r','\\r','"','\\"'];arg+="";for(var i=0;i<6;i+=2){arg=arg.split(a[i]).join(a[i+1])};out.push(arg);out.push('"');return out;default:out.push(String(arg));return out;}}};

// controls/label.js
new function(_){var label=new base2.Package(this,{name:"label",version:"0.1",imports:"generator,controls",exports:"Label"});eval(this.imports);var Label=Control.extend({constructor:function(text,options){this.base(options,{css_class:'wa_label'});this.text=text;},element_tag:'span',generate:function(dom_element){dom_element.append(this.text);},setText:function(text){this.dom_element.html(text);}});eval(this.exports);};

// controls/buttons.js
new function(_){var buttons=new base2.Package(this,{name:"buttons",version:"0.1",imports:"observers,generator,controls",exports:"Button"});eval(this.imports);var Button=Control.extend({class_name:"Button",constructor:function(options){this.base(options);this.is_pressed=this.option("pressed",false);this.label=this.option("label",null);this.toggles=this.option("toggles",false);this.is_active=this.option("active",true);this.is_down=this.is_pressed;},element_tag:'span',generate:function(dom_element){var label=this.label;dom_element.addClass("wa_button").append(label);var me=this;dom_element.bind('mousedown',function(){me.toggle();});dom_element.bind('mouseup',function(){me.action();});},update:function(){var dom_element=this.dom_element;if(!this.is_active)
dom_element.addClass('wa_button_grey');else{dom_element.removeClass('wa_button_grey');if(this.is_down)
dom_element.addClass('wa_button_pressed');else
dom_element.removeClass('wa_button_pressed');}},toggle:function(){this.is_down=!this.is_down;this.update();},setPressed:function(is_pressed){if(is_pressed==this.is_pressed)
return;this.is_pressed=is_pressed;this.is_down=is_pressed;this.update();this.broadcast("changed",this.is_pressed);},setActive:function(is_active){if(this.is_active!=is_active){this.is_active=is_active;this.update();}},action:function(){if(!this.is_active)return;if(this.toggles){this.is_pressed=!this.is_pressed;this.broadcast("changed",this.is_pressed);}
else{this.is_pressed=true;this.broadcast("changed",this.is_pressed);this.is_pressed=false;}
this.is_down=this.is_pressed;this.update();}});eval(this.exports);};

// controls/tabs.js
new function(_){var tabs=new base2.Package(this,{name:"tabs",version:"0.1",imports:"observers,generator,controls",exports:"TabRow"});eval(this.imports);var DEFAULT_TAB_WIDTH=80;var TabRow=Control.extend({constructor:function(options){this.base(options,{css_class:'wa_tab_row'});this.choices=this.option("choices",[]);this.selected=-1;this.value=this.option("value",null);this.tabs=new Array();},generate:function(dom_element){dom_element.append(div({'class':'wa_tab_slide'},table({'class':'wa_tab_table'},tr({}))));var row=jQuery("tr",dom_element);var choices=this.choices;for(var index=0;index<this.choices.length;index+=1){var choice=choices[index];if(typeof choice=="string")
this.addTab(dom_element,choice,index);else{value=choice.value;if(typeof value=="undefined")
value=index;this.addTab(dom_element,choice.label,value,choice.width,choice.has_close);}}
this.update();},addTab:function(dom_element,label,value,width,has_close){if(typeof width=="undefined")
width=null;if(typeof has_close=="undefined")
has_close=false;var tab_button=new TabButton(label,value,width,has_close);var row=jQuery("tr",dom_element);tab_button.makeControl(row);tab_button.tabAddedTo(this,this.tabs.length);this.tabs.push(tab_button);},selectTab:function(index){if(index==this.selected)return;this.value=this.tabs[index].value;this.update();this.broadcast("changed",this.value,this.index);},closeTab:function(index){var tabs=this.tabs;var removed=tabs[index];tabs.splice(index,1);removed.tabRemovedFrom(this);this.renumberTabs();if(index<=this.selected)
this.selectTab(this.selected-1);this.update();this.broadcast("closed",removed.value);},renumberTabs:function(){var tabs=this.tabs;for(var index=0;index<tabs.length;index+=1)
tabs[index].index=index;},deselectAll:function(){var tabs=this.tabs;for(var index=0;index<tabs.length;index+=1)
tabs[index].setSelected(false);this.selected=-1;},update:function(){this.deselectAll();var value=this.value;var tabs=this.tabs;for(var index=0;index<tabs.length;index+=1){if(tabs[index].value==value){tabs[index].setSelected(true);this.selected=index;}}}});var TabButton=Control.extend({constructor:function(label,value,width,has_close){this.base({},{css_class:'wa_tab',width:width});this.label=label;this.value=value;this.has_close=has_close;this.selected=false;},tabAddedTo:function(tab_row,index){this.tab_row=tab_row;this.index=index;},tabRemovedFrom:function(tab_row){if(tab_row!=this.tab_row)return;this.tab_row=null;this.dom_element.hide("fast");},element_tag:'td',generate:function(dom_element){var span_html=span({'class':'wa_tab_label'},this.label);dom_element.append(span_html);dom_element.append(span({}));if(this.has_close)
jQuery("span:eq(1)",dom_element).addClass('wa_tab_close');this.attach(dom_element);},attach:function(dom_element){var me=this;dom_element.click(function(){me.select();});jQuery("span:eq(1)",dom_element).click(function(){me.close();});},select:function(){var tab_row=this.tab_row;if(tab_row==null)return;tab_row.selectTab(this.index);},setSelected:function(is_selected){if(this.is_selected==is_selected)return;this.is_selected=is_selected;this.update();},close:function(){var tab_row=this.tab_row;if(tab_row==null)return;tab_row.closeTab(this.index);},update:function(){var dom_element=this.dom_element;if(this.is_selected)
dom_element.addClass("wa_tab_pressed");else
dom_element.removeClass("wa_tab_pressed");}});eval(this.exports);};

// controls/forms.js
new function(_){var forms=new base2.Package(this,{name:"forms",version:"0.2",imports:"controls,generator",exports:"Form,LabelItem,TextItem,PasswordItem,TagsItem,CheckboxItem,PulldownItem,"
+"FileItem,RadioItem,DateItem,SaveButton"});eval(this.imports);var FormItem=Control.extend({class_name:"FormItem",element_tag:'tr',constructor:function(name,options){this.base(options);this.name=name;this.is_valid=true;this.setValue(this.option('value'));},getForm:function(){var examine=this;while(examine!=null&&!(examine instanceof Form))
examine=examine.parent;return examine;},getItem:function(name){var form=this.getForm();if(form==null)return null;return form.getItem(name);},addedTo:function(parent){this.base(parent);var form=this.getForm();if(form!=null){form.registerItem(this);this.each(function(child){form.registerItem(child);});}},setValue:function(new_value){if(new_value==this.value)return;this.value=new_value;this.update();},update:function(){},say:function(){var parts=[];for(var index=0;index<arguments.length;index+=1)
parts.push(arguments[index]);var message_div=jQuery("div.wa_form_message",this.control_cell);message_div.html(parts.join(""));},check:function(){this.say("");this.is_valid=this.validate();return this.is_valid;},validate:function(){return this.validateRequired();},validateRequired:function(){var is_optional=this.option("optional",false);if(!is_optional){var value=jQuery.trim(this.value);if(value==""){this.say('*'+this.getLabel()+' is required');return false;}}
return true;},itemName:function(name){if(typeof name=="undefined")
name=this.name;var form=this.getForm();var object=form.object;if(object==null)
return name;else
return object+'['+name+']';},getLabel:function(){var label=this.option("label");if(label==null){label=this.name;label=label.charAt(0).toUpperCase()+label.substr(1);}
return label;},generate:function(dom_element){var label_text=this.getLabel();if(label_text!="")
dom_element.append(td({'class':'wa_form_label'},this.getLabel()+":"));else
dom_element.append(td());dom_element.append(td({},""));var control_cell=jQuery("td:eq(1)",dom_element);this.control_cell=control_cell;this.generateControl(control_cell);control_cell.append(div({'class':'wa_form_message'}));},generateControl:function(control_cell){throw new Error("Implement in subclasses.");}});var LabelItem=FormItem.extend({class_name:"LabelItem",constructor:function(name,options){this.base(name,options);},generateControl:function(control_cell){control_cell.append(span({'class':'wa_text'},this.value));}});var TextItem=FormItem.extend({class_name:"TextItem",constructor:function(name,options){this.base(name,options);this.lines=this.option('lines',1);this.length=this.option('length');this.allow=this.option('allow');this.match=this.option('match');this.value='';},generateControl:function(control_cell){if(this.lines>1)
this.generateTextArea(control_cell);else
this.generateTextItem(control_cell);},generateTextItem:function(control_cell){var item_name=this.itemName();var attributes={type:'text',name:item_name};if(this.length==null){attributes['class']='wa_text_item';}
else{attributes['maxlength']=this.length;attributes['size']=this.length;attributes['class']='wa_form_text_var';}
control_cell.append(input(attributes));this.attach(jQuery("> input",control_cell));},generateTextArea:function(control_cell){var item_name=this.itemName();control_cell.append(textarea({name:item_name,"class":'wa_text_area',rows:this.lines}));this.control=jQuery("> textarea",control_cell);this.attach(this.control);},attach:function(control){this.control=control;var self=this;control.blur(function(event){self.setValue(control.val());self.check();});if(this.allow!=null){control.keyup(function(event){self.onChange(control,event);});}},onChange:function(control,event){var value=control.val();if(value.match(this.allow))
this.value=value;else
control.val(this.value);},validate:function(){if(!this.base())
return false;if(this.match!=null&&!this.value.match(this.match)){this.say("Invalid input");return false;}
return true;},update:function(){if(typeof(this.control)!="undefined")
this.control.val(this.value);}});var CheckboxItem=FormItem.extend({class_name:"CheckboxItem",constructor:function(name,options){options.optional=true;this.base(name,options);},generateControl:function(control_cell){var item_name=this.itemName();var comment=this.option("comment","");control_cell.append(input({type:'checkbox',name:item_name,"class":'wa_checkbox_item'})+comment);this.checkbox=jQuery('input',control_cell);var self=this;this.checkbox.change(function(event){self.onChange(event);});},update:function(){if(this.checkbox!=null)
this.checkbox.attr("checked",this.value);},onChange:function(event){var value=jQuery(event.target).attr("checked");this.setValue(value);}});var RadioItem=FormItem.extend({class_name:"RadioItem",constructor:function(name,options){this.base(name,options);this.choices=this.option("choices",[]);this.values=this.option("values",this.choices);},generateControl:function(control_cell){var item_name=this.itemName();var choices=this.choices;var controls=[];for(var index in choices)
controls.push(input({name:item_name,type:"radio",value:index},choices[index]));control_cell.append(controls.join(""));var self=this;this.control=jQuery("input[name='"+item_name+"']");this.control.change(function(event){self.onChange(event);});},update:function(){if(this.control==null||(typeof this.value)=="undefined")
return;var values=this.values;var value=this.value;var index=0;this.control.each(function(){this.checked=(values[index]==value);index+=1;});},onChange:function(event){var index=jQuery(event.target).val();var value=this.values[index];this.setValue(value);}});var FileItem=FormItem.extend({class_name:"FileItem",constructor:function(name,options){this.base(name,options);var extensions=this.option("extensions",[]);this.setExtensions(extensions);},generateControl:function(control_cell){var item_name=this.itemName();control_cell.append(input({type:'file',name:item_name,"class":'wa_file_item'}));var control=jQuery('input',control_cell);this.control=control;var self=this;control.blur(function(event){self.onBlur();});control.change(function(event){self.onChange(event);});},setExtensions:function(extensions){if(typeof extensions=="string")
extensions=[extensions];for(var index=0;index<extensions.length;index+=1){var extension=extensions[index];if(extension[0]!='.'){extensions[index]='.'+extension;}}
this.extensions=extensions;},update:function(){if(this.dom_element!=null){var value=this.value;if(typeof(value)=="undefined"||value==null)
value='';this.dom_element.val(value);}},validate:function(){this.base();if(this.extensions.length==0)
return false;var name=this.control.val();if(!this.matchesExtension(name,this.extensions)){this.say("*Must end with: "
+formatList(this.extensions,"or"));return false;}
return true;},matchesExtension:function(name,extensions){for(var index=0;index<extensions.length;index+=1){var extension=extensions[index];var part=name.substring(name.length-extension.length);if(part==extension)
return true;}
return false;},onBlur:function(){this.setValue(this.control.val());this.validate();},onChange:function(event){var value=this.control.val();this.setValue(value);this.check();}});var PulldownItem=FormItem.extend({class_name:"PulldownItem",constructor:function(name,options){this.base(name,options);this.choices=this.option("choices",[]);this.values=this.option("values",this.choices);},generateControl:function(control_cell){var controls=[];var choices=this.choices;var item_name=this.itemName();for(var index=0;index<choices.length;index+=1)
controls.push(option({value:index},choices[index]));control_cell.append(select({"class":"wa_select_item"},controls)+
input({name:item_name,type:"hidden"}));this.attach(control_cell);},attach:function(control_cell){var self=this;this.selector=jQuery("select",control_cell);this.selector.change(function(event){self.onChange(event);});this.value_field=jQuery('input',control_cell);},update:function(){var values=this.values;for(var index=0;index<values.length;index+=1){if(this.value==values[index])
this.selector.val(index);}},onChange:function(event){var index=jQuery("option:selected",event.target).val();var value=this.values[index];this.setValue(value);this.setHiddenField(value);},setHiddenField:function(value){var hidden_field=jQuery("input[type=hidden]",this.dom_element);hidden_field.val(value);}});var TagsItem=FormItem.extend({class_name:"TagsItem",constructor:function(name,options){this.base(name,options);this.tag_value=this.value;if(this.tag_value==null)
this.tag_value="";},generateControl:function(control_cell){var item_name=this.itemName();control_cell.append(div({"class":"wa_tag_selector"},"-- no tags --"),div({style:"position:relative;"},input({type:'text',"class":'wa_tag_field'}),input({type:'hidden',name:item_name}),div({"class":"wa_tag_button"},a({href:"#","class":"wa_tag_link"},"Add Tag"))));this.attach(control_cell);},attach:function(control_cell){var self=this;this.selector=jQuery(".wa_tag_selector",control_cell);this.tag_field=jQuery("input:eq(0)",control_cell);this.tag_field.keyup(function(event){self.onChange(event);});this.tag_field.blur(function(event){self.onAction();return false;});this.tag_button=jQuery(".wa_tag_link",control_cell);this.tag_button.click(function(event){self.onAction();return false;});},onChange:function(event){var tag_field=this.tag_field;var tag_value=tag_field.val();if(tag_value.match(/^[ a-zA-Z0-9,]*$/)){var tokens=tag_value.split(",");var count=tokens.length;for(var index=0;index<(count-1);index+=1)
this.addTag(tokens[index]);if(count>1){tag_value=reduceBlanks(tokens[tokens.length-1]);tag_field.val(tag_value);}
this.tag_value=tag_value;}
else
tag_field.val(this.tag_value);this.updateSelection();},onAction:function(){var field_text=jQuery.trim(this.tag_field.val());if(field_text=="")return;var tags=this.value.slice(0);var index=this.findTag(field_text);if(index>=0){tags.splice(index,1);this.setValue(tags);this.tag_field.val("");this.updateSelection();this.update();}
else{tags.push(field_text);this.setValue(tags);this.tag_field.val("");this.updateSelection();this.update();}},addTag:function(tag_value){if(!tag_value.match(/^[ a-zA-Z0-9]*$/))
return;var tag_value=reduceBlanks(tag_value);var index=this.findTag(tag_value);if(index==-1){var tags=this.value.slice(0);tags.push(tag_value);this.setValue(tags);this.update();}},setValue:function(value){if(value==null)
value=[];if(typeof value=="string")
value=value.split(",");for(var index=0;index<value.length;index+=1){var tag=reduceBlanks(value[index]);if(tag!="")
value[index]=tag;}
this.base(value);this.setHiddenField(value);},setHiddenField:function(tags){var hidden_field=jQuery("input[type='hidden']",this.dom_element);hidden_field.val(tags.join(","));},update:function(){if(this.dom_element==null)return;var tags=this.value;if(tags==null)
tags=[];var links=new Array();if(tags.length==0)
links.push("-- no tags --");for(var index=0;index<tags.length;index+=1)
links.push(a({href:"#"},tags[index]));this.selector.html(links.join(", "));var me=this;jQuery("a",this.selector).each(function(index,element){me.setupTagLink(tags[index],element);});},setupTagLink:function(tag,element){var me=this;jQuery(element).click(function(){me.selectTag(tag);return false;});},selectTag:function(tag){this.tag_field.val(tag);this.updateSelection();},findTag:function(tag){tag=reduceBlanks(tag);var tags=this.value;for(var index=0;index<tags.length;index+=1){if(tags[index]==tag)return index;}
return-1;},updateSelection:function(){jQuery("a",this.selector).removeClass("wa_tag_selected");var field_text=this.tag_field.val();var index=this.findTag(field_text);if(index>=0){this.tag_button.html("Remove Tag");}
else{this.tag_button.html("Add Tag");}},validateRequired:function(){var is_optional=this.option("optional",false);if(!is_optional){var value=this.value;if(typeof(value)=="undefined"||value==null||value.length==0){this.say('*'+this.getLabel()+' is required');return false;}}
return true;}});var PasswordItem=FormItem.extend({class_name:"PasswordItem",constructor:function(name,options){this.base(name,options);this.confirm=this.option('confirm',false);},generateControl:function(control_cell){var item_name=this.itemName();control_cell.append(input({type:'password',name:item_name,"class":'wa_text_item'}));if(this.confirm){var confirm_name=item_name+"_confirm";control_cell.append(div({'class':'wa_form_password_confirm'},"Confirm Password:")+
input({type:'password',"class":'wa_text_item'}));}
this.attach(control_cell);},attach:function(control_cell){var self=this;var password_field=jQuery("input",control_cell);password_field.keydown(function(event){self.onChange(event);});password_field.blur(function(event){self.onBlur();});this.password_field=password_field;if(this.confirm){var confirm_field=jQuery('input:eq(1)',control_cell);confirm_field.keydown(function(event){self.onChange(event);});confirm_field.blur(function(event){self.onBlur();});this.confirm_field=confirm_field;}},onChange:function(event){},onBlur:function(){this.setValue(this.dom_element.val());this.check();},validate:function(){this.base();var password=this.password_field.val();var length=password.length;if(length>0&&length<this.count){this.say("*At least "+
this.count+" characters");return false;}
if(this.confirm){var confirm_password=this.confirm_field.val();if(confirm_password!=''&&password!=confirm_password){this.say("*Passwords do not match");return false;}}
return true;},update:function(){}});var MONTH_NAMES=["jan","january","feb","february","mar","march","apr","april","may","may","jun","june","jul","july","aug","august","sep","september","oct","october","nov","november","dec","december"];var DATE_PATTERN=new RegExp("^[0-3][0-9] *("
+MONTH_NAMES.join('|')+") [12]*[0-9]{3}$");var DateItem=FormItem.extend({class_name:"DateItem",constructor:function(name,options){this.base(name,options);},generateControl:function(control_cell){var item_name=this.itemName();control_cell.append(input({name:item_name,'class':'wa_form_text_var'}));this.control=jQuery('input',control_cell);this.control.datepicker({dateFormat:'dd M yy',changeMonth:true,changeYear:true});var self=this;this.control.change(function(event){self.onChange(event);});},onChange:function(event){var value=this.control.val();this.setValue(value);this.check();},validate:function(){this.base();var value=this.control.val().toLowerCase();if(!value.match(DATE_PATTERN)){this.say("*Must use \"12 Jan 2001\" format")
return false;}
return true;},update:function(){var value=this.value;if(value==null||typeof(value)=="undefined")
value='';if(value instanceof Date)
value=jQuery.datepicker.formatDate('dd M yy',value);this.control.val(value);}});var SaveButton=FormItem.extend({class_name:"SaveButton",constructor:function(name,options){this.base(name,options);},generate:function(dom_element){dom_element.append(td({'class':'wa_form_label'}));dom_element.append(td({},""));var control_cell=jQuery("td:eq(1)",dom_element);this.control_cell=control_cell;this.generateControl(control_cell);control_cell.append(div({'class':'wa_form_message'}));},generateControl:function(control_cell){var label=this.getLabel();var button_type="button";if(this.option("submit",false))
button_type="submit";control_cell.append(input({type:button_type,value:label,'class':'wa_form_submit'}));this.submit_button=jQuery('input:eq(0)',control_cell);var self=this;this.submit_button.click(function(event){self.onSave(event);});if(this.option('cancel',false)){control_cell.append('&nbsp;'+
input({type:'button','value':"Cancel",'class':'wa_form_cancel'}));this.cancel_button=jQuery('input:eq(1)',control_cell);this.cancel_button.click(function(event){self.onCancel(event);});}},validate:function(){this.is_valid=true;return true;},onSave:function(event){var form=this.getForm();if(form==null)
return false;if(form.onSave())
return true;event.preventDefault();return false;},onCancel:function(){var form=this.getForm();if(form!=null)
return form.onCancel();return false;}});var GroupItem=FormItem.extend({class_name:"GroupItem",constructor:function(name,options){this.base(name,options);},generateControl:function(control_cell){}});var Form=GroupItem.extend({class_name:"Form",element_tag:'div',constructor:function(options){this.base("Form",options);this.object=this.option('object');this.item_map={};if(arguments.length>1)
for(var index=1;index<arguments.length;index+=1)
this.add(arguments[index]);},registerItem:function(item){this.item_map[item.name]=item;},getItem:function(name){var item=this.item_map[name];if(typeof item=="undefined")
return null;else
return item;},setItemValue:function(name,value){var item=this.getItem(name);if(item!=null)
item.setValue(value);},getItemValue:function(name){var item=this.getItem(name);if(item==null)
return null;var value=item.value;if(typeof value=="string")
value=encodeUnicode(value);return value;},setData:function(source_object){this.each(function(item){var value=source_object[item.name];if(typeof value=="undefined")
value=null;if(typeof value=="string")
value=decodeUnicode(value);item.setValue(value);});},updateData:function(destination_object){var me=this;this.each(function(item){var value=item.value;if(typeof value=="string")
value=encodeUnicode(value);destination_object[item.name]=value;});},getJSONString:function(){var data={};this.updateData(data);return JSONstring.make(data);},onSave:function(){if(this.checkForm()){this.broadcast("onSave");return true;}
else{this.broadcast("onInvalid");return false;}},onCancel:function(){this.broadcast("onCancel");return false;},message:function(item,message){this.message_element.html(message);},checkForm:function(){var error_item=this.find(function(item){return!item.check();});return error_item==null;},generate:function(dom_element){var form_options=this.makeFormOptions();dom_element.append(form(form_options,table({'class':'wa_form_table'})));this.generateAuthToken(jQuery('form',dom_element));this.setContentsElement(jQuery('table',dom_element));},makeFormOptions:function(){var form_options={};var method=this.option("method");if(method!=null)
form_options.method=method;var action=this.option("action");if(action!=null)
form_options.action=action;var enctype=this.option("enctype");if(enctype!=null)
form_options.enctype=enctype;return form_options;},generateAuthToken:function(form_element){var token=this.option("auth_token");if(token==null)return;form_element.append(input({name:"authenticity_token",type:"hidden",value:token}));}});function reduceBlanks(text){var out=[];var blank_count=0;for(var index=0;index<text.length;index+=1){var next_char=text.charAt(index);if(next_char==' ')
blank_count+=1;else{if(blank_count>0&&out.length>0)
out.push(' ');blank_count=0;out.push(next_char);}}
return out.join('');}
function formatList(items,or_and){if(typeof or_and=="undefined")
or_and="or";var parts=new Array();var last_index=items.length-1;for(var index=0;index<=last_index;index+=1){if(index==last_index&&last_index>0)
parts.push(", "+or_and+" ");else if(index>0)
parts.push(", ");parts.push(items[index].toString());}
return parts.join("");}
function encodeUnicode(text){var result=new Array();for(var index=0;index<text.length;index+=1){var character=text.charAt(index);if(character<="\u007F")
result.push(character);else
result.push("&#"+character.charCodeAt(0)+";");}
return result.join("");}
function decodeUnicode(text){var parts=text.split(/(&#[0-9]+;)/g);for(var index=0;index<parts.length;index+=1){var part=parts[index];var digits=part.match(/&#([0-9]+);/);if(digits!=null){parts[index]=String.fromCharCode(digits[1]);}}
return parts.join("");}
eval(this.exports);};

// controls/tables.js
new function(_){var tables=new base2.Package(this,{name:"tables",version:"0.1",imports:"observers,options,generator,controls,list_models",exports:"Table,Column,DateColumn"});eval(this.imports);var DEFAULT_COLUMN_WIDTH=100;var DEFAULT_TABLE_HEIGHT=260;var BORDER_WIDTH=0;var SCROLL_BORDER=17;var TOP_BAR_HEIGHT=26;var IE_PADDING=6;var Column=Broadcaster.extend({constructor:function(id,options){this.base();this.setOptions(options);this.id=id;this.index=-1;this.label=this.option("label",id);this.width=this.option("width",DEFAULT_COLUMN_WIDTH);this.sortable=this.option("sortable",false);this.is_sorted=this.option("sorted",false);this.is_ascending=this.option("ascending",true);this.css_class=this.option("class");this.getValue=this.option("get");if(this.getValue==null)
this.getValue=function(source){return source[id];};},setSortable:function(is_sortable){if(this.sortable==is_sortable)return;this.sortable=is_sortable;this.broadcast("changed");},setSorted:function(is_sorted){if(this.is_sorted==is_sorted)
return;this.is_sorted=is_sorted;this.broadcast("changed");},setAscending:function(is_ascending){if(!this.sortable||this.is_ascending==is_ascending)
return;this.is_ascending=is_ascending;this.broadcast("changed");},computedWidth:function(){var width=this.width-BORDER_WIDTH;if(jQuery.browser.msie)
width-=IE_PADDING;return width;},compareItems:function(item_a,item_b){if(!(this.sortable&&this.is_sorted))
return 0;var value_a=this.getValue(item_a);if(typeof(value_a)=="string")
value_a=value_a.toLowerCase();var value_b=this.getValue(item_b);if(typeof(value_b)=="string")
value_b=value_b.toLowerCase();var result=0;if(value_a<value_b)
result=-1;else if(value_a>value_b)
result=+1;if(!this.is_ascending)
result=-result;return result;},render:function(item,index){var attributes={};if(this.css_class!=null)
attributes['class']=this.css_class;return td(attributes,this.getValue(item));}});Column.implement(OptionsMixin);var DateColumn=Column.extend({compareItems:function(item_a,item_b){if(!(this.sortable&&this.is_sorted))
return 0;var text_a=this.getValue(item_a);var text_b=this.getValue(item_b);if(text_a==null||text_a==''){if(text_b==null||text_b=='')
return 0;else
return-1;}
if(text_b==null||text_b=='')
return+1;var value_a=jQuery.datepicker.parseDate('dd M yy',text_a);var value_b=jQuery.datepicker.parseDate('dd M yy',text_b);var result=0;if(value_a<value_b)
result=-1;else if(value_a>value_b)
result=+1;if(!this.is_ascending)
result=-result;return result;}});var Table=Control.extend({class_name:"Table",constructor:function(options){this.base(options);this.initializeColumns(this.option("columns",[]));this.filter=null;var model=this.option("model",null);if(model!=null)
this.setModel(model);this.height=this.option("height",DEFAULT_TABLE_HEIGHT);this.selected=-1;this.row_elements=new Array();},initializeColumns:function(columns){for(var index=0;index<columns.length;index+=1){var column=columns[index];if(typeof column=="string")
columns[index]=new Column(column);}
this.columns=columns;this.measure();},getState:function(){var state_object={};var columns=this.columns;for(var index=0;index<columns.length;index+=1){if(columns[index].is_sorted){state_object.sort_column=index;state_object.sort_ascending=columns[index].is_ascending;break;}}
this.table_body.saveState(state_object);return state_object;},restoreState:function(state_object){var columns=this.columns;if(typeof(state_object.sort_column)=="number"){var column=columns[state_object.sort_column];column.setSorted(true);column.setAscending(state_object.sort_ascending);}
this.table_body.restoreState(state_object);},measure:function(){var columns=this.columns;var width=0;for(var index=0;index<columns.length;index+=1)
width+=columns[index].width;if(jQuery.browser.msie)
width+=1;this.inner_width=width;this.width=this.inner_width+SCROLL_BORDER;},setModel:function(model){this.model=model;this.update();},generate:function(dom_element){var table_header=new TableHeader(this.columns,{width:this.width,inner_width:this.inner_width});table_header.makeControl(dom_element);var table_body=new TableBody(this.filter,this.columns,{width:this.width,inner_width:this.inner_width,height:this.height-TOP_BAR_HEIGHT});table_body.makeControl(dom_element);table_body.addListener("select",this);this.table_body=table_body;},update:function(){var model=this.model;if(model!=null&&this.filter==null&&typeof(this.table_body)!="undefined"){this.filter=new ListFilter(model);this.table_body.setModel(this.filter);}},select:function(index,item){this.broadcast("select",index,item);},getSelectedIndex:function(){return this.table_body.getSelectedIndex();},getSelectedItem:function(){return this.table_body.getSelectedItem();}});var TableHeader=Control.extend({class_name:"TableHeader",constructor:function(columns,options){this.base(options,{css_class:"wa_table_header"});this.inner_width=this.option('inner_width');this.columns=columns;},generate:function(dom_element){var table_id=this.id+"_table";var columns=this.columns;var col_html=[];for(var index=0;index<columns.length;index+=1){var column_width=columns[index].computedWidth();col_html.push(col({style:{width:column_width}}));}
dom_element.append(table({id:table_id},col_html,tr()));var row_element=jQuery("#"+table_id+" tr");for(var index=0;index<columns.length;index+=1)
this.addColumn(index,columns[index],row_element);},addColumn:function(index,column,row_element){column.addListener("changed",this);var button=new ColumnButton({model:column});button.makeControl(row_element);},changed:function(source){if(source.is_sorted){var columns=this.columns;for(var index=0;index<columns.length;index+=1){var column=columns[index];column.setSorted(column===source);}}}});var ColumnButton=Control.extend({class_name:"ColumnButton",constructor:function(options){var model=options["model"];this.base(options,{css_class:"wa_table_header_cell"});this.setModel(model);},setModel:function(model){this.model=model;var self=this;model.addListener("changed",this,"update");this.resetTooltip();},element_tag:'th',generate:function(dom_element){var model=this.model;dom_element.append(span({'class':'wa_sort_text'},model.label));dom_element.append(span({}," "));var self=this;dom_element.click(function(){self.toggle();});this.resetTooltip();},resetTooltip:function(){var model=this.model;var dom_element=this.dom_element;if(model==null||dom_element==null)
return;if(model.sortable)
dom_element.attr('title',"Click to sort column");else
dom_element.attr('title',null);},toggle:function(){var model=this.model;if(model.sortable){if(model.is_sorted)
model.setAscending(!model.is_ascending);else
model.setSorted(true);}},update:function(){var dom_element=this.dom_element;var icon=jQuery("span:eq(1)",dom_element);var model=this.model;if(model.is_sorted){dom_element.addClass("wa_table_cell_highlight");if(model.is_ascending){icon.removeClass("wa_sort_icon_down");icon.addClass("wa_sort_icon_up");}
else{icon.addClass("wa_sort_icon_down");icon.removeClass("wa_sort_icon_up");}}
else{dom_element.removeClass("wa_table_cell_highlight");icon.removeClass("wa_sort_icon_down");icon.removeClass("wa_sort_icon_up");}}});var TableBody=Control.extend({class_name:"TableBody",constructor:function(model,columns,options){this.base(options,{css_class:'wa_table_body',height:200,width:options.width});this.inner_width=this.option('inner_width');this.initializeColumns(columns);this.selected=-1;this.rows=[];this.row_items=[];if(model!=null)
this.setModel(model);},setModel:function(model){if(model===this.model)return;this.model=model;model.addListener("changed",this);model.addListener("added",this);model.addListener("removed",this);model.addListener("filtered",this);if(this.dom_element)
this.generateRows(this.dom_element);},initializeColumns:function(columns){this.columns=columns;for(var index=0;index<columns.length;index+=1)
columns[index].addListener("changed",this,"columnsChanged");},saveState:function(state_object){state_object.selected=this.selected;state_object.scroll=this.dom_element.scrollTop();},restoreState:function(state_object){var scroll=state_object.scroll;var selected=state_object.selected;this.dom_element.scrollTop(scroll);var prior_selected=this.selected;if(selected==prior_selected)return;if(prior_selected!=-1){var row_id=this.id+"_"+prior_selected;var row=jQuery("#"+row_id+" > td",this.dom_element);row.removeClass("wa_table_selected");}
this.selected=selected;var row_id=this.id+"_"+selected;var row=jQuery("#"+row_id+" > td",this.dom_element);row.addClass("wa_table_selected");},generate:function(dom_element){dom_element.css({height:this.height,width:this.width});columns=this.columns;var col_html=[];for(var index=0;index<columns.length;index+=1){var column_width=columns[index].computedWidth();if(jQuery.browser.msie)
column_width-=3;col_html.push(col({style:{width:column_width}}));}
dom_element.append(table({},col_html));this.generateRows(dom_element);},generateRows:function(dom_element){var model=this.model;if(this.model==null)
return;var table=jQuery("#"+this.id+" table");this.table=table;var self=this;model.each(function(item,index){var row_id=self.id+"_"+index;self.appendRow(table,row_id,item,index);self.bindRow(row_id,index,item);});this.rows.length=model.size();},appendRow:function(table,row_id,item,index){var model=this.model;var columns=this.columns;var cells=new Array();var rows=this.rows;var row_items=this.row_items;for(var column_index=0;column_index<columns.length;column_index+=1)
cells.push(columns[column_index].render(item,index));table.append(tr({id:row_id},cells));var item_index=model.getItemIndex(index);rows[item_index]=jQuery("#"+row_id);row_items[index]=item;},bindRow:function(row_id,index,item){var self=this;jQuery("#"+row_id+" td").click(function(){self.select(index,item);});},changed:function(index){var model=this.model;var item=model.get(index);var item_index=model.getItemIndex(index);var row=this.rows[item_index];var columns=this.columns;var cells=new Array();for(var column_index=0;column_index<columns.length;column_index+=1)
cells.push(columns[column_index].render(item,index));row.html(cells.join(""));},added:function(index){this.update();},removed:function(index){this.update();},filtered:function(){this.update();},update:function(){if(this.model==null)return;var rows=this.rows;var model=this.model;var table=this.table;var visible={};var count=model.size();for(var index=0;index<count;index+=1){var item_index=model.getItemIndex(index);visible[item_index]=true;table.append(rows[item_index]);}
for(var item_index=0;item_index<rows.length;item_index+=1){var row=rows[item_index];if(visible[item_index])
row.show();else
row.hide();}},columnsChanged:function(column){this.model.orderBy(function(item_a,item_b){return column.compareItems(item_a,item_b);});},select:function(selected,item){var prior_selected=this.selected;if(selected!=prior_selected){if(prior_selected!=-1){var row_id=this.id+"_"+prior_selected;var row=jQuery("#"+row_id+" > td",this.dom_element);row.removeClass("wa_table_selected");}
this.selected=selected;var row_id=this.id+"_"+selected;var row=jQuery("#"+row_id+" > td",this.dom_element);row.addClass("wa_table_selected");}
this.broadcast("select",selected,item);},getSelectedIndex:function(){if(this.selected<0)return null;return this.selected;},getSelectedItem:function(){if(this.selected<0)return null;return this.row_items[this.selected];}});eval(this.exports);};

// controls/tag_selector.js
new function(_){var tag_selector=new base2.Package(this,{name:"tag_selector",version:"0.1",imports:"observers,generator,controls,buttons,tag_index",exports:"TagSelector"});eval(this.imports);var TagSelector=Control.extend({class_name:"TagSelector",constructor:function(options){this.base(options);this.model=this.option("model");this.tag_index=new TagIndex({list:this.model});this.selected={};this.mode=this.option("mode","one");this.included={};},setModel:function(model){this.model=model;this.tag_index=new TagIndex({list:model});this.generateTags(this.dom_element);this.update();},generate:function(container){container.append(div({id:this.id}));var dom_element=jQuery("#"+this.id);var readout_id=this.id+"_readout";dom_element.append(div({id:readout_id,'class':'wa_tags_readout'}));this.readout=jQuery("#"+readout_id);var tag_panel_id=this.id+"_tags";dom_element.append(div({id:tag_panel_id,'class':'wa_tags_select'}));this.tag_panel=jQuery("#"+tag_panel_id);this.generateTags(dom_element);this.generateButtons(dom_element);return dom_element;},generateButtons:function(dom_element){var button_panel_id=this.id+"_buttons";dom_element.append(div({id:button_panel_id,'class':'wa_tags_buttons'}));var button_panel=jQuery("#"+button_panel_id);var clear_button=new Button({label:"Clear",width:74});clear_button.makeControl(button_panel);clear_button.addListener("changed",this,"clear");var self=this;var one_button=new Button({label:"One",toggles:true,width:30});one_button.makeControl(button_panel);one_button.addListener("changed",function(is_pressed){if(is_pressed)self.setMode("one");});this.one_button=one_button;var and_button=new Button({label:"And",toggles:true,width:30});and_button.makeControl(button_panel);and_button.addListener("changed",function(is_pressed){if(is_pressed)self.setMode("and");});this.and_button=and_button;var or_button=new Button({label:"Or",toggles:true,width:30});or_button.makeControl(button_panel);or_button.addListener("changed",function(is_pressed){if(is_pressed)self.setMode("or");});this.or_button=or_button;},generateTags:function(dom_element){var tag_panel=this.tag_panel;tag_panel.empty();var self=this;var tags=this.tag_index.findBestSelectors(16);this.tags=tags;var frequencies=this.tag_index.getNormalizedFrequencies(tags);for(var index=0;index<tags.length;index+=1){var tag=tags[index];var size=10+Math.round(8*frequencies[tag]);tag_panel.append(span({style:{font_size:size},'class':'wa_tags_tag'},tag)+" ");var link=$("span:eq("+index+")",tag_panel);link.bind("click",tag,function(event){self.selectTag(event.data);});}},getState:function(){var state_object={};state_object.mode=this.mode;var selected=this.selected;var tags=[];for(var name in selected){if(selected[name])tags.push(name);}
state_object.tags=tags;return state_object;},restoreState:function(state_object){this.setMode(state_object.mode);var tags=state_object.tags;for(var index=0;index<tags.length;index+=1)
this.selectTag(tags[index]);},setMode:function(mode){if(mode==this.mode)return;if(mode=="one")
this.selected={};this.mode=mode;this.update();},selectTag:function(tag){if(this.mode=="one")
this.selected={};var is_selected=this.selected[tag];if(typeof(is_selected)=="undefined")
is_selected=false;is_selected=!is_selected;this.selected[tag]=is_selected;this.update();},clear:function(){this.selected={};this.update();},update:function(){this.updateReadout();this.updateTags();this.updateButtons();this.updateIncluded();},updateTags:function(){var selected=this.selected;var tags=this.tags;var tag_panel=this.tag_panel;for(var index=0;index<tags.length;index+=1){var link=$("span:eq("+index+")",tag_panel);if(selected[tags[index]])
link.addClass("wa_tags_selected");else
link.removeClass("wa_tags_selected");}},updateReadout:function(){var readout=this.readout;var selected=this.selected;var tags=this.tags;var parts=[];for(var index=0;index<tags.length;index+=1){var tag=tags[index];if(selected[tag])
parts.push(tag);}
var separater=" and ";if(this.mode=="or")
separater=" or ";var message=parts.join(separater);if(message=="")message="-- All Items --";readout.html(message);},updateButtons:function(){var mode=this.mode;if(mode=="one"){this.one_button.setPressed(true);this.and_button.setPressed(false);this.or_button.setPressed(false);}
else if(mode=="and"){this.one_button.setPressed(false);this.and_button.setPressed(true);this.or_button.setPressed(false);}
else if(mode=="or"){this.one_button.setPressed(false);this.and_button.setPressed(false);this.or_button.setPressed(true);}},updateIncluded:function(){var tags=[];for(var tag in this.selected){if(this.selected[tag])
tags.push(tag);}
var locations=this.tag_index.collectLocations(tags,(this.mode=="and"));var included={};for(var index=0;index<locations.length;index+=1)
included[locations[index]]=true;this.included=included;this.broadcast("changed",included);}});eval(this.exports);};

// controls/text_selector.js
new function(_){var tag_selector=new base2.Package(this,{name:"text_selector",version:"0.1",imports:"observers,generator,controls,buttons,label",exports:"TextSelector"});eval(this.imports);var CLEAR_BUTTON_WIDTH=14;var TextSelector=Control.extend({constructor:function(options){this.base(options);this.width=this.option("width",180);this.fields=this.option("fields",["title"]);this.has_focus=false;},generate:function(dom_element){dom_element.addClass('wa_text_sel');var control_name=this.id+"_input";dom_element.append(input({type:'text','class':'wa_text_sel_input',name:control_name,style:{width:this.width-CLEAR_BUTTON_WIDTH-16}}));this.control=jQuery("> input",dom_element);var self=this;this.control.keyup(function(event){self.onChanged();});this.control.focus(function(){self.onFocus();});this.control.blur(function(){self.onBlur();});var clear_button=new Button({label:"x",width:CLEAR_BUTTON_WIDTH});this.clear_button=clear_button;clear_button.makeControl(dom_element);clear_button.setActive(false);clear_button.addListener("changed",function(){self.clear();});var search_label=new Label("Search");this.search_label=search_label;search_label.makeControl(dom_element);},clear:function(){this.control.val("");this.onChanged();},setValue:function(text){if(text!=this.text){this.text=text;this.pattern=new RegExp(text,"i");this.broadcast("changed",this.pattern);}},onChanged:function(){var text=this.control.val();if(text.length==0&&!this.has_focus)
this.search_label.show();this.clear_button.setActive(text.length>0);this.setValue(text);},onFocus:function(){this.has_focus=true;this.search_label.hide();},onBlur:function(){this.has_focus=false;if(this.text.length==0)
this.search_label.show();},getFilter:function(){var self=this;return function(item,index){var fields=self.fields;var pattern=self.pattern;for(var index=0;index<fields.length;index+=1){var field_name=fields[index];var text=item[field_name].toString();if(text.match(pattern)!=null)
return true;}
return false;}},getState:function(){return{text:this.text};},restoreState:function(state_object){this.control.val(state_object.text);this.onChanged();}});eval(this.exports);};

// controls/dialogs.js
new function(_){var dialogs=new base2.Package(this,{name:"dialogs",version:"0.1",imports:"observers,generator,controls,buttons",exports:"Dialog,OkCancelDialog"});eval(this.imports);var SHADOW_SIZE=6;var Dialog=Control.extend({constructor:function(options,defaults){this.base(defaults,{width:300,height:260,css_class:'wa_dialog'});this.setOptions(options);},generate:function(dom_element){var width=this.option('width');var height=this.option('height');dom_element.append(div({'class':'wa_dialog_shadow',style:{width:width-SHADOW_SIZE,height:height-SHADOW_SIZE,left:SHADOW_SIZE,top:SHADOW_SIZE}}));dom_element.append(div({'class':'wa_dialog_content',style:{width:width-SHADOW_SIZE,height:height-SHADOW_SIZE}}));this.setContentsElement(jQuery('.wa_dialog_content',dom_element));},show:function(){this.dom_element.show("fast");},hide:function(){this.dom_element.hide();}});var OkCancelDialog=Dialog.extend({constructor:function(options){this.base(options,{width:300,height:100,css_class:'wa_dialog'});},generate:function(dom_element){this.base(dom_element);var width=this.width;var height=this.height;var okay_button=new Button({position:'absolute',left:width-118,top:height-30,label:"Ok",width:45});this.add(okay_button);okay_button.addListener("changed",this,"onOkPressed");var cancel_button=new Button({position:'absolute',left:width-63,top:height-30,label:"Cancel",width:45});this.add(cancel_button);cancel_button.addListener("changed",this,"onCancelPressed");var contents_element=this.contents_element;var message_id=this.id+"_message";contents_element.append(div({id:message_id,'class':'wa_dialog_message'}));this.message_area=jQuery('#'+message_id);dom_element.hide();},ask:function(text,ok_callback,cancel_callback){this.ok_callback=ok_callback;this.cancel_callback=cancel_callback;this.message_area.html(text);this.show();},onOkPressed:function(){this.hide();var ok_callback=this.ok_callback;if(ok_callback)ok_callback();},onCancelPressed:function(){this.hide();var cancel_callback=this.cancel_callback;if(cancel_callback)cancel_callback();}});eval(this.exports);};

// controls/checklist.js
new function(_){var checklist=new base2.Package(this,{name:"checklist",version:"0.1",imports:"observers,list_models,generator,controls",exports:"Checklist"});eval(this.imports);var DEFAULT_WIDTH=210;var TABLE_BORDER=15;var CHECKBOX_WIDTH=15;var Checklist=Control.extend({class_name:"Checklist",constructor:function(options){this.base(options,{css_class:'wa_checklist'});this.initializeIdAccessor();this.initializeGroupAccessor();this.initializeLabelAccessor();this.setModel(this.option("model"));this.width=this.option("width",DEFAULT_WIDTH);},initializeIdAccessor:function(){var id_option=this.option("item_id");if(typeof(id_option)=="string")
accessor=function(item){return item[id_option];};else if(id_option==null)
accessor=function(item){return""};else
accessor=id_option;this.getItemId=accessor;},initializeGroupAccessor:function(){var group_option=this.option("group");if(typeof(group_option)=="string")
accessor=function(item){return item[group_option];};else if(group_option==null)
accessor=function(item){return item}
else
accessor=group_option;this.getGroup=accessor;},initializeLabelAccessor:function(){var label_option=this.option("label");if(typeof(label_option)=="string")
accessor=function(record){return record[label_option];};else if(label_option==null)
accessor=function(record){return record}
else
accessor=label_option;this.getLabel=accessor;},setModel:function(model){var getGroup=this.getGroup;var getLabel=this.getLabel;model=new ListFilter(model);model.orderBy(function(item_a,item_b){var group_a=getGroup(item_a).toLowerCase();var group_b=getGroup(item_b).toLowerCase();if(group_a<group_b)
return-1;if(group_a>group_b)
return+1;var label_a=getLabel(item_a).toLowerCase();var label_b=getLabel(item_b).toLowerCase();if(label_a<label_b)
return-1;if(label_a>label_b)
return+1;return 0;});this.model=model;this.buildGroupIndex();},buildGroupIndex:function(){var group_index=new Object();var group_names=new Array();var group=null;var getGroup=this.getGroup;this.model.each(function(item,index){var item_group=getGroup(item);if(item_group!=group){group=item_group;group_names.push(group);group_index[group]=index;}});this.group_names=new ListModel(group_names);this.group_index=group_index;},generate:function(dom_element){var width=this.width;var selector=new GroupSelector({model:this.group_names,width:width});selector.addListener("select",this,"selectGroup");selector.makeControl(dom_element);this.selector=selector;var item_list=new ItemList({model:this.model,width:width,getItemId:this.getItemId,getGroup:this.getGroup,getLabel:this.getLabel});item_list.makeControl(dom_element);item_list.addListener("scrolled",this);item_list.addListener("checked",this);item_list.addListener("unchecked",this);this.item_list=item_list;},selectGroup:function(group){this.item_list.scroll(group);},scrolled:function(group){this.selector.setSelected(group);},checked:function(item){this.broadcast("checked",item);},unchecked:function(item){this.broadcast("unchecked",item);},uncheck:function(item){item.selected=false;var item_list=this.item_list;this.model.each(function(source_item,index){if(item===source_item){item_list.updateRow(index);}});}});var GroupSelector=Control.extend({class_name:"GroupSelector",constructor:function(options){this.base(options,{css_class:'wa_clist_select'});this.width=this.option("width");var model=this.option("model");model.addListener("changed",this,"update");this.model=model;this.selected=null;},generate:function(dom_element){},update:function(){var self=this;var dom_element=this.dom_element;dom_element.empty();this.model.each(function(item,index){var button_id=self.id+"_"+index;dom_element.append(div({id:button_id,'class':'wa_clist_group_buton'},item));jQuery("#"+button_id,dom_element).click(function(){self.select(item);});});},select:function(item){if(this.selected==item)return;this.broadcast("select",item);},setSelected:function(item){var button_id;var prior_selected=this.selected;if(prior_selected==item)return;var dom_element=this.dom_element;var model=this.model;if(typeof prior_selected!="undefined"){button_id="#"+this.id+"_"+
model.indexOf(prior_selected);jQuery(button_id,dom_element).removeClass('wa_clist_group_selected');}
this.selected=item;button_id="#"+this.id+"_"+model.indexOf(item);jQuery(button_id,this.dom_element).addClass('wa_clist_group_selected');}});var ItemList=Control.extend({class_name:"ItemList",constructor:function(options){this.base(options,{css_class:'wa_clist_items'});this.model=this.option("model");this.width=this.option("width");this.getLabel=this.option("getLabel");this.getGroup=this.option("getGroup");this.getItemId=this.option("getItemId");this.group_list=new Array();this.group_map=new Object();},generate:function(dom_element){var self=this;dom_element.scroll(function(){self.scrolled();});var element_top=dom_element.offset().top;this.element_top=element_top;var table_width=this.width-TABLE_BORDER;dom_element.append(table({style:'width:'+table_width+'px;'}));var table_element=jQuery("table",dom_element);this.cell_width=this.width-16-15;var group=null;var item_count=0;var group_count=0;var getGroup=this.getGroup;var getLabel=this.getLabel;this.model.each(function(item,index){var item_group=getGroup(item);if(item_group!=group){group=item_group;self.generateGroupHeader(group,group_count,table_element);group_count+=1;}
self.generateItemRow(item,index,table_element);});},generateItemRow:function(item,index,table_element){var getLabel=this.getLabel;var getItemId=this.getItemId;var row_id=this.id+"r"+index;table_element.append(tr({id:row_id},td({'class':'wa_clist_checkcol'},div({'class':'wa_clist_checkbox'})),td({'class':'wa_clist_id'},getItemId(item)),td({'class':'wa_clist_label'},getLabel(item))));var self=this;var row_element=jQuery("#"+row_id+" td",this.dom_element);row_element.click(function(){self.toggleRow(item,row_id);});this.updateRow(index);},updateRow:function(index){var item=this.model.get(index);var row_id=this.id+"r"+index;var element=jQuery("#"+row_id+" .wa_clist_checkbox",this.dom_element);var is_selected=item.selected;if(typeof(is_selected)=="undefined")
is_selected=false;if(is_selected)
element.addClass('wa_clist_checkbox_selected');else
element.removeClass('wa_clist_checkbox_selected');},generateGroupHeader:function(group,count,table_element){group_id=this.id+'g'+count;table_element.append(tr({id:group_id,'class':'wa_clist_group_row'},td({style:'width:'+CHECKBOX_WIDTH+'px'}),td({colspan:2},group)));var group_element=jQuery("#"+group_id,table_element);var group_object={group:group,element:group_element};this.group_list.push(group_object);this.group_map[group]=group_object;},toggleRow:function(item,row_id){var element=jQuery("#"+row_id+" .wa_clist_checkbox",this.dom_element);if(typeof item.selected=="undefined")
item.selected=false;item.selected=!item.selected;if(item.selected){element.addClass('wa_clist_checkbox_selected');this.broadcast("checked",item);}
else{element.removeClass('wa_clist_checkbox_selected');this.broadcast("unchecked",item);}},scroll:function(group){var group_object=this.group_map[group];if(typeof group_object=="undefined")return;var dom_element=this.dom_element;var top=this.dom_element.offset().top;var element=group_object.element;var offset=element.offset().top-top+dom_element.scrollTop();dom_element.animate({'scrollTop':offset},'fast');},scrolled:function(){var top=this.dom_element.offset().top;var group_list=this.group_list;for(var index=1;index<group_list.length;index+=1){var group_object=group_list[index];var element=group_object.element;var offset=element.offset().top;if(offset>top){this.broadcast("scrolled",group_list[index-1].group);break;}}}});eval(this.exports);};

// controls/addlist.js
new function(_){var addlist=new base2.Package(this,{name:"addlist",version:"0.1",imports:"observers,list_models,buttons,generator,controls,dialogs,checklist",exports:"AddList"});eval(this.imports);var AddList=Control.extend({constructor:function(options){this.base(options);this.selected=-1;this.initializeIdAccessor();this.initializeGroupAccessor();this.initializeLabelAccessor();this.is_sorted=this.option("sorted",true);var items=this.option("items",new ListModel());items.addListener("added",this,"onAdd");items.addListener("removed",this,"onRemove");this.items=items;var source=this.option("source",new ListModel());this.source=source;},initializeGroupAccessor:function(){var group_option=this.option("group");if(typeof(group_option)=="string")
accessor=function(item){return item[group_option];};else if(group_option==null)
accessor=function(item){return item}
else
accessor=group_option;this.getGroup=accessor;},initializeLabelAccessor:function(){var label_option=this.option("label");if(typeof(label_option)=="string")
accessor=function(item){return item[label_option];};else if(label_option==null)
accessor=function(item){return item};else
accessor=label_option;this.getLabel=accessor;},initializeIdAccessor:function(){var id_option=this.option("item_id");if(typeof(id_option)=="string")
accessor=function(item){return item[id_option];};else if(id_option==null)
accessor=function(item){return item};else
accessor=id_option;this.getItemId=accessor;},generate:function(dom_element){var frame_id=this.id+"_frame";dom_element.append(div({id:frame_id,'class':'wa_addlist_list'}));var add_button=new Button({label:"Add...",width:54});add_button.makeControl(dom_element);add_button.addListener("changed",this,"add");var remove_button=new Button({label:"Remove",width:54});remove_button.makeControl(dom_element);remove_button.addListener("changed",this,"remove");this.generateDialog(dom_element);this.generateItemTable(jQuery('#'+frame_id));},generateDialog:function(dom_element){var dialog=new AddDialog({items:this.items,source:this.source,getLabel:this.getLabel,getGroup:this.getGroup,getItemId:this.getItemId});dialog.makeControl(dom_element);this.dialog=dialog;},generateItemTable:function(element){var table_id=this.id+"_table";element.append(table({id:table_id}));this.table_element=jQuery('#'+table_id);},update:function(){var self=this;var items=this.items;if(this.is_sorted){var getItemId=this.getItemId;items.sort(function(item_a,item_b){var id_a=getItemId(item_a);var id_b=getItemId(item_b);if(id_a==id_b)
return 0;else if(id_a<id_b)
return-1;else
return+1;});}
var table_element=this.table_element;table_element.empty();var getLabel=this.getLabel;this.items.each(function(item,index){self.generateItemRow(table_element,item,index);});},generateItemRow:function(table_element,item,index){var item_id=this.getItemId(item);var item_label=this.getLabel(item);table_element.append(tr({},td({'class':'wa_addlist_id'},item_id),td({'class':'wa_addlist_label'},item_label)));var row_element=jQuery('tr:eq('+index+') > td',table_element);var self=this;row_element.click(function(){self.select(index);});},select:function(index){var table_element=this.table_element;var selected=this.selected;if(selected>=0){var prior_element=jQuery('tr:eq('+selected+') > td',table_element);prior_element.removeClass('wa_addlist_selected');}
var row_element=jQuery('tr:eq('+index+') > td',table_element);row_element.addClass('wa_addlist_selected');this.selected=index;},add:function(){this.dialog.show();},onAdd:function(index,value){this.update();this.broadcast("added",value,index);},onRemove:function(index,value){this.update();this.broadcast("removed",value,index);},remove:function(){var selected=this.selected;if(selected<0)return;var item=this.items.get(selected);this.items.remove(selected);this.selected=-1;var getItemId=this.getItemId;var item_id=getItemId(item);var dialog=this.dialog;this.source.each(function(source_item,source_index){if(getItemId(source_item)==item_id){dialog.remove(source_item);}});}});var AddDialog=Dialog.extend({constructor:function(options){this.base(options,{width:216,height:325});this.getLabel=this.option("getLabel");this.getGroup=this.option("getGroup");this.getItemId=this.option("getItemId");var items=this.option('items',new ListModel());this.items=items;var source=this.option('source',new ListModel());this.source=source;this.initializedSelected(items,source);var checklist=new Checklist({model:this.source,item_id:this.getItemId,label:this.getLabel,group:this.getGroup,left:5,top:5});this.checklist=checklist;checklist.addListener("checked",this,"onAdd");checklist.addListener("unchecked",this,"onRemove");this.add(checklist);var close_button=new Button({label:"Ok",width:40,position:'absolute',left:this.width-75,top:this.height-30});this.add(close_button);close_button.addListener("changed",this,"onClose");},initializedSelected:function(items,source){var getItemId=this.getItemId;var id_map={};items.each(function(item){id_map[getItemId(item)]=true;});source.each(function(source_item){if(id_map[getItemId(source_item)])
source_item.selected=true;});},generate:function(dom_element){this.base(dom_element);dom_element.hide();},show:function(){this.base();this.checklist.show();},onAdd:function(check_item){;this.items.add(check_item);},onRemove:function(check_item){var getItemId=this.getItemId;var check_id=getItemId(check_item);var items=this.items;items.each(function(item,index){var item_id=getItemId(item);if(item_id==check_id)
items.remove(index);});},remove:function(item){this.checklist.uncheck(item);},onClose:function(){this.hide();}});eval(this.exports);};

// controls/scroller.js
new function(_){var scroller=new base2.Package(this,{name:"scroller",version:"0.1",imports:"controls,options,generator",exports:"Scroller"});eval(this.imports);var Scroller=Control.extend({class_name:"Scroller",constructor:function(options){this.base(options,{css_class:'wa_scroller'});},generate:function(dom_element){var content_id=this.id+"_content";dom_element.append(div({id:content_id,'class':'wa_scroller_content'}));this.setContentsElement(jQuery("#"+content_id));}});eval(this.exports);};