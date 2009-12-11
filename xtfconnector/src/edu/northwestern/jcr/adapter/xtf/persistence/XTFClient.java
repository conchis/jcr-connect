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

import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.net.URLEncoder;

import javax.jcr.RepositoryException;

import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

import edu.northwestern.jcr.adapter.xtf.util.ApplyXPath;

/**
 * <p><code>XTFClient</code> accesses XTF repository
 * through the XTF search servlet.<p> Its key function is to
 * take a raw XML query generated in the query processor
 * and execute agaist the raw servlet, and then return
 * a list of matching documents.
 *
 * @author Xin Xiang
 */
public class XTFClient {

	/** log4j logger. */
	private static Logger log = 
		LoggerFactory.getLogger(XTFClient.class);

	/** URL to the XTF search servlet */	
	private String baseURL;

	/** host of the XTF app */
	private String host = "";

	/** port of the XTF app */
	private	String port = "";

    /** Seconds to wait before a connection is established. */
    public int TIMEOUT_SECONDS = 20;

    /** Seconds to wait while waiting for data over the socket (SO_TIMEOUT). */
    public int SOCKET_TIMEOUT_SECONDS = 1800; // 30 minutes

    /** Maxiumum http connections per host (for REST calls only). */
    public int MAX_CONNECTIONS_PER_HOST = 15;

    /** Maxiumum total http connections (for REST calls only). */
    public int MAX_TOTAL_CONNECTIONS = 30;

	/** first part of the raw XML query */
	private static String xmlQuery1 = "<query indexPath=\"index\" style=\"NullStyle.xsl\" maxDocs=\"all\">";

	/** second part of the raw XML query */
	private static String xmlQuery2 = "</query>";

	/**
	 * Creates a new <code>XTFClient</code> instance. If the
	 * property file xtf.properties is present, the property values in 
	 * that file are used. Otherwise the default values are used.
	 */
	public XTFClient() // throws Exception
	{
		boolean property = true;

		Properties props = new Properties();
		try {
			props.load(new FileInputStream("xtf.properties"));
		} catch(IOException e) {
			e.printStackTrace();

			// set default
			host = "connectdev.at.northwestern.edu";
			port = "9090";

			property = false;
		}

		if (property) {
			host = props.getProperty("host");
			port = props.getProperty("port");
		}

		baseURL = "http://" + host + ":" + port
			+ "/" + "xtf";
	}

	/**
	 * Sends an HTTP GET request and returns the GetMethod instance.
	 *
	 * @param url URL of the resource
	 * @return the GetMethod instance
	 */
	private GetMethod httpGet(String url) throws Exception
	{
		GetMethod getMethod;
		MultiThreadedHttpConnectionManager m_cManager;

        m_cManager = new MultiThreadedHttpConnectionManager();
        m_cManager.getParams()
                .setDefaultMaxConnectionsPerHost(MAX_CONNECTIONS_PER_HOST);
        m_cManager.getParams().setMaxTotalConnections(MAX_TOTAL_CONNECTIONS);
        m_cManager.getParams().setConnectionTimeout(TIMEOUT_SECONDS * 1000);
        m_cManager.getParams().setSoTimeout(SOCKET_TIMEOUT_SECONDS * 1000);
        HttpClient client = new HttpClient(m_cManager);

		getMethod = new GetMethod(url);
		getMethod.setDoAuthentication(true);
		getMethod.getParams().setParameter("Connection", "Keep-Alive");
		
		try {
			client.executeMethod(getMethod);
		} catch (Exception e) {
			String msg = "error connecting to the XTF server";
            log.error(msg);
            throw new RepositoryException(msg, null);
		}

		if (getMethod.getStatusCode() != SC_OK) {
			log.warn("status code: " + getMethod.getStatusCode());
		}

		return getMethod;
	}

