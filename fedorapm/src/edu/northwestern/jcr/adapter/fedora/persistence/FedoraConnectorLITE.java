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

import javax.jcr.RepositoryException;

import fedora.client.HttpInputStream;

import static org.apache.commons.httpclient.HttpStatus.SC_CREATED;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.apache.commons.httpclient.HttpStatus.SC_NO_CONTENT;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>FedoraConnectorLITE</code> accesses Fedora repository
 * and implements the abstract methods defined in {@link FedoraConnector}
 * using Fedora API-A-LITE API. For a detailed explanation of Fedora REST API please 
 * refer to <a href="http://www.fedora-commons.org/documentation/3.2/API-A-LITE.html">Fedora Repository 3.2 Documentation: API-A-LITE</a>
 * 
 * This class was written for the sole purpose of connecting to legacy Fedora repositories that
 * do not support REST API and should not be used for Fedora version 3.2 and above. It uses
 * a guest account to connect to the Fedora repository and therefore provides read-only access
 * to Fedora contents and supports only simple queries.
 *
 * @author Xin Xiang
 */
public class FedoraConnectorLITE extends FedoraConnector {

    /** log4j logger. */
    private static Logger log = 
        LoggerFactory.getLogger(FedoraConnectorREST.class);

    public FedoraConnectorLITE(String host, String port, String user, String pass, String protocol,
                               String context, String phrase, String gsearchcontext, String gsearchfields)
    {
        super(host, port, user, pass, protocol, context, phrase, gsearchcontext, gsearchfields);
    }
    
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
    private int httpPost(String url) throws Exception
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
            String msg = "error connecting to the Fedora server";
            log.error(msg);
            throw new RepositoryException(msg, null);
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
    public void createObject(String pid) throws Exception
    {
        System.err.println("Operation not supported!");
    }

    /**
     * Deletes a digital object.
     * Wrapper of purgeObject in Fedora REST.
     *
     * @param pid pid of the object to be deleted
     */
    public void deleteObject(String pid)
    {
        System.err.println("Operation not supported!");
    }

    /**
     * Wrapper of findObjects in API-A-LITE
     * Get a list of objects in Fedora repository
     *
     * @param query the pattern of pid
     * @return a list of pid that satisfy tha pattern
     */
    public String [] listObjects(String query) throws Exception
    {
        List<String> list = new ArrayList<String>();

        return  (String []) list.toArray(new String[0]);
    }

    /**
     * Gets the create timestamp of the object.
     *
     * @param pid pid of the object
     */
    public String getCreated(String pid, String dsID)
    {
        String response = "";
        Pattern pattern;
        Matcher matcher;
        String line;

        try {
            if (dsID == null) {
                response = fc.getResponseAsString("/get/" + pid + "?xml=true", true, false);
            }
            else {
                response = fc.getResponseAsString("/get/" + pid + "?xml=true", true, false);
            }
        } catch (Exception e) {
            return null;
        }

        if (dsID == null) {
            pattern = Pattern.compile("<objCreateDate>([^<]+)</objCreateDate>");
        }
        else {
            pattern = Pattern.compile("<objCreateDate>([^<]+)</objCreateDate>");
        }

        matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            line = matcher.group(1);

            return line;
        }

