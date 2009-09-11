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
 * <code>FedoraConnectorAPIX</code> accesses Fedora repository
 * and implements the abstract methods defined in 
 * {@link FedoraConnector} using API-A and API-M. For a detailed
 * explanation of Fedora API-A and API-M please refer to
 * <a href="http://www.fedora-commons.org/documentation/3.2/API-A.html">Fedora Repository 3.2 Documentation: API-A</a> and <a href="http://www.fedora-commons.org/documentation/3.2/API-M.html">Fedora Repository 3.2 Documentation: API-M</a>.
 *
 * @author Xin Xiang
 */
public class FedoraConnectorAPIX extends FedoraConnector {
	/**
	 * Creates a dummy Fedora object with default attributes.
	 * @param pid pid the new object
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
	 * Wrapper of purgeObject in Fedora API-M.
	 *
	 * @param pid pid of the object to be deleted
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
	 * Gets a list of first-level objects in Fedora repository
	 *
	 * @param pattern the pattern of pid
	 * @return a list of pid that satisfy tha pattern
	 */
	public String [] listObjects(String pattern)
	{
		String [] resultFields = new String [] {"pid"};
		FieldSearchQuery query = new FieldSearchQuery();
		List<String> list = new ArrayList<String>();
		String pid;
		int i;

		if (pattern != null) {
			query.setTerms(pattern);
		}
		else {
			query.setTerms(searchPhrase);
		}

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

					list.add(pid);
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
	 * Tests if a given digital object already exists in the Fedora 
	 * repository.
	 * @param pid pid of the object to be tested
	 * @return whether the object exists
	 */
	public boolean existsObject(String pid)
	{
		// returns false so create operation will throw exception

		return false;
	}

	/**
	 * Wrappper of listDatastreams in API-A.
	 *
	 * @param pid pid of the object
	 * @return list of the <code>DataStream</code> objects
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
	 *
	 * @param pid pid of the object
	 * @param dsID id of the datastream
	 * @return byte content of the data stream
	 */
	public byte[] getDataStream(String pid, String dsID)
	{
		MIMETypedStream ds = null;

		try {
			ds = fc.getAPIA().getDatastreamDissemination(pid, dsID, null);
		} catch (Exception e) {
			
		}

		return ds.getStream();
	}

	/**
	 * Tests if a given data stream already exists in the Fedora repository.
	 *
	 * @param pid pid of the object
	 * @param dsID id of the datastream
	 * @return whether the data stream exists
	 */
	public boolean existsDataStream(String pid, String dsID)
	{
		// to be implemented

		return true;
	}

	/**
	 * Modfies the default Dublin Core data stream.
	 *
	 * @param pid pid of the object
	 * @param bytes byte content of the new data stream
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
	 *
	 * @param pid pid of the object
	 * @param dsID id of the data stream
	 * @param mimeType MIME type of the data stream content
	 * @param fileName name of the file storing the data stream content
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
	 * 
	 * @param pid pid of the object
	 * @param dsID id of the data stream
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