	/**
	 * Sends an HTTP POST request and returns the GetMethod instance.
	 *
	 * @param url URL of the resource
	 * @return the PostMethod instance
	 */
	private PostMethod httpPost(String url) throws Exception
	{
		PostMethod postMethod;
		MultiThreadedHttpConnectionManager m_cManager;

        m_cManager = new MultiThreadedHttpConnectionManager();
        m_cManager.getParams()
                .setDefaultMaxConnectionsPerHost(MAX_CONNECTIONS_PER_HOST);
        m_cManager.getParams().setMaxTotalConnections(MAX_TOTAL_CONNECTIONS);
        m_cManager.getParams().setConnectionTimeout(TIMEOUT_SECONDS * 1000);
        m_cManager.getParams().setSoTimeout(SOCKET_TIMEOUT_SECONDS * 1000);
        HttpClient client = new HttpClient(m_cManager);

		postMethod = new PostMethod(url);
		postMethod.setDoAuthentication(true);
		postMethod.getParams().setParameter("Connection", "Keep-Alive");
		
		try {
			client.executeMethod(postMethod);
		} catch (Exception e) {
			String msg = "error connecting to the XTF server";
            log.error(msg);
            throw new RepositoryException(msg, null);
		}

		if (postMethod.getStatusCode() != SC_OK) {
			log.warn("status code: " + postMethod.getStatusCode());
		}

		return postMethod;
	}

	/**
	 * Sends an HTTP GET request and returns the response body as
	 * a string.
	 *
	 * @param url URL of the resource
	 * @return the response body as <code>String</code>
	 */
	private String getMethod(String url) throws Exception
	{
		return httpGet(url).getResponseBodyAsString();
	}

	/**
	 * Sends an HTTP POST request and returns the response body as
	 * a string.
	 *
	 * @param url URL of the resource
	 * @return the response body as <code>String</code>
	 */
	private String postMethod(String url) throws Exception
	{
		return httpPost(url).getResponseBodyAsString();
	}

	/**
	 * Sends an HTTP GET request and returns the response body as
	 * a byte array.
	 *
	 * @param url URL of the resource
	 * @return the response body as byte array
	 */
	private byte [] getMethodByte(String url) throws Exception
	{
		return httpGet(url).getResponseBody();
	}

	/**
	 * Returns a list of values for a given facet at a given level.
	 *
	 * @param path list of path strings
	 * @return list of facets
	 */
	public String [] getFacet(String [] path) throws Exception
	{
		String body;
		ApplyXPath xpathProc;
		String [] s = null;
		String queryString;
		String xpath;
		int i;

		if (path == null) {
			queryString = "browse-all=yes";
			xpath = "/crossQueryResult/facet[@field='facet-date']/";
		}
		else {
			queryString = "f1-date=";
			xpath = "/crossQueryResult/facet[@field='facet-date']/";
		}

		for (i = 0; path != null && i < path.length; ++i) {
			queryString += path[i];
			xpath += "group[@value='" + path[i] + "']/";

			if (i < path.length - 1) {
				queryString += "::";
			}
		}

		queryString += "&debugStep=4b";
		xpath += "group/@value";

		System.out.println(queryString);
		System.out.println(xpath);

		body = getMethod(baseURL + "search?" + queryString);
		// System.out.println(body);
		xpathProc = new ApplyXPath();
		try {
			s = xpathProc.evaluateString(body, xpath);
		} catch (Exception e) {
			
		}

		return s;
	}

	/**
	 * Returns a list of files under a given facet at a given level.
	 *
	 * @param path list of path strings
	 * @return list of files
	 */
	public String [] getFiles(String [] path) throws Exception
	{
		String body;
		ApplyXPath xpathProc;
		String [] s = null;
		String queryString;
		String xpath;
		int i;

		queryString = "f1-date=";
		xpath = "/crossQueryResult/docHit/meta[facet-date/text()='";

		for (i = 0; i < path.length; ++i) {
			queryString += path[i];
			xpath += path[i];

			if (i < path.length - 1) {
				queryString += "::";
				xpath += "::";
			}
		}

		queryString += "&debugStep=4b";
		xpath += "']/title/text()";

		// xpath = "/crossQueryResult/docHit/meta[facet-date/text()='2002::01::01']/title/text()";

		System.out.println(queryString);
		System.out.println(xpath);

		body = getMethod(baseURL + "search?" + queryString);
		xpathProc = new ApplyXPath();
		try {
			s = xpathProc.evaluateString(body, xpath);
		} catch (Exception e) {
			
		}

		return s;
	}