        return  null;
    }

    /**
     * Gets the last modifed timestamp of the object.
     *
     * @param pid pid of the object
     */
    public String getLastModified(String pid)
    {
        String response = "";
        Pattern pattern;
        Matcher matcher;
        String line;

        try {
            response = fc.getResponseAsString("/get/" + pid + "?xml=true", true, false);
        } catch (Exception e) {
            return null;
        }

        pattern = Pattern.compile("<objLastModDate>([^<]+)</objLastModDate>");
        matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            line = matcher.group(1);

            return line;
        }

        return  null;
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
                                       String.format("/listDatastreams/%s?xml=true", 
                                                     URLEncoder.encode(pid, "UTF-8")), true, false);
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
        HttpInputStream inputStream = null;
        ReadableByteChannel channel;
        ByteBuffer buf;
        byte [] bytes;
        int numRead = 0;
        int length = 0;
        
        try {
            if (dsID.equals("DC")) {
                return fc.getResponseAsString(String.format("/get/%s/DC", 
                                                            URLEncoder.encode(pid, "UTF-8")), true, false).getBytes();
            }

            inputStream = fc.get(String.format("/get/%s/%s", 
                                               URLEncoder.encode(pid, "UTF-8"), dsID), true, false);
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
        } catch (Exception e) {
            log.debug("Error getting datastream: " + e.getMessage());
            
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                log.debug("Error closing input stream: " + e.getMessage());
            }
        }
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
            response = fc.getResponseAsString(String.format("/search?query=pid%%7E%s&xml=true&pid=true", 
                                                            URLEncoder.encode(pid, "UTF-8")),
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
                                       String.format("/listDatastreams/%s?xml=true", 
                                                     URLEncoder.encode(pid, "UTF-8")), true, false);
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
     * CHANGE ME - use REST !!!
     * Modfies the default Dublin Core data stream.
     *
     * @param pid pid of the object
     * @param bytes byte content of the new data stream
     */
    public void modifyDCDataStream(String pid, byte [] bytes)
    {
        System.err.println("Operation not supported!");
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
        System.err.println("Operation not supported!");
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
        System.err.println("Operation not supported!");
    }

    /**
     * Gets the comma-separated path consisting of PIDs of the objects 
     * along the path.
     *
     * @param pids list of pid
     * @return list of comma-separated path elements
     */
    @Override
    public String [] getPath(String [] pids)
    {
        return pids;
    }

    /**
     * Runs full-text search agains the gSearch service.
     *
     * @param value value of the search expression
     * @return list of pids
     */
    @Override
    public String [] searchFullText(String value)
    {
        String response = "";
        String allResponses;
        String line;
        int i;
        String pid;
        List<String> list = new ArrayList<String>();
        String url;
        String sessionToken;
        Pattern pattern;
        Matcher matcher;

        list = new ArrayList<String>();

        try {
            value = URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {

        }

        try {
            response = fc.getResponseAsString("/search?terms=" + 
                                              value + "&maxResults=1024&xml=true&pid=true",
                                              true, false);
        } catch (Exception e) {
            return null;
        }

        allResponses = response;
        while (response.contains("<token>")) {
            sessionToken = response.substring(response.indexOf("<token>") + 7,
                                              response.indexOf("</token>"));

            try {
                response = fc.getResponseAsString("/search?terms=" +
                                                  value + "&maxResults=1024&xml=true&pid=true&sessionToken=" + 
                                                  sessionToken, true, false);
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
     * Gets a list of first-level objects (objects that are not a member of 
     * some other object) in Fedora repository through resource index.
     *
     * @param filter filter condition applied - null if there is no filter
     */
    @Override
    public String [] listObjectsRI(String filter) throws Exception
    {
        Pattern pattern;
        Matcher matcher;
        String query;

        query = "*";

        if (filter != null) {
            pattern = Pattern.compile("\"([^\"]+)\"");
            matcher = pattern.matcher(filter);
        
            if (matcher.find()) {
                query = matcher.group(1);
            }
        }

        log.info(query);

        return searchFullText(query);
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
        if (pid == null) {
            return listObjectsRI(filter);
        }
        else {
            System.err.println("Operation not supported!");
            return null;
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
    @Override
    public String getProperty(String pid, String uri)
    {
        Pattern pattern;
        Matcher matcher;
        String field;
        String dcString;

        if (uri.startsWith("http://purl.org/dc/elements/1.1/")) {
            field = uri.replaceAll("http://purl.org/dc/elements/1.1/", "dc:");
            dcString = new String(getDataStream(pid, "DC"));

            pattern = Pattern.compile("<" + field + ">([^<]+)</" + field + ">");
            matcher = pattern.matcher(dcString);
        
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return "";
    }

    /**
     * Tests if the property exists.
     *
     * @param pid pid of the object
     * @param predicate property name
     * @return whether the property exists
     */
    @Override
    public boolean existsProperty(String pid, String predicate)
    {
        String [] fields;

        if (predicate.startsWith("http://purl.org/dc/elements/1.1/")) {
            fields = getDCFields(pid);

            for (String field : fields) {
                if (predicate.equals("http://purl.org/dc/elements/1.1/" + field)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Lists the name (not value) of the properties.
     *
     * @param pid pid of the object
     * @return list of property names
     */
    @Override
    public String [] listProperties(String pid)
    {
        String [] fields;
        List<String> list = new ArrayList<String>();

        fields = getDCFields(pid);
        for (String field : fields) {
            list.add("http://purl.org/dc/elements/1.1/" + field);
        }

        return  (String []) list.toArray(new String[0]);
    }

    private String [] getDCFields(String pid)
    {
        Pattern pattern;
        Matcher matcher;
        String dcString;
        String field;
        List<String> list = new ArrayList<String>();

        dcString = new String(getDataStream(pid, "DC"));

        pattern = Pattern.compile("<dc:([^>]+)>");
        matcher = pattern.matcher(dcString);
        
        while (matcher.find()) {
            field = matcher.group(1);
            list.add(field);
        }

        return  (String []) list.toArray(new String[0]);
    }
}
