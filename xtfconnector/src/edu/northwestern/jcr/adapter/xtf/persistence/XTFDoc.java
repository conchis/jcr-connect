/*
 * Copyright 2009 Northwestern University
 *
 * Licensed under the Educational Community License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package edu.northwestern.jcr.adapter.xtf.persistence;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * <code>XTFDoc</code> stores definition of an XTF document
 * returned as a result of a search request. Specifically,
 * it maintains a map from all the metadata fields to their
 * values. It also saves the location of the underlying
 * file(s) for future retrieval.

 * <p>An XTF document can be either an item or a collection.
 *
 * @author Xin Xiang
 */
public class XTFDoc {
	
	/** id of the doc: path for collection, path + recordNum for item */
	private String id;

	/** path */
	private String path;

	/** record number for item */
	private String recordNum;

	/** maps the name of the metadata field to the list of values */
	private Map<String, List<String>> valueMap;

	/**
	 * Creates a new <code>XTFDoc</code> instance.
	 */
	public XTFDoc(String path, String recordNum) 
	{
		this.path = path;
		this.recordNum = recordNum;

		if (recordNum == null) {
			id = path;
		}
		else {
			id = path + "/" + recordNum;
		}

		valueMap = new HashMap<String, List<String>>();
	}

	/**
	 * Gets the ID of the document.
	 */
	public String getID()
	{
		return id;
	}

	/**
	 * Adds a new name/value pair for metadata.
	 *
	 * @param name name of the field
	 * @param value value of the field
	 */
	public void addProperty(String name, String value)
	{
		if (valueMap.get(name) == null) {
			valueMap.put(name, new ArrayList<String>());
		}

		valueMap.get(name).add(value);
	}

	/**
	 * Returns the value(s) of a property.
	 * @param name name of the field
	 * @return list of values
	 */
	public String [] getProperty(String name)
	{
		return valueMap.get(name).toArray(new String[1]);
	}

	/**
	 * Lists all the propery names of this item.
	 *
	 * @return list of properties
	 */
	public String [] listProperties()
	{
		Set<String> set;
		List<String> list;

		set = valueMap.keySet();
		list = new ArrayList<String>();

		for (String s : set) {
			list.add(s);
		}

		return list.toArray(new String[0]);
	}
}
