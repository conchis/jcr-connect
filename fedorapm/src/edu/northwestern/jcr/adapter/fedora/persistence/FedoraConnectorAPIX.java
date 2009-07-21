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

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import fedora.server.access.FedoraAPIA;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.ObjectFields;
import fedora.server.types.gen.MIMETypedStream;
import fedora.server.types.gen.DatastreamDef;

import fedora.server.management.FedoraAPIM;
import fedora.common.Constants;

import org.apache.axis.types.NonNegativeInteger;

/**
 * <code>FedoraConnector</code> accesses Fedora repository
 * using Fedora API-A,API-M.
 *
 * It is not called FedoraClient to distinguish with the class in Fedora API.
 *
 * @author Xin Xiang
 */
public class FedoraConnectorAPIX extends FedoraConnector {
	/**
	 * Create a dummy Fedora object with default attributes
	 */
	public void createObject(String pid)
	{
		String foxml;

		foxml = FOXMLPART1 + pid + FOXMLPART2;

		try {
			System.out.println("ingesting " + pid);

			// make the SOAP call on API-M using the connection stub
			pid = fc.getAPIM().ingest(foxml.getBytes(), 
									  Constants.FOXML1_1.uri, null);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to insert object!");
		}
	}

	/**
	 * Deletes a digital object.
	 * Wrapper of purgeObject in Fedora API-M
	 */
	public void deleteObject(String pid)
	{
		try {
			fc.getAPIM().purgeObject(pid, null, false);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to delete digital object!");
		}
	}

	/**
	 * Wrapper of findObjects in Fedora API-A.
	 * Get a list of first-level objects in Fedora repository
	 *
	 */
	public String [] listObjects()
	{
		String [] resultFields = new String [] {"pid"};
		FieldSearchQuery query = new FieldSearchQuery();
		List<String> list = new ArrayList<String>();
		String pid;
		String [] children;
		Map<String, Integer> map = new HashMap<String, Integer>();
		int i;

		children = listMembers(null);
		i = 0;
		for (String child : children) {
			map.put(child, i);
			i++;
		}

		query.setTerms(searchPhrase);
		
		try {
			FieldSearchResult result =
				fc.getAPIA().findObjects(resultFields, 
										 new NonNegativeInteger(""
																+ 128),
										 query);
			int matchNum = 0;
			while (result != null) {
				for (i = 0; i < result.getResultList().length; i++) {
					ObjectFields o = result.getResultList()[i];
					pid = o.getPid();
					matchNum++;

					if (map.get(pid) == null) {
						// not a member of anything
						list.add(pid);
					}
				}

				if (result.getListSession() != null &&
					result.getListSession().getToken() != null) {
					result = fc.getAPIA().resumeFindObjects(result.getListSession().getToken());
				}
				else {
					result = null;
				}
			}
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getClass().getName()
							   + 
							   (e.getMessage() == null ? "" : 
								": " + e.getMessage()));
		}

		return (String []) list.toArray(new String[0]);
	}

	/**
	 * Test if a given digital object alrady exists in the Fedora repository.
	 */
	public boolean existsObject(String pid)
	{
		// returns false so create operation will throw exception

		return false;
	}

	/**
	 * Wrappper of listDatastreams in API-A.
	 */
	public DataStream [] listDataStreams(String pid)
	{
		DatastreamDef [] dsDef = null;
		DataStream [] dsList;
		String mimeType;
		int i;

		try {
			dsDef = fc.getAPIA().listDatastreams(pid, null);
		} catch (Exception e) {
			System.err.println("pid: " + pid);
			System.err.println("ERROR: " + e.getClass().getName()
							   + 
							   (e.getMessage() == null ? "" : 
								": " + e.getMessage()));
			return null;
		}

		dsList = new DataStream[dsDef.length];

		for (i = 0; i < dsDef.length; ++i) {
			mimeType = dsDef[i].getMIMEType();
			if (mimeType == null || mimeType.equals("")) {
				// set default MIME type
				mimeType = "application/octet-stream";
			}

			dsList[i] = new DataStream(dsDef[i].getID(),
									   dsDef[i].getLabel(),
									   mimeType);
		}

		return dsList;
	}


	/**
	 * Wrapper of getDatastreamDissemination in API-A.
	 */
	byte[] getDataStream(String pid, String dsID)
	{
		MIMETypedStream ds = null;

		try {
			ds = fc.getAPIA().getDatastreamDissemination(pid, dsID, null);
		} catch (Exception e) {
			
		}

		return ds.getStream();
	}

	/**
	 * Test if a given data stream alrady exists in the Fedora repository.
	 */
	public boolean existsDataStream(String pid, String dsID)
	{
		// to be implemented

		return true;
	}

	/**
	 * 
	 */
	public void modifyDCDataStream(String pid, byte [] bytes)
	{
		try {
			// make the SOAP call on API-M using the connection stub
			fc.getAPIM().modifyDatastreamByValue(pid,
												 "DC",
												 null,
												 null,
												 null,
												 null,
												 bytes,
												 null,
												 null,
												 null,
												 true);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to update DC stream!");
		}
	}

	/**
	 * Adds a data stream.
	 * Wrapper of addDatastream in Fedora API-M
	 */
	public void addDataStream(String pid, String dsID, 
							  String mimeType, String fileName)
	{
		try {
			String dsLocation = fc.uploadFile(new File(fileName));
			System.out.println("filed uploaded at " + dsLocation);

			if (mimeType == null) {
				// set default MIME type
				mimeType = "application/octet-stream";
			}

			fc.getAPIM().addDatastream(pid,
									   dsID,
									   null,
									   // set label to id by default
									   dsID,
									   false,
									   mimeType, // "image/jpeg",
									   null,
									   dsLocation,
									   "M",
									   "A",
									   null,
									   null,
									   null);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to add data stream!");
		}
	}

	/**
	 * Deletes a data stream.
	 * Wrapper of purgeDatastream in Fedora API-M
	 */
	public void deleteDataStream(String pid, String dsID)
	{
		try {
			fc.getAPIM().purgeDatastream(pid,
									   dsID,
									   null,
									   null,
									   null,
									   false);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to delete data stream!");
		}
	}
}
