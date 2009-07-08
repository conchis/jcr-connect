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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import fedora.client.FedoraClient;
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
 * using Fedora API-A,API-M/REST.
 *
 * It is not called FedoraClient to distinguish with the class in Fedora API.
 *
 * @author Xin Xiang
 */
class FedoraConnector {

	/** fedora client handle */
	private FedoraClient fc;

	/** phrase used to search for all objects */
	private static String searchPhrase;

	private static final String FOXMLPART1 = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?><foxml:digitalObject VERSION=\"1.1\" PID=\"";

	private static final String FOXMLPART2 = 
		"\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"><foxml:objectProperties><foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"Active\"/><foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\"fedoraAdmin\"/></foxml:objectProperties></foxml:digitalObject>";

	/**
	 * Creates a new <code>FedoraConnector</code> instance.
	 */
	public FedoraConnector() {
		String protocol = "";
		String host = "";
		String port = "";
		String context = "";
		String user = "";
		String pass = "";
		boolean property = true;

		Properties props = new Properties();
		try {
			props.load(new FileInputStream("fedora.properties"));
		} catch(IOException e) {
			e.printStackTrace();

			// set default
			host = "connectdev.at.northwestern.edu";
			port = "9090";
			user = "fedoraAdmin";
			pass = "123456";
			protocol = "http";
			context = "fedora";
			searchPhrase = "*";

			property = false;
		}

		if (property) {
			protocol = props.getProperty("protocol");
			host = props.getProperty("host");
			port = props.getProperty("port");
			context = props.getProperty("context");
			user = props.getProperty("user");
			pass = props.getProperty("password");
			searchPhrase = props.getProperty("phrase");
		}

		String [] resultFields = new String [] {"pid"};
		// FIXME:  Get around hardcoding the path in the baseURL
		String baseURL = protocol + "://" + host + ":" + port + "/" + context;

		try {
			fc = new FedoraClient(baseURL, user, pass);
		} catch (Exception e) {

		}
	}

	/**
	 * Create a dummy Fedora object with default attributes
	 */
	public void createFedoraObject(String pid)
	{
		String foxml;

		foxml = FOXMLPART1 + pid + FOXMLPART2;

		try {
			// purge the object
			System.out.println("removing " + pid);

			try {
				fc.getAPIM().purgeObject(pid, null, false);
			} catch (Exception e) {
				// ignore if the object exists
				// System.err.println("error removing the object");
				// e.printStackTrace();
			}

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
	 * Wrapper of findObjects in Fedora API-A.
	 * Get a list of objects in Fedora repository
	 *
	 */
	public String [] listObjects()
	{
		String [] resultFields = new String [] {"pid"};
		FieldSearchQuery query = new FieldSearchQuery();
		List<String> list = new ArrayList<String>();
		String pid;

		query.setTerms(searchPhrase);
		
		try {
			FieldSearchResult result =
				fc.getAPIA().findObjects(resultFields, 
										 new NonNegativeInteger(""
																+ 128),
										 query);
			int matchNum = 0;
			while (result != null) {
				for (int i = 0; i < result.getResultList().length; i++) {
					ObjectFields o = result.getResultList()[i];
					pid = o.getPid();
					matchNum++;
					// System.out.println("#" + matchNum);
					// System.out.println(pid);

					list.add(pid);
				}

				result = null;
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
	 * Wrappper of listDatastreams in API-A.
	 */
	public DataStream [] listDataStreams(String pid)
	{
		DatastreamDef [] dsDef = null;
		DataStream [] dsList;
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
			dsList[i] = new DataStream(dsDef[i].getID(),
									   dsDef[i].getLabel(),
									   dsDef[i].getMIMEType());
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

	public void addDataStream(String pid, String dsID, byte [] bytes)
	{
		try {
			File tempFile = File.createTempFile("fedora-upload-", null);
			FileOutputStream fos = new FileOutputStream(tempFile);
			fos.write(bytes);
			fos.flush();
			fos.close();
			String dsLocation = fc.uploadFile(new File("fedora-upload-"));
			System.out.println("filed uploaded at " + dsLocation);

			fc.getAPIM().addDatastream(pid,
									   dsID,
									   null,
									   null,
									   false,
									   null, // "image/jpeg",
									   null,
									   dsLocation,
									   "M",
									   "A",
									   null,
									   null,
									   null);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to update stream!");
		}
	}
}
