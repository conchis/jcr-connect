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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.net.URLEncoder;
import java.net.MalformedURLException;

import javax.jcr.RepositoryException;

import fedora.client.FedoraClient;

import fedora.server.management.FedoraAPIM;
import fedora.common.Constants;
import fedora.common.PID;

import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import org.apache.commons.httpclient.methods.PostMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p><code>FedoraConnector</code> accesses Fedora repository
 * using resource index or Fedora API-M.<p>
 *
 * <p>It is designed as the abstract class for Fedora access.
 * {@link FedoraConnectorAPIX} and {@link FedoraConnectorREST}
 * inherit this class and implement the abstract methods defined
 * here in either API-A/API-M or REST.</p>
 *
 * <p>Some methods are implemented here since they use resource index
 * directly or the operations are only implemented in API-M, for example,
 * adding a relationship bewteen Fedora objects.</p> For a detailed
 * explanation of Fedora API-A, API-M and resource index search please 
 * refer to <a href="http://www.fedora-commons.org/documentation/3.2/API-A.html">Fedora Repository 3.2 Documentation: API-A</a>, <a href="http://www.fedora-commons.org/documentation/3.2/API-M.html">Fedora Repository 3.2 Documentation: API-M</a>, and <a href="http://www.fedora-commons.org/documentation/3.2/Resource%20Index%20Search.html">Fedora Repository 3.2 Documentation: Resource Index Search</a>. Make sure the resource index of the Fedora repository is enabled in order
 * for this connector to function correctly.
 *
 * <p>It is not called <code>FedoraClient</code> to be distinguished with 
 * the class in Fedora API.</p>
 *
 * @author Xin Xiang
 */
public abstract class FedoraConnector {

	/** log4j logger. */
	private static Logger log = 
		LoggerFactory.getLogger(FedoraConnector.class);
	
	/** fedora client handle. */
	FedoraClient fc;

	/** fedora base URL. */
	String baseURL;

	/** gsearch URL. */
	String gsearchURL;

	/** phrase used to search for all objects. */
	static String searchPhrase;

	/** fields for full-text search against gSearch */
	String [] gsearchFields;

	/** first part of the FOXML template. */
	static final String FOXMLPART1 = 
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?><foxml:digitalObject VERSION=\"1.1\" PID=\"";

	/** second part of the FOXML template. */
	static final String FOXMLPART2 = 
		"\" xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"><foxml:objectProperties><foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"Active\"/><foxml:property NAME=\"info:fedora/fedora-system:def/model#ownerId\" VALUE=\"fedoraAdmin\"/></foxml:objectProperties></foxml:digitalObject>";

	/**
	 * Creates a new <code>FedoraConnector</code> instance. If the
	 * property file fedora.properties is present, the property values in 
	 * that file are used. Otherwise the default values are used.
	 */
	public FedoraConnector() // throws Exception
	{
		String protocol = "";
		String host = "";
		String port = "";
		String context = "";
		String gsearchContext = "";
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

		gsearchContext = "fedoragsearch";
		gsearchFields = new String [] {"rss"};

		if (property) {
			protocol = props.getProperty("protocol");
			host = props.getProperty("host");
			port = props.getProperty("port");
			context = props.getProperty("context");
			user = props.getProperty("user");
			pass = props.getProperty("password");
			searchPhrase = props.getProperty("phrase");

			String s = props.getProperty("gsearchcontext");

			if (s != null && ! s.equals("")) {
				gsearchContext = s;
			}

			s = props.getProperty("gsearchfields");
			if (s != null && ! s.equals("")) {
				gsearchFields = s.split(",");
			}			
		}

		baseURL = protocol + "://" + host + ":" + port + "/" + context;
		gsearchURL = protocol + "://" + host + ":" + port + "/" + gsearchContext;

		try {
			fc = new FedoraClient(baseURL, user, pass);
		} catch (MalformedURLException e) {
			String msg = "error connecting to the Fedora server";
            log.error(msg);
            // throw new RepositoryException(msg, e);
		}
	}


	/**
	 * Creates a dummy Fedora object with default attributes.
	 *
	 * @param pid pid the new object
	 */
	public abstract void createObject(String pid) throws Exception;

	/**
	 * Deletes a digital object.
	 *
	 * @param pid pid of the object to be deleted
	 */
	public abstract void deleteObject(String pid);

	/**
	 * Gets a list of objects in Fedora repository.
	 *
	 * @param pattern the pattern of pid
	 * @return a list of pid that satisfy tha pattern
	 */
	public abstract String [] listObjects(String pattern) throws Exception;

	/**
	 * Tests if a given digital object already exists in the Fedora 
	 * repository.
	 *
	 * @param pid pid of the object to be tested
	 * @return whether the object exists
	 */
	public abstract boolean existsObject(String pid);