	/**
	 * Returns the content of file given its title, assuming no two documents
	 * have the same title.
	 *
	 * @param path list of path strings
	 * @param title title of the item
	 * @return byte content of the file
	 */
	public byte [] getFileContent(String [] path, String title) throws Exception
	{
		String body;
		ApplyXPath xpathProc;
		String [] s = null;
		String queryString;
		String xpath;
		String url;
		int i;

		queryString = "f1-date=";
		xpath = "/crossQueryResult/docHit[meta/facet-date/text()='";

		for (i = 0; i < path.length; ++i) {
			queryString += path[i];
			xpath += path[i];

			if (i < path.length - 1) {
				queryString += "::";
				xpath += "::";
			}
		}

		queryString += "&debugStep=4b";
		xpath += "' and meta/title/text() = '" + title + "']/@path";

		System.out.println(queryString);
		System.out.println(xpath);

		body = getMethod(baseURL + "search?" + queryString);
		// System.out.println(body);
		xpathProc = new ApplyXPath();
		try {
			s = xpathProc.evaluateString(body, xpath);
		} catch (Exception e) {
			
		}

		url = baseURL + "data/" + s[0].replaceAll("default:", "");
		// return s[0];
		return getMethodByte(url);
	}

	/**
	 * Returns a list of paths for hits of keyword search.
	 *
	 * @param keyword keyword
	 * @return list of XTFDoc objects
	 */
	public XTFDoc [] getPathXSLT(String keyword) throws Exception
	{
		String body;
		List<String> list;
		String queryString;
		VTDGen vg = new VTDGen();
		VTDNav vn;
		int a, t;
		String path = "";
		String recordNum = "";
		int pageNumber = 1;
		int totalDocs = 0;
		int totalGroupDocs = 0;
		int retrievedDocs = 0;
		int retrievedGroupDocs = 0;
		boolean itemGroup = false;
		String name, value, cValue;
		int count;
		List<XTFDoc> docList;
		XTFDoc doc;

		list = new ArrayList<String>();
		docList = new ArrayList<XTFDoc>();
		queryString = "query=subject%3A" + URLEncoder.encode(keyword, "UTF-8");
		// queryString = "query=" + 
		// 	URLEncoder.encode(xmlQuery1 + keyword + xmlQuery2, "UTF-8");

		do {
			// iterative over pages
			queryString += "&raw=1";

			System.out.println(queryString);

			body = getMethod(baseURL + "/search?" + queryString);
			// body = postMethod(baseURL + "/rawQuery?" + queryString);
			// System.out.println(body);

			vg.setDoc(body.getBytes("UTF-8"));
			vg.parse(true);
			// vg.parseFile("/Users/xxiang/tmp/why.xml", true);
			vn = vg.getNav();
		
			// move the cursor manually
			if (! vn.matchElement("crossQueryResult") ) {
				return null;
			}

			a = vn.getAttrVal("totalDocs");
			if (a != -1) {
				totalDocs = Integer.parseInt(vn.toNormalizedString(a));
			}
			System.out.println("number of documents: " + totalDocs);
			
			if (! vn.toElement(VTDNav.FC, "facet") ||
				! vn.toElement(VTDNav.FC, "group") ) {
				return null;
			}

			a = vn.getAttrVal("totalDocs");
			if (a != -1) {
				totalGroupDocs = Integer.parseInt(vn.toNormalizedString(a));
			}
			
			if (! vn.toElement(VTDNav.FC, "docHit")) {
				// go to next group
				vn.toElement(VTDNav.NS);
				a = vn.getAttrVal("totalDocs");
				if (a != -1) {
					totalGroupDocs = Integer.parseInt(vn.toNormalizedString(a));
				}
				vn.toElement(VTDNav.FC, "docHit");
			}

			System.out.println("number of documents in group: " + 
							   totalGroupDocs);

			do {
				// iterate over docHit elements
				if (! vn.matchElement("docHit")) {
					// the collection group exhausted
					itemGroup = true;
					retrievedGroupDocs = 0;
					pageNumber = 0;
					break;
				}
				
				a = vn.getAttrVal("path");
				if (a != -1) {
					path = vn.toNormalizedString(a);
				}

				// for query result processing
				path = path.replaceAll(":", "/");

				a = vn.getAttrVal("recordNum");
				if (a != -1) {
					// path and record number
					recordNum = vn.toNormalizedString(a);
					list.add(path + "/" + recordNum);
				}
				else {
					// no record number, path only
					list.add(path);
				}

				doc = new XTFDoc(path, recordNum);

				vn.toElement(VTDNav.FC, "meta");

				// iterate over the metadata fields
				vn.toElement(VTDNav.FC);

				do {
					name = vn.toString(vn.getCurrentIndex());

					if (name.equals("mods")) {
						continue;
					}

					// System.out.print("Name: " + 
					// 				 vn.toString(vn.getCurrentIndex()));
					t = vn.getText(); // get the index of the text
					if (t != -1) {
						value = vn.toNormalizedString(t);
						// System.out.println(", " + "Value: " + 
						// 			   vn.toNormalizedString(t));
					}
					else {
						// System.out.println("");
						System.out.println("Name: " + 
										 vn.toString(vn.getCurrentIndex()));

						count = 0;
						cValue = "";

						int i, min, max;
						min = vn.getCurrentIndex();
						if (vn.toElement(VTDNav.NS)) {
							max = vn.getCurrentIndex();
							
							for (i = min; i < max; ++i) {
								if (vn.getTokenType(i) == 
									VTDNav.TOKEN_CHARACTER_DATA) {
									cValue += vn.toNormalizedString(i);
								}
							}

							vn.toElement(VTDNav.PS);
						}

						value = cValue;
						System.out.println("text: " + cValue);
					}

					doc.addProperty(name, value);
				} while (vn.toElement(VTDNav.NS));
				// end of iteration over metadata fields
				
				vn.toElement(VTDNav.P);

				vn.toElement(VTDNav.P);

				retrievedDocs++;
				retrievedGroupDocs++;

				docList.add(doc);

				System.out.println(retrievedDocs + " doc(s) retrieved");
			} while (vn.toElement(VTDNav.NS, "docHit"));

			System.out.println("number of hits: " + list.size());

			pageNumber++;
			queryString = "query=subject%3A" + 
				URLEncoder.encode(keyword, "UTF-8") + "&page=" + pageNumber;
			if (itemGroup) {
				queryString += "&group=Items";
			}
		} while (retrievedDocs < totalDocs);

		// System.out.println("number of hits: " + list.size());

		return docList.toArray(new XTFDoc[0]);
	}

