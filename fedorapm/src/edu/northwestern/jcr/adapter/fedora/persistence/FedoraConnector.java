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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Properties;

import java.net.URLEncoder;

import fedora.client.FedoraClient;

import fedora.server.management.FedoraAPIM;
import fedora.common.Constants;
import fedora.common.PID;

import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * <code>FedoraConnector</code> accesses Fedora repository
 * using Fedora API-A,API-M/REST.
 *
 * It is not called FedoraClient to distinguish with the class in Fedora API.
 *
 * @author Xin Xiang
 */
public abstract class FedoraConnector {

	/** fedora client handle */
	FedoraClient fc;

	/** fedora base URL */
	String baseURL;

	/** phrase used to search for all objects */
	static String searchPhrase;

	static final String FOXMLPART1 = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?><foxml:digitalObject VERSION=\"1.1\" PID=\"";

	static final String FOXMLPART2 = 
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

		baseURL = protocol + "://" + host + ":" + port + "/" + context;

		try {
			fc = new FedoraClient(baseURL, user, pass);
		} catch (Exception e) {

		}
	}


	/**
	 * Create a dummy Fedora object with default attributes
	 */
	public abstract void createObject(String pid);

	/**
	 * Deletes a digital object.
	 * Wrapper of purgeObject in Fedora API-M
	 */
	public abstract void deleteObject(String pid);

	/**
	 * Get a list of objects in Fedora repository
	 *
	 */
	public abstract String [] listObjects(String pattern);

	/**
	 * Test if a given digital object alrady exists in the Fedora repository.
	 */
	public abstract boolean existsObject(String pid);

	/**
	 * Wrappper of listDatastreams in API-A.
	 */
	public abstract DataStream [] listDataStreams(String pid);

	/**
	 * Wrapper of getDatastreamDissemination in API-A.
	 */
	public abstract byte[] getDataStream(String pid, String dsID);

	/**
	 * Test if a given data stream alrady exists in the Fedora repository.
	 */
	public abstract boolean existsDataStream(String pid, String dsID);

	/**
	 * 
	 */
	// public abstract void modifyDCDataStream(String pid, byte [] bytes);

	/**
	 * Adds a data stream.
	 * Wrapper of addDatastream in Fedora API-M
	 */
	public abstract void addDataStream(String pid, String dsID, 
									   String mimeType, String fileName);

	/**
	 * Deletes a data stream.
	 * Wrapper of purgeDatastream in Fedora API-M
	 */
	public abstract void deleteDataStream(String pid, String dsID);

	private String postMethod(String url) throws Exception
	{
		PostMethod postMethod;

		postMethod = new PostMethod(url);
		postMethod.setDoAuthentication(true);
		postMethod.getParams().setParameter("Connection", "Keep-Alive");
		postMethod.setContentChunked(true);
		fc.getHttpClient().executeMethod(postMethod);
		
		if (postMethod.getStatusCode() != SC_OK) {
			System.err.println("status code: " + 
							   postMethod.getStatusCode());
		}

		return postMethod.getResponseBodyAsString();
	}

	/**
	 * Get a list of all descendants of a given object in Fedora 
	 * repository through resource index.
	 * The result is in CSV format as if it is generated directly
	 * from resouce index.
	 */
	public String [] listDescendantsRI(String pid)
	{
		String [] members;
		Map<String, String> pathMap;
		Queue<String> queue;
		List<String> resultList;
		String nextPID;
		String parentPath;

		pathMap = new HashMap<String, String>();
		queue = new LinkedList<String>();
		resultList = new ArrayList<String>();

		if (pid == null) {
			members = listObjectsRI();
		}
		else {
			// to be implemented
			members = listMembers(pid);
		}

		for (String member : members) {
			queue.add(member);
			pathMap.put(member, member);
			resultList.add(member);
		}

		while (! queue.isEmpty()) {
			nextPID = queue.remove();
			parentPath = pathMap.get(nextPID);

			members = listMembers(nextPID);

			for (String member : members) {
				queue.add(member);
				pathMap.put(member, parentPath + "," + member);
				resultList.add(parentPath + "," + member);
			}
		}

		return (String []) resultList.toArray(new String [0]);
	}


	/**
	 * Get a list of first-level objects in Fedora repository through
	 * resource index.
	 */
	public String [] listObjectsRI()
	{
		String response = "";
		String query;
		String line;
		int i;
		String [] children;
		Map<String, Integer> map = new HashMap<String, Integer>();
		List<String> list = new ArrayList<String>();
		String url;

		children = listMembers(null);
		i = 0;
		for (String child : children) {
			map.put(child, i);
			i++;
		}

		query = "select $t from <#ri> where $s <http://purl.org/dc/elements/1.1/identifier>$t";

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=itql&format=CSV&query=" + URLEncoder.encode(query, "UTF-8");
			response = postMethod(url);

			BufferedReader reader =
				new BufferedReader(new StringReader(response));
			// skip header
			line = reader.readLine();

			list = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				if (map.get(line) == null) {
					// not a member of anything
					list.add(line);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to list objects!");
		}

		return  (String []) list.toArray(new String[0]);
	}

	/**
	 * Get the path consisting of PIDs of the objects along the path.
	 */
	public String [] getPath(String [] pids)
	{
		Map<String, String> parentMap;
		String parentPID;
		String [] result;
		int i;

		parentMap = new HashMap<String, String>();
		result = new String [pids.length];
		i = 0;

		for (String pid : pids) {
			result[i] = "";
					
			do {
				parentPID = parentMap.get(pid);
				if (parentPID == null) {
					parentPID = getParent(pid);
					if (parentPID == null) {
						parentPID = "root";
					}

					parentMap.put(pid, parentPID);
				}

				if (!result[i].equals("")) {
					result[i] = "," + result[i];
				}
				result[i] = pid + result[i];
				pid = parentPID;
			} while (!parentPID.equals("root"));

			i++;
		}

		return result;
	}

	/**
	 * Get the parent of the object
	 */
	public String getParent(String pid)
	{
		String query;
		String [] result;

		query = "select $s from <#ri> where $a <http://purl.org/dc/elements/1.1/identifier> $s and $b <info:fedora/fedora-system:def/relations-external#isMemberOfCollection> $a and $b <http://purl.org/dc/elements/1.1/identifier> $t and $t <mulgara:is> '" + pid + "'";

		result = searchObjects(query);

		if (result.length < 1) {
			return null;
		}
		else {
			return result[0];
		}
	}

	/**
	 * Lists members of the collection represented by the pid
	 */
	public String [] listMembers(String pid)
	{
		String predicate;
		List<String> list = null;
		String object;
		String query;
		String url;
		String response = "";
		String line;

		if (pid != null) {
			// member of pid
			query = "select $s from <#ri> where $s <info:fedora/fedora-system:def/relations-external#isMemberOfCollection> <" + PID.toURI(pid) + ">;";
		}
		else {
			// member of anything
			query = "select $s from <#ri> where $s <info:fedora/fedora-system:def/relations-external#isMemberOfCollection> $t;";
		}

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=itql&format=CSV&query=" + URLEncoder.encode(query, "UTF-8");

			response = postMethod(url);
			BufferedReader reader =
				new BufferedReader(new StringReader(response));
			// skip header
			line = reader.readLine();

			list = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(Constants.FEDORA.uri)) {
					line = line.substring(Constants.FEDORA.uri.length());
					list.add(line);
					System.out.println(line);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to add data stream!");
		}

		return list.toArray(new String [0]);
	}

	/**
	 * add members of the collection represented by the pid
	 */
	public void addMember(String pid, String cpid)
	{
		String predicate;

		try {
			predicate = "info:fedora/fedora-system:def/relations-external#isMemberOfCollection";
			if (!fc.getAPIM().addRelationship(cpid, predicate, PID.toURI(pid),
											   false, null)) {
				System.out.println("error adding relationship");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to add member!");
		}
	}

	/**
	 * Add a relationship.
	 * Use API-M since there is no other option.
	 */
	public void addProperty(String pid, String uri, String literal)
	{
		try {
			if (!fc.getAPIM().addRelationship(pid, uri, literal,
											  true, 
											  // Constants.RDF_XSD.STRING.uri  
											  null)) {
				System.out.println("error adding relationship");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to add relationship!");
		}
	}

	/**
	 * Get the value of a property.
	 * Use resource index search as oppososed to API-M.
	 */
	public String getProperty(String pid, String uri)
	{
		String query;
		String url;
		String response = "";
		String line = "";

		query = "select $t from <#ri> where <" + PID.toURI(pid) + 
			"> <" + uri + "> $t";

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=itql&format=CSV&query=" + URLEncoder.encode(query, "UTF-8");

			response = postMethod(url);
			BufferedReader reader =
				new BufferedReader(new StringReader(response));
			// skip header
			reader.readLine();

			// one line only
			line = reader.readLine();
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to add data stream!");
		}

		return line;
	}

	/**
	 * Delete a relationship.
	 * Use API-M since there is no other option.
	 */
	public void deleteProperty(String pid, String uri)
	{
		try {
			if (!fc.getAPIM().purgeRelationship(pid, uri, 
												// cannot be null
												// error in API-M doc
												getProperty(pid, uri),
												true, null)) {
				System.out.println("error deleting relationship");
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.err.println("failed to delete relationship!");
		}
	}

	/**
	 * List the name (not value) of the properties.
	 */
	public String [] listProperties(String pid)
	{
		List<String> list = null;
		String query;
		String url;
		String response = "";
		String line;

		query = "select $s from <#ri> where <" + PID.toURI(pid) + "> $s $t";

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=itql&format=CSV&query=" + URLEncoder.encode(query, "UTF-8");

			response = postMethod(url);
			BufferedReader reader =
				new BufferedReader(new StringReader(response));
			// skip header
			line = reader.readLine();

			list = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("info:fedora/") ||
					line.startsWith("http://purl.org/dc/elements/1.1/")) {
					// ignore Fedora and DC predicates
					continue;
				}

				list.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to add data stream!");
		}

		return list.toArray(new String [0]);
	}

	/**
	 * Test if the property exists.
	 */
	public boolean existsProperty(String pid, String predicate)
	{
		String query;
		String url;
		String response = "";
		String line;

		query = "select $s from <#ri> where <" + PID.toURI(pid) + "> <" +
			predicate + "> $s";

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=itql&format=CSV&query=" + URLEncoder.encode(query, "UTF-8");

			response = postMethod(url);
			BufferedReader reader =
				new BufferedReader(new StringReader(response));
			// skip header
			line = reader.readLine();

			if (reader.readLine() != null) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to add data stream!");
		}

		return false;
	}

	/**
	 * Run the iTQL expression against the resource index and
	 * return objects found.
	 */
	public String [] searchObjects(String query)
	{
		List<String> list = null;
		String url;
		String response = "";
		String line;

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=itql&format=CSV&query=" + URLEncoder.encode(query, "UTF-8");

			response = postMethod(url);
			BufferedReader reader =
				new BufferedReader(new StringReader(response));
			// skip header
			line = reader.readLine();

			list = new ArrayList<String>();
			while ((line = reader.readLine()) != null) {
				list.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("failed to search resource index!");
		}

		return list.toArray(new String [0]);
	}
}