	/**
	 * Lists the data streams of a Fedora object.
	 *
	 * @param pid pid of the object
	 * @return list of the <code>DataStream</code> objects
	 */
	public abstract DataStream [] listDataStreams(String pid);

	/**
	 * Returns the data stream content.
	 *
	 * @param pid pid of the object
	 * @param dsID id of the datastream
	 * @return byte content of the data stream
	 */
	public abstract byte[] getDataStream(String pid, String dsID);

	/**
	 * Tests if a given data stream alrady exists in the Fedora repository.
	 *
	 * @param pid pid of the object
	 * @param dsID id of the datastream
	 * @return whether the data stream exists
	 */
	public abstract boolean existsDataStream(String pid, String dsID);

	/**
	 * 
	 */
	public abstract void modifyDCDataStream(String pid, byte [] bytes);

	/**
	 * Adds a data stream.
	 *
	 * @param pid pid of the object
	 * @param dsID id of the data stream
	 * @param mimeType MIME type of the data stream content
	 * @param fileName name of the file storing the data stream content
	 */
	public abstract void addDataStream(String pid, String dsID, 
									   String mimeType, String fileName);

	/**
	 * Deletes a data stream.
	 * 
	 * @param pid pid of the object
	 * @param dsID id of the data stream
	 */
	public abstract void deleteDataStream(String pid, String dsID);

	/**
	 * Sends an HTTP POST request and returns the response body as
	 * a string.
	 *
	 * @param url URL of the resource
	 * @return the response body as <code>String</code>
	 */
	private String postMethod(String url) throws Exception
	{
		PostMethod postMethod;

		postMethod = new PostMethod(url);
		postMethod.setDoAuthentication(true);
		postMethod.getParams().setParameter("Connection", "Keep-Alive");
		postMethod.setContentChunked(true);
		try {
			fc.getHttpClient().executeMethod(postMethod);
		} catch (Exception e) {
			String msg = "error connecting to the Fedora server";
            log.error(msg);
            throw new RepositoryException(msg, null);
		}

		if (postMethod.getStatusCode() != SC_OK) {
			log.warn("status code: " + postMethod.getStatusCode());
		}

		return postMethod.getResponseBodyAsString();
	}