	/**
	 * Returns a list of paths for hits of raw XML search.
	 *
	 * @param query the raw XML query 
	 * @return list of XTFDoc object
	 */
	public XTFDoc [] getPath(String query) throws Exception
	{
		String body;
		List<String> list;
		String queryString;
		VTDGen vg = new VTDGen();
		VTDNav vn;
		int a, t;
		String path = "";
		String recordNum = "";
		int pageNumber = 1;
		int totalDocs = 0;
		int retrievedDocs = 0;
		String name, value, cValue;
		int count;
		List<XTFDoc> docList;
		XTFDoc doc;
		String identifier, type;

		list = new ArrayList<String>();
		docList = new ArrayList<XTFDoc>();

		// construct the query string
		System.out.println("XML Query: " + xmlQuery1 + query + xmlQuery2);
		queryString = "query=" + 
			URLEncoder.encode(xmlQuery1 + query + xmlQuery2, "UTF-8");

		System.out.println(queryString);

		body = postMethod(baseURL + "/rawQuery?" + queryString);

		// set up the VTD parser
		vg.setDoc(body.getBytes("UTF-8"));
		vg.parse(true);
		vn = vg.getNav();
		
		// move the cursor manually
		if (! vn.matchElement("crossQueryResult") ) {
			return null;
		}

		a = vn.getAttrVal("totalDocs");
		if (a != -1) {
			totalDocs = Integer.parseInt(vn.toNormalizedString(a));
		}
		System.out.println("number of documents: " + totalDocs);

		// navigate to the docHit elements
		if (! vn.toElement(VTDNav.FC, "docHit")) {
			return null;
		}

		// iterate over all the hits
		do {
			a = vn.getAttrVal("path");
			if (a != -1) {
				path = vn.toNormalizedString(a);
			}

			// for query result processing
			path = path.replaceAll(":", "/");

			// The "path" attribute of the <docHit> element in the XTF query 
			// results will be used as the JCR path for the corresponding JCR 
			// node in the Jackrabbit repository. The "recordNum" attribute 
			// will also be attached if it is present.
			a = vn.getAttrVal("recordNum");
			if (a != -1) {
				// path and record number
				recordNum = vn.toNormalizedString(a);
				list.add(path + "/" + recordNum);
			}
			else {
				// no record number, path only
				list.add(path);
			}

			doc = new XTFDoc(path, recordNum);

			vn.toElement(VTDNav.FC, "meta");

			// iterate over the metadata fields
			vn.toElement(VTDNav.FC);

			identifier = "";
			type = "";
			do {
				name = vn.toString(vn.getCurrentIndex());

				if (name.equals("mods")) {
					// ignore MODS elements
					continue;
				}

				// System.out.print("Name: " + 
				// 				 vn.toString(vn.getCurrentIndex()));
				t = vn.getText(); // get the index of the text
				if (t != -1) {
					value = vn.toNormalizedString(t);
					// System.out.println(", " + "Value: " + 
					// 			   vn.toNormalizedString(t));

					if (name.equals("identifier") &&
						value.startsWith("http://")) {
						identifier = value;
					}
					else if (name.equals("facet-type-tab")) {
						System.out.println("type: " + value);
						type = value;
					}
				}
				else {
					// System.out.println("Name: " + 
					// 				   vn.toString(vn.getCurrentIndex()));

					count = 0;
					cValue = "";

					int i, min, max;
					min = vn.getCurrentIndex();
					if (vn.toElement(VTDNav.NS)) {
						max = vn.getCurrentIndex();
							
						for (i = min; i < max; ++i) {
							if (vn.getTokenType(i) == 
								VTDNav.TOKEN_CHARACTER_DATA) {
								cValue += vn.toNormalizedString(i);
							}
						}

						vn.toElement(VTDNav.PS);
					}

					value = cValue;
					// System.out.println("text: " + cValue);
				}

				doc.addProperty(name, value);
			} while (vn.toElement(VTDNav.NS));
			// end of iteration over metadata fields

			if (! identifier.equals("")) {
				setFileURL(doc, identifier, type);
			}
				
			vn.toElement(VTDNav.P);
			vn.toElement(VTDNav.P);

			docList.add(doc);

			System.out.println(++retrievedDocs + " doc(s) retrieved");
		} while (vn.toElement(VTDNav.NS, "docHit")); // iteration of docs

		return docList.toArray(new XTFDoc[0]);
	}

