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

import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.net.URLEncoder;

import fedora.client.HttpInputStream;

import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.apache.commons.httpclient.HttpStatus.SC_NO_CONTENT;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>FedoraConnectorREST</code> accesses Fedora repository
 * and implements the abstract methods defined in {@link FedoraConnector}
 * using Fedora REST API. For a detailed explanation of Fedora REST API please 
 * refer to <a href="http://www.fedora-commons.org/documentation/3.2/REST%20API.html">Fedora Repository 3.2 Documentation: REST API</a>
 * 
 * @author Xin Xiang
 */
public class FedoraConnectorREST extends FedoraConnector {

	/** log4j logger. */
	private static Logger log = 
		LoggerFactory.getLogger(FedoraConnectorREST.class);
	
	/**
	 * Sends an HTTP DELETE request and returns the status code.
	 *
	 * @param url URL of the resource
	 * @return status code
	 */
	private int httpDelete(String url)
	{
		DeleteMethod deleteMethod = null;

		try {
			deleteMethod = new DeleteMethod(url);
			deleteMethod.setDoAuthentication(true);
			deleteMethod.getParams().setParameter("Connection", "Keep-Alive");
			fc.getHttpClient().executeMethod(deleteMethod);

			return deleteMethod.getStatusCode();
		} catch (Exception e) {
			e.printStackTrace();
			log.warn("failed to delete!");
			
			return -1;
		} finally {
			if (deleteMethod != null) {
				deleteMethod.releaseConnection();
			}
		}
	}

	/**
	 * Sends an HTTP POST request and returns the status code.
	 *
	 * @param url URL of the service
	 * @return status code
	 */
	private int httpPost(String url)
	{
		PostMethod postMethod = null;

		try {
			postMethod = new PostMethod(url);
			postMethod.setDoAuthentication(true);
			postMethod.getParams().setParameter("Connection", "Keep-Alive");
			postMethod.setContentChunked(true);
			fc.getHttpClient().executeMethod(postMethod);
		
			return postMethod.getStatusCode();
		} catch (Exception e) {
			e.printStackTrace();
			log.warn("failed to post!");
			
			return -1;
		} finally {
			if (postMethod != null) {
				postMethod.releaseConnection();
			}
		}
	}

	/**
	 * Creates a dummy Fedora object with default attributes.
	 * @param pid pid the new object
	 */
	public void createObject(String pid)
	{
		String url;
		PostMethod postMethod = null;
		int statusCode;

		try {
			pid = URLEncoder.encode(pid, "UTF-8");
		} catch (Exception e) {

		}

		url = baseURL + "/objects/" + pid;
		log.debug("ingesting " + pid);
		statusCode = httpPost(url);
		if (statusCode != SC_OK && statusCode != SC_CREATED) {
			log.warn("status code: " + statusCode);
		};
	}

	/**
	 * Deletes a digital object.
	 * Wrapper of purgeObject in Fedora REST.
	 *
	 * @param pid pid of the object to be deleted
	 */
	public void deleteObject(String pid)
	{
		String url;
		int statusCode;

		try {
			pid = URLEncoder.encode(pid, "UTF-8");
		} catch (Exception e) {

		}

		url = baseURL + "/objects/" + pid;
		statusCode = httpDelete(url);
		if (statusCode != SC_OK && statusCode != SC_NO_CONTENT) {
			log.warn("status code: " + statusCode);
		};
	}

	/**
	 * Wrapper of findObjects in REST
	 * Get a list of objects in Fedora repository
	 *
	 * @param query the pattern of pid
	 * @return a list of pid that satisfy tha pattern
	 */
	public String [] listObjects(String query)
	{
		String response = "";
		String allResponses;
		Pattern pattern;
		Matcher matcher;
		String line;
		String pid;
		List<String> list = new ArrayList<String>();
        String sessionToken;

		try {
			response = fc.getResponseAsString("/objects?query=pid%7E" + 
											  query + "&maxResults=1024&resultFormat=xml&pid=true",
											  true, false);
		} catch (Exception e) {
			return null;
		}

		allResponses = response;
		while (response.contains("<token>")) {
			sessionToken = response.substring(response.indexOf("<token>") + 7,
											  response.indexOf("</token>"));

			try {
				response = fc.getResponseAsString("/objects?query=pid%7E" +
												  query + "&maxResults=1024&resultFormat=xml&pid=true&sessionToken=" + sessionToken, true, false);
			} catch (Exception e) {
				break;
			}

			allResponses += response;
		}

		pattern = Pattern.compile("<pid>[^<]+</pid>");
		matcher = pattern.matcher(allResponses);
		
		while (matcher.find()) {
			line = matcher.group();
			pid = line.substring(5, line.length() - 6);
			list.add(pid);
		}

		return  (String []) list.toArray(new String[0]);
	}

