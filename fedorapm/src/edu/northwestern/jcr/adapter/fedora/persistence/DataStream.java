/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.northwestern.jcr.adapter.fedora.persistence;

import java.util.Map;
import java.util.HashMap;

/**
 * <code>DataStream</code> stores definition of a datastream
 * and associate a data stream node UUIC with the data stream
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
	 * Associate the data stream object with the UUID of JCR node
	 */
	public void setUUID(String uuid)
	{
		if (dsMap.get(uuid) == null) {
			dsMap.put(uuid, this);
		}
	}

	/**
	 * Retrieve the DataStream object from the UUID of JCR node
	 */
	public static DataStream getDSFromUUID(String uuid)
	{
		return dsMap.get(uuid);
	}
}