	/**
	 * Sets the file URL property values based on the METS document.
	 * The file URL can be used for retrieval of the content of the item.
	 *
	 * @param doc XTFDoc object
	 * @param identifier the identifier element in query result
	 */
	private void setFileURL(XTFDoc doc, String identifier, String type) 
		throws Exception
	{
		String body = "";
		String metsURL;
		VTDGen vg = new VTDGen();
		VTDNav vn;
		int a;
		String location;

		System.out.println("identifier: " + identifier);

		if (type.equals("website") || identifier.indexOf("ark:") < 0) {
			// identifier is the file location
			System.out.println("adding: " + identifier);
			doc.addProperty("fileLocation", identifier);

			return;
		}

		// identifier is the METS file location
		metsURL = "http://" + host + "/mets/" +
			identifier.substring(identifier.indexOf("ark:")) + "/";
		System.out.println("URL of the METS file: " + metsURL);
		try {
			body = getMethod(metsURL);
		} catch (Exception e) {
			System.err.println("error retrieving the METS file");
		}

		vg.setDoc(body.getBytes("UTF-8"));
		vg.parse(true);
		vn = vg.getNav();
		
		// navigate to the file element in the file section
		if ((! vn.matchElement("mets") || 
			! vn.toElement(VTDNav.FC, "fileSec") ||
			! vn.toElement(VTDNav.FC, "fileGrp") ||
			 ! vn.toElement(VTDNav.FC, "file")) &&
			(! vn.matchElement("mets:mets") || 
			! vn.toElement(VTDNav.FC, "mets:fileSec") ||
			! vn.toElement(VTDNav.FC, "mets:fileGrp") ||
			 ! vn.toElement(VTDNav.FC, "mets:file"))) {
			System.err.println("error parsing");
		}

		// iterate over the file location elements
		do {
			vn.toElement(VTDNav.FC, "FLocat");
			a = vn.getAttrVal("xlink:href");
			if (a != -1) {
				location = vn.toNormalizedString(a);
				if (location.startsWith("http")) {
					System.out.println("adding: " + location);
					doc.addProperty("fileLocation", location);
				}
			}
			vn.toElement(VTDNav.P);
		} while (vn.toElement(VTDNav.NS, "file"));
	}
}
