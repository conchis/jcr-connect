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
import java.io.FileWriter;
import java.io.BufferedWriter;

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import fedora.client.FedoraClient;
import fedora.client.HttpInputStream;
import fedora.server.access.FedoraAPIA;
import fedora.server.types.gen.FieldSearchQuery;
import fedora.server.types.gen.FieldSearchResult;
import fedora.server.types.gen.ObjectFields;
import fedora.server.types.gen.MIMETypedStream;
import fedora.server.types.gen.DatastreamDef;

import fedora.server.management.FedoraAPIM;
import fedora.common.Constants;

import org.apache.axis.types.NonNegativeInteger;

import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;


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

	/** fedora base URL */
	private String baseURL;

	/** phrase used to search for all objects */
	private static String searchPhrase;

	private static final String FOXMLPART1 = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?><foxml:digitalObject VERSION=\"1.1\" PID=\"";

	private static final String FOXMLPART2 = 
		"\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"><foxml:objectProperties><foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"Active\"/><foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\"fedoraAdmin\"/></foxml:objectProperties></foxml:digitalObject>";

	private static boolean useREST = true;

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

			if (! props.getProperty("rest").equals("y")) {
				useREST = false;
			}
		}

		baseURL = protocol + "://" + host + ":" + port + "/" + context;

		try {
			fc = new FedoraClient(baseURL, user, pass);
		} catch (Exception e) {

		}
	}

	/**
	 * Escape special characters in http request strings
	 * @param s String
	 */
	private String escapeString(String s)
	{
		return s.replaceAll("/", "%2F");
	}

	/**
	 * HTTP DELETE.
	 * @param url String
	 * @ret status code
	 */
	private int httpDelete(String url)
	{
		DeleteMethod deleteMethod;

		try {
			deleteMethod = new DeleteMethod(url);
			deleteMethod.setDoAuthentication(true);
			deleteMethod.getParams().setParameter("Connection", "Keep-Alive");
			fc.getHttpClient().executeMethod(deleteMethod);

			return deleteMethod.getStatusCode();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to delete!");
			
			return -1;
		}
	}

	/**
	 * Create a dummy Fedora object with default attributes
	 */
	public void createObjectREST(String pid)
	{
		String url;
		PostMethod postMethod;

		url = baseURL + "/objects/" + pid;
		try {
			System.out.println("ingesting " + pid);

			postMethod = new PostMethod(url);
			postMethod.setDoAuthentication(true);
			postMethod.getParams().setParameter("Connection", "Keep-Alive");
			postMethod.setContentChunked(true);
			fc.getHttpClient().executeMethod(postMethod);

			if (postMethod.getStatusCode() != SC_CREATED) {
				System.err.println("status code: " + 
								   postMethod.getStatusCode());
				System.err.println("failed to insert digital object!");
			};
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to insert object!");
		}
	}

	/**
	 * Deletes a digital object.
	 * Wrapper of purgeObject in Fedora REST.
	 */
	public void deleteObjectREST(String pid)
	{
		String url;
		int statusCode;

		url = baseURL + "/objects/" + pid;
		statusCode = httpDelete(url);
		if (statusCode != SC_OK) {
			System.err.println("status code: " + 
							   statusCode);
			// System.err.println("failed to delete digital object!");
		};
	}

	/**
	 * Wrapper of findObjects in REST
	 * Get a list of objects in Fedora repository
	 *
	 */
	public String [] listObjectsREST()
	{
		String response = "";
		Pattern pattern;
		Matcher matcher;
		String line;
		String pid;
		List<String> list = new ArrayList<String>();

		try {
			response = fc.getResponseAsString("/objects?query=pid%7E*&maxResults=1024&resultFormat=xml&pid=true",
											  true, false);
		} catch (Exception e) {
			return null;
		}

		pattern = Pattern.compile("<pid>[^<]+</pid>");
		matcher = pattern.matcher(response);
		
		while (matcher.find()) {
			line = matcher.group();
			pid = line.substring(5, line.length() - 6);
			list.add(pid);
		}

		return  (String []) list.toArray(new String[0]);
	}

	/**
	 * Wrappper of listDatastreams in REST.
	 */
	public DataStream [] listDataStreamsREST(String pid)
	{
		DataStream dataStream;
		int i;
		String response = "";
		Pattern pattern, attributePattern;
		Matcher matcher, attributeMatcher;
		String line;
		String s = "";
		String dsid, label, mimeType;
		List<DataStream> list = new ArrayList<DataStream>();

		try {
			response = 
				fc.getResponseAsString(
									   String.format("/objects/%s/datastreams.xml", pid), true, false);
		} catch (Exception e) {
			return null;
		}

		pattern = Pattern.compile("<datastream [^>]+/>");
		matcher = pattern.matcher(response);
		
		while (matcher.find()) {
			// iterate over all the datastream elements
			line = matcher.group();

			// get dsid, label and mime type respectively
			attributePattern = Pattern.compile("dsid=\"[^\"]+\"");
			attributeMatcher = attributePattern.matcher(line);
			if (attributeMatcher.find()) {
				s = attributeMatcher.group();
			}

			dsid = s.substring(6, s.length() - 1);

			attributePattern = Pattern.compile("label=\"[^\"]*\"");
			attributeMatcher = attributePattern.matcher(line);
			if (attributeMatcher.find()) {
				s = attributeMatcher.group();
			}
			label = s.substring(7, s.length() - 1);

			attributePattern = Pattern.compile("mimeType=\"[^\"]*\"");
			attributeMatcher = attributePattern.matcher(line);
			if (attributeMatcher.find()) {
				s = attributeMatcher.group();
			}
			mimeType = s.substring(10, s.length() - 1);

			if (mimeType == null || mimeType.equals("")) {
				// set default MIME type
				mimeType = "application/octet-stream";
			}

			// add the data stream object
			dataStream = new DataStream(dsid, label, mimeType);
			list.add(dataStream);

			System.out.println(dsid + ", " + label + ", " + mimeType);
		}

		return list.toArray(new DataStream[0]);
	}

	/**
	 * Wrapper of getDatastreamDissemination in REST.
	 */
	byte[] getDataStreamREST(String pid, String dsID)
	{
		HttpInputStream inputStream;
		ReadableByteChannel channel;
		ByteBuffer buf;
		byte [] bytes;
		int numRead = 0;
		int length = 0;
        
		try {
			inputStream = fc.get(String.format("/objects/%s/datastreams/%s/content", pid, dsID), true, false);
		} catch (Exception e) {
			return null;
		}

		channel = Channels.newChannel(inputStream);
		// Create a direct ByteBuffer
        buf = ByteBuffer.allocateDirect(10 * 1024 * 1024);

		while (numRead >= 0) {
            // Read bytes from the channel
			try {
				numRead = channel.read(buf);
			} catch (Exception e) {
				return null;
			}

			if (numRead > 0) {
				length += numRead;
			}
        }	

		bytes = new byte[length];
		// reset the position of the buffer to zero
		buf.rewind();
		buf.get(bytes);

		return bytes;
	}

	/**
	 * Test if a given digital object alrady exists in the Fedora repository.
	 */
	public boolean existsObjectREST(String pid)
	{
		String response = "";
		Pattern pattern;
		Matcher matcher;

		try {
			response = fc.getResponseAsString(String.format("/objects?query=pid%%7E%s&resultFormat=xml&pid=true", pid),
											  true, false);
		} catch (Exception e) {
			return false;
		}

		pattern = Pattern.compile("<pid>[^<]+</pid>");
		matcher = pattern.matcher(response);
		
		return matcher.find();
	}


	/**
	 * Test if a given data stream alrady exists in the Fedora repository.
	 */
	public boolean existsDataStreamREST(String pid, String dsID)
	{
		String response = "";
		Pattern pattern, attributePattern;
		Matcher matcher, attributeMatcher;
		String line;
		String s = "";
		String id;

		try {
			response = 
				fc.getResponseAsString(
									   String.format("/objects/%s/datastreams.xml", pid), true, false);
		} catch (Exception e) {
			return false;
		}

		pattern = Pattern.compile("<datastream [^>]+/>");
		matcher = pattern.matcher(response);
		
		while (matcher.find()) {
			// iterate over all the datastream elements
			line = matcher.group();

			// get dsid, label and mime type respectively
			attributePattern = Pattern.compile("dsid=\"[^\"]+\"");
			attributeMatcher = attributePattern.matcher(line);
			if (attributeMatcher.find()) {
				s = attributeMatcher.group();
			}

			id = s.substring(6, s.length() - 1);

			if (id.equals(dsID)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds a data stream.
	 * Wrapper of addDatastream in Fedora REST
	 */
	public void addDataStreamREST(String pid, String dsID, 
							  String mimeType, String fileName)
	{
		String url;
		PostMethod postMethod;

		try {
			String dsLocation = fc.uploadFile(new File(fileName));
			System.out.println("filed uploaded at " + dsLocation);

			if (mimeType == null) {
				// set default MIME type
				mimeType = "application/octet-stream";
			}

			url = baseURL + "/objects/" + pid + "/datastreams/" + dsID +
				"?controlGroup=M&dsLabel=" + dsID + 
				"&mimeType=" + escapeString(mimeType) + 
				"&dsLocation=" + escapeString(dsLocation);

			postMethod = new PostMethod(url);
			postMethod.setDoAuthentication(true);
			postMethod.getParams().setParameter("Connection", "Keep-Alive");
			postMethod.setContentChunked(true);
			fc.getHttpClient().executeMethod(postMethod);

			if (postMethod.getStatusCode() != SC_CREATED) {
				System.err.println("status code: " + 
								   postMethod.getStatusCode());
				System.err.println("failed to add data stream!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to add data stream!");
		}
	}

	/**
	 * Deletes a data stream.
	 * Wrapper of purgeDatastream in Fedora REST
	 */
	public void deleteDataStreamREST(String pid, String dsID)
	{
		String url;
		int statusCode;

		url = baseURL + "/objects/" + pid + "/datastreams/" + dsID;
		statusCode = httpDelete(url);
		if (statusCode != SC_OK) {
			System.err.println("status code: " + 
							   statusCode);
			// System.err.println("failed to delete digital object!");
		};
	}

	/**
	 * Create a dummy Fedora object with default attributes
	 */
	public void createObject(String pid)
	{
		String foxml;

		if (useREST) {
			createObjectREST(pid);

			return;
		}

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
		if (useREST) {
			deleteObjectREST(pid);
			
			return;
		}

		try {
			fc.getAPIM().purgeObject(pid, null, false);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to delete digital object!");
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

		if (useREST) {
			return listObjectsREST();
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
	 * Test if a given digital object alrady exists in the Fedora repository.
	 */
	public boolean existsObject(String pid)
	{
		if (useREST) {
			return existsObjectREST(pid);
		}
		
		// to be implemented

		return true;
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

		if (useREST) {
			return listDataStreamsREST(pid);
		}

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

		if (useREST) {
			return getDataStreamREST(pid, dsID);
		}

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
		if (useREST) {
			return existsDataStreamREST(pid, dsID);
		}

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
		if (useREST) {
			addDataStreamREST(pid, dsID, mimeType, fileName);
			
			return;
		}

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
		if (useREST) {
			deleteDataStreamREST(pid, dsID);

			return;
		}

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
