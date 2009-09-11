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
package edu.northwestern.jcr.adapter.fedora.persistence;

import java.util.Map;
import java.util.HashMap;

/**
 * <code>DataStream</code> stores definition of a datastream and
 * maintains a map that associates the UUID of the JCR node 
 * corresponding to the data stream with the <code>DataStream</code> 
 * object.
 *
 * @author Xin Xiang
 */
class DataStream {
	
	/** id of the stream */
	public String id;

	/** label */
	public String label;
	
	/** MIME type */
	public String mimeType;

	/** maps the UUID to the DataStream object */
	private final static Map<String, DataStream> dsMap = 
		new HashMap<String, DataStream>();

	/**
	 * Creates a new <code>DataStream</code> instance.
	 */
	public DataStream(String id, String label, String mimeType) 
	{
		this.id = id;
		this.label = label;
		this.mimeType = mimeType;
	}

	/**
	 * Creates a new <code>DataStream</code> instance.
	 */
	public DataStream(String id)
	{
		this.id = id;
	}

	/**
	 * Sets the label of the <code>DataStream</code> instance.
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}

	/**
	 * Sets the MIME type of the <code>DataStream</code> instance.
	 */
	public void setMIMEType(String mimeType)
	{
		this.mimeType = mimeType;
	}

	/**
	 * Associates the <code>DataStream</code> object with the UUID of JCR node.
	 */
	public void setUUID(String uuid)
	{
		if (dsMap.get(uuid) == null) {
			dsMap.put(uuid, this);
		}
	}

	/**
	 * Retrieves the <code>DataStream</code> object from the UUID of JCR node.
	 */
	public static DataStream getDSFromUUID(String uuid)
	{
		return dsMap.get(uuid);
	}
}