	/**
	 * Wrappper of listDatastreams in REST.
	 *
	 * @param pid pid of the object
	 * @return list of the <code>DataStream</code> objects
	 */
	public DataStream [] listDataStreams(String pid)
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
									   String.format("/objects/%s/datastreams.xml", URLEncoder.encode(pid, "UTF-8")), true, false);
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

			log.debug(dsid + ", " + label + ", " + mimeType);
		}

		return list.toArray(new DataStream[0]);
	}

	/**
	 * Wrapper of getDatastreamDissemination in REST.
	 *
	 * @param pid pid of the object
	 * @param dsID id of the datastream
	 * @return byte content of the data stream
	 */
	public byte[] getDataStream(String pid, String dsID)
	{
		HttpInputStream inputStream;
		ReadableByteChannel channel;
		ByteBuffer buf;
		byte [] bytes;
		int numRead = 0;
		int length = 0;
        
		try {
			inputStream = fc.get(String.format("/objects/%s/datastreams/%s/content", URLEncoder.encode(pid, "UTF-8"), dsID), true, false);
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
	 * Tests if a given digital object already exists in the Fedora repository.
	 *
	 * @param pid pid of the object to be tested
	 * @return whether the object exists
	 */
	public boolean existsObject(String pid)
	{
		String response = "";
		Pattern pattern;
		Matcher matcher;

		try {
			response = fc.getResponseAsString(String.format("/objects?query=pid%%7E%s&resultFormat=xml&pid=true", URLEncoder.encode(pid, "UTF-8")),
											  true, false);
		} catch (Exception e) {
			return false;
		}

		// System.out.println("response: " + response);

		pattern = Pattern.compile("<pid>[^<]+</pid>");
		matcher = pattern.matcher(response);
		
		return matcher.find();
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
		String response = "";
		Pattern pattern, attributePattern;
		Matcher matcher, attributeMatcher;
		String line;
		String s = "";
		String id;

		try {
			response = 
				fc.getResponseAsString(
									   String.format("/objects/%s/datastreams.xml", URLEncoder.encode(pid, "UTF-8")), true, false);
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
	 * Wrapper of addDatastream in Fedora REST.
	 *
	 * @param pid pid of the object
	 * @param dsID id of the data stream
	 * @param mimeType MIME type of the data stream content
	 * @param fileName name of the file storing the data stream content
	 */
	public void addDataStream(String pid, String dsID, 
							  String mimeType, String fileName)
	{
		String url;
		int statusCode;

		try {
			String dsLocation = fc.uploadFile(new File(fileName));
			log.debug("filed uploaded at " + dsLocation);

			if (mimeType == null) {
				// set default MIME type
				mimeType = "application/octet-stream";
			}

			url = baseURL + "/objects/" + URLEncoder.encode(pid, "UTF-8") + 
				"/datastreams/" + dsID +
				"?controlGroup=M&dsLabel=" + dsID + 
				"&mimeType=" + URLEncoder.encode(mimeType, "UTF-8") + 
				"&dsLocation=" + URLEncoder.encode(dsLocation, "UTF-8");

			statusCode = httpPost(url);

			if (statusCode != SC_CREATED) {
				log.warn("status code: " + statusCode);
				log.warn("failed to add data stream!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("failed to add data stream!", e);
		}
	}

	/**
	 * Deletes a data stream.
	 * Wrapper of purgeDatastream in Fedora REST
	 * 
	 * @param pid pid of the object
	 * @param dsID id of the data stream
	 */
	public void deleteDataStream(String pid, String dsID)
	{
		String url;
		int statusCode;

		try {
			pid = URLEncoder.encode(pid, "UTF-8");
		} catch (Exception e) {

		}

		url = baseURL + "/objects/" + pid +	"/datastreams/" + dsID;
		statusCode = httpDelete(url);
		if (statusCode != SC_OK) {
			log.warn("status code: " + statusCode);
		};
	}
}