	/**
	 * Gets a list of all descendants of a given object in Fedora 
	 * repository through resource index, applying the filter
	 * if available.
	 * The result is in CSV format as if it is generated directly
	 * from resouce index.
	 *
	 * @param pid pid of the object
	 * @param filter filter condition applied - null if there is no filter
	 * @return list of pid of the descendants that satisfy the filter condition
	 */
	public String [] listDescendantsRI(String pid, String filter) throws Exception
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
			try {
				members = listObjectsRI(null);
			} catch (Exception e) {
				throw e;
			}			
		}
		else {
			// to be implemented
			members = listMembers(pid, null);
		}

		for (String member : members) {
			queue.add(member);
			pathMap.put(member, member);
		}

		if (filter != null) {
			if (pid == null) {
				try {
					members = listObjectsRI(filter);
				} catch (Exception e) {
					throw e;
				}
			}
			else {
				// to be implemented
				members = listMembers(pid, filter);
			}
		}

		// add only those satisfying the filter to the result list
		for (String member : members) {
			resultList.add(member);
		}

		while (! queue.isEmpty()) {
			nextPID = queue.remove();
			parentPath = pathMap.get(nextPID);

			members = listMembers(nextPID, null);

			for (String member : members) {
				queue.add(member);
				pathMap.put(member, parentPath + "," + member);
			}

			if (filter != null) {
				members = listMembers(nextPID, filter);
			}

			// add only those satisfying the filter to the result list			
			for (String member : members) {			  
				resultList.add(parentPath + "," + member);
			}
		}
		
		return (String []) resultList.toArray(new String [0]);
	}

	/**
	 * Gets a list of first-level objects (objects that are not a member of 
	 * some other object) in Fedora repository through resource index.
	 *
	 * @param filter filter condition applied - null if there is no filter
	 */
	public String [] listObjectsRI(String filter) throws Exception
	{
		String response = "";
		String query;
		String line;
		int i;
		List<String> list = new ArrayList<String>();
		String url;

		query = "SELECT $id from <#ri> {$s <http://purl.org/dc/elements/1.1/identifier> $id OPTIONAL { $s <info:fedora/fedora-system:def/relations-external#isMemberOfCollection> $parent } . FILTER (!bound($parent))";

		if (filter != null) {
			query += " " + filter;
		}

		query += "}";

		log.info(query);

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=sparql&format=CSV&query=" + URLEncoder.encode(query, "UTF-8");
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
			// e.printMessage();
			// System.err.println(e.getMessage());
			log.error("failed to list objects!", e);

			throw e;
		}

		return  (String []) list.toArray(new String[0]);
	}

	/**
	 * Filters the object list based on the filter.
	 *
	 * @param list the orignial list of objects
	 * @param filter filter condition applied - null if there is no filter
	 * @return list of the objects that satisfy the condition
	 */
	public String [] filterObjects(String [] list, String filter)
	{
		String response = "";
		String query;
		String line;
		int i;
		List<String> resultList;
		String url;

		query = "select $t from <#ri> { $s <http://purl.org/dc/elements/1.1/identifier> $t . FILTER (";
		
		for (i = 0; i < list.length; ++i) {
			if (i > 0) {
				query += " || ";
			}
			// exact match
			query += "regex($t, '^" + list[i] + "$')";
		}

		query += ")";

		if (filter != null) {
			query += " " + filter;
		}

		query += "}";

		resultList = new ArrayList<String>();

		log.info(query);

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=sparql&format=CSV&query=" + URLEncoder.encode(query, "UTF-8");
			response = postMethod(url);

			BufferedReader reader =
				new BufferedReader(new StringReader(response));
			// skip header
			line = reader.readLine();

			while ((line = reader.readLine()) != null) {
				resultList.add(line);
			}
		} catch (Exception e) {
			 e.printStackTrace();
			 log.error("failed to list objects!", e);
		}
		
		return  (String []) resultList.toArray(new String[0]);
	}

	/**
	 * Gets the comma-separated path consisting of PIDs of the objects 
	 * along the path.
	 *
	 * @param pids list of pid
	 * @return list of comma-separated path elements
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
	 * Gets the parent of the object.
	 *
	 * @param pid pid of the object
	 * @return parent pid
	 */
	public String getParent(String pid)
	{
		String query;
		String [] result;

		query = "select $s from <#ri> where $a <http://purl.org/dc/elements/1.1/identifier> $s and $b <info:fedora/fedora-system:def/relations-external#isMemberOfCollection> $a and $b <http://purl.org/dc/elements/1.1/identifier> $t and $t <mulgara:is> '" + pid + "'";

		result = searchObjects(query, "itql");

		if (result.length < 1) {
			return null;
		}
		else {
			return result[0];
		}
	}

	/**
	 * Lists members of the collection represented by the pid, 
	 * applying the filter if available.
	 *
	 * @param pid pid of the object
	 * @param filter filter condition applied - null if there is no filter
	 * @return list of pid of the members that satisfy the filter condition
	 */
	public String [] listMembers(String pid, String filter)
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
			query = "SELECT $s from <#ri> {$s <info:fedora/fedora-system:def/relations-external#isMemberOfCollection> $parent . $parent <http://purl.org/dc/elements/1.1/identifier> '" + pid + "' .";
		}
		else {
			// member of anything
			query = "select $s from <#ri> {$s <info:fedora/fedora-system:def/relations-external#isMemberOfCollection> $t .";
		}

		if (filter != null) {
			query += " " + filter;
		}

		query += "}";

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=sparql&format=CSV&query=" + URLEncoder.encode(query, "UTF-8");

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
					log.debug(line);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("failed to add data stream!", e);
		}

		return list.toArray(new String [0]);
	}

	/**
	 * Adds a member of the collection represented by the pid.
	 *
	 * @param pid pid of the parent object
	 * @param cpid pid of the child object
	 */
	public void addMember(String pid, String cpid)
	{
		String predicate;

		try {
			predicate = "info:fedora/fedora-system:def/relations-external#isMemberOfCollection";
			if (!fc.getAPIM().addRelationship(cpid, predicate, PID.toURI(pid),
											  false, null)) {
				log.warn("error adding relationship");
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("failed to add member!", e);
		}
	}

	/**
	 * Adds a property and value.
	 * API-M is used since there is no other option.
	 *
	 * @param pid pid of the object
	 * @param uri URI of the predicate
	 * @param literal property value as literal string
	 */
	public void addProperty(String pid, String uri, String literal)
	{
		try {
			if (!fc.getAPIM().addRelationship(pid, uri, literal,
											  true, 
											  // Constants.RDF_XSD.STRING.uri  
											  null)) {
				log.warn("error adding relationship");
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("failed to add relationship!", e);
		}
	}

	/**
	 * Gets the value of a property.
	 * Use resource index search as oppososed to API-M since it is much
	 * faster.
	 *
	 * @param pid pid of the object
	 * @param uri URI of the predicate
	 * @return property value as literal string
	 */
	public String getProperty(String pid, String uri)
	{
		String query;
		String url;
		String response = "";
		String line = "";

		query = "select $t from <#ri> where <" + PID.toURI(pid) + 
			"> <" + uri + "> $t";

		log.info(query);

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
			log.error("failed to add data stream!", e);
		}

		return line;
	}

	/**
	 * Deletes a property.
	 * API-M is used since there is no other option.
	 *
	 * @param pid pid of the object
	 * @param uri URI of the predicate
	 */
	public void deleteProperty(String pid, String uri)
	{
		if (uri.contains("http://purl.org/dc/elements/1.1")) {
			// Dublin Core properties
			return;
		}

		try {
			if (!fc.getAPIM().purgeRelationship(pid, uri, 
												// cannot be null
												// error in API-M doc
												getProperty(pid, uri),
												true, null)) {
				log.warn("error deleting relationship");
			}
		} catch (Exception e) {
			log.error("failed to delete relationship!", e);
		}
	}

	/**
	 * Lists the name (not value) of the properties.
	 *
	 * @param pid pid of the object
	 * @return list of property names
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
				if (line.startsWith("info:fedora/")
					// || line.startsWith("http://purl.org/dc/elements/1.1/")
					) {
					// ignore Fedora and DC predicates
					continue;
				}

				list.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("failed to add data stream!", e);
		}

		return list.toArray(new String [0]);
	}

	/**
	 * Tests if the property exists.
	 *
	 * @param pid pid of the object
	 * @param predicate property name
	 * @return whether the property exists
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
			log.error("failed to add data stream!", e);
		}

		return false;
	}

	/**
	 * Executes the SparQL/iTQL expression against the resource index and
	 * returns objects found.
	 *
	 * @param query the query string
	 * @param language "sparql" or "itql"
	 * @return list of pid of the objects returned by the query
	 */
	public String [] searchObjects(String query, String language)
	{
		List<String> list = null;
		String url;
		String response = "";
		String line;

		try {
			url = baseURL + "/risearch?type=tuples&flush=true&lang=" + 
				language + "&format=CSV&query=" + 
				URLEncoder.encode(query, "UTF-8");

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
			log.error("failed to search resource index!", e);
		}

		return list.toArray(new String [0]);
	}

	/**
	 * Modifies or creates a Dublic Core field/value pair in the DC data stream.
	 *
	 * @param pid pid of the digital object
	 * @param field Dublin Core field to be added or modified
	 * @param value new value of the field
	 */
	public void modifyDCField(String pid, String field, String value)
	{
		String dcXML;
		byte [] b;
		String oldValue, newValue;	
		Pattern pattern;
		Matcher matcher;
		int index;

		if (field.equals("identifier")) {
			// cannot change identifier
			log.error("attemp to change dc:identifier!");
			return;
		}

		b = getDataStream(pid, "DC");
		dcXML = new String(b);

		// DOT matches anything including newline characters
		pattern = Pattern.compile("<dc:" + field + ">.*</dc:" + field + ">", Pattern.DOTALL);
		matcher = pattern.matcher(dcXML);

		newValue = "<dc:" + field + ">" + value + "</dc:" + field + ">";

		if (matcher.find()) {
			// replace current value
			oldValue = matcher.group();
			index = matcher.start();
			dcXML = dcXML.substring(0, index) + newValue + dcXML.substring(index + oldValue.length());
		}
		else {
			// add to the end
			index = dcXML.indexOf("</oai_dc:dc>");
			dcXML = dcXML.substring(0, index) + newValue + "\n</oai_dc:dc>";
		}

		modifyDCDataStream(pid, dcXML.getBytes());
	}

	/**
	 * Runs full-text search agains the gSearch service.
	 *
	 * @param value value of the search expression
	 * @return list of pids
	 */
	public String [] searchFullText(String value)
	{
		String response;
		String url;
		List<String> resultList;
		Pattern pattern;
		Matcher matcher;
		String result = "";
		String pid;

		resultList = new ArrayList<String>();

		// run the search against each field
		for (String field : gsearchFields) {
			url = gsearchURL + "/rest?operation=gfindObjects&query=dsm." + field + "%3A\"" + value + "\"";

			try {
				response = postMethod(url);
			} catch (Exception e) {
				return resultList.toArray(new String[0]);
			}

			result += response;
		}

		// DOT matches anything including newline characters
		pattern = Pattern.compile("<span class=\"hitno\">[^<]+</span><a href=\"[^\"]+\">([^<]+)</a>", Pattern.DOTALL);
		matcher = pattern.matcher(result);

		while (matcher.find()) {
			pid = matcher.group(1);
			if (! resultList.contains(pid)) {
				resultList.add(pid);
			}
		}

		return resultList.toArray(new String[0]);
	}
}
