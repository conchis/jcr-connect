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

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.AbstractPersistenceManager;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

// new
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;

/**
 * <p><code>FedoraPersistenceManager</code> is a Fedora-based
 * persistence manager that persists <a href="http://jackrabbit.apache.org/api/1.5/org/apache/jackrabbit/core/state/ItemState.html">ItemState</a>
 * and <a href="http://jackrabbit.apache.org/api/1.5/org/apache/jackrabbit/core/state/NodeReferences.html">NodeReferences</a> objects using Fedora REST API or
 * API-A/API-M.</p>
 *
 * <p>The persistence manager implements the 
 * <a href="http://jackrabbit.apache.org/api/1.5/org/apache/jackrabbit/core/persistence/PersistenceManager.html">org.apache.jackrabbit.core.persistence.PersistenceManager</a> interface. 
 * It translates all node/property storing and loading requests to API calls 
 * (REST or API-A/API-M) made to the underlying Fedora repository.</p>
 * 
 * <p>In the default workspace configuration file 
 * <code>jackrabbit/workspaces/default/workspace.xml</code> this Fedora 
 * persistence manager
 * is set as the persistence manager used by the Jackrabbit repository
 * (in place of the default <code>ObjectPersistenceManager</code>).
 *
 * <p> The backbone of this persistence manager consists of the following
 * methods defined in the Jackrabbit persistence manager interface:
 * <ul>
 * <li>load()<br>
 * Loads persisted node/property data into memory.
 * <li>store()<br>
 * Persists node/property data to the underlying repository.
 * <li>exists()<br>
 * Tests if the node/property exists in the underlying repository.
 * <li>destroy()<br>
 * Permanently deletes the node/property from the underlying repository.
 * </ul>
 * </p>
 *
 * <p> For a detailed document on persistence manager configuration
 * in Jackrabbit, please refer to <a href="http://wiki.apache.org/jackrabbit/PersistenceManagerFAQ">Persistence Manager (PM) FAQ on the Jackrabbit Wiki</a>.
 *
 * <p>In a Fedora repository PIDs are unique identifiers of objects while 
 * in JCR multiple nodes can have the same name. To map the JCR paths of 
 * different nodes to different PIDs, the following format is used:
 *
 * <p><code>slingX:Y%57JCR_Name</code>
 *
 * <p>where X represents the level of the node in the JCR tree and Y is 
 * the hash code for the full JCR path of the node.
 *
 * <p>To map the PID of an existing Fedora object to the name of a JCR 
 * node some rules are applied:
 * <ul>
 * <li>Colon (:) is replaced by underscore (_) so as not to be confused 
 * with the namespace delimiter.
 * <li>Underscore (_) is replaced by space ( ) and two underscores (__) in 
 * PID represent a single underscore (_) in JCR path.
 * </ul>
 * For example, the node with path "/a" will be mapped to a Fedora object 
 * with PID "sling1:1554%57a", while an existing Fedora object with PID 
 * "test:example" will be named "test_example" from the JCR perspective. 
 *
 * @author Xin Xiang
 */
public class FedoraPersistenceManager extends AbstractPersistenceManager {

	/** log4j logger */
	private static Logger log = 
		LoggerFactory.getLogger(FedoraPersistenceManager.class);

	/** initialization flag */
	private boolean initialized;

	/** map UUID of the digital object node to JCR path */
	private static Map<String, String> uuidMap = new HashMap<String, String>();

	/** map digital object node ID to its parent digital object node ID */
	private static Map<String, String> doMap = new HashMap<String, String>();

	/** map data stream node ID to its parent digital object node ID */
	private static Map<String, String> dsMap = new HashMap<String, String>();

	/** map jcr:content node ID to its parent data stream node ID */
	private static Map<String, String> contentMap = 
		new HashMap<String, String>();

	/** list of IDs of nodes reprenting Sling folders */
	private static List<String> slingNodeList = new ArrayList<String>();

	/** list of IDs of nodes reprenting Fedora objects */
	private static List<String> fedoraNodeList = new ArrayList<String>();

	/** map child UUID to relative path */
	private static Map<String, String> childrenMap = 
		new HashMap<String, String>();

	/** map UUID, DS name to DS UUID */
	private static Map<String, Map<String, String>> dsNameMap =
		new HashMap<String, Map<String, String>>();

	/** fedora client handle */
	public static FedoraConnector fc;

	/** list of nodes that are pending to persist */
	private static Map<String, NodeState> pendingNodeMap = 
		new HashMap<String, NodeState>(); 

	/** list of properties that are pending to persist */
	private static List<PropertyState> pendingProperties = 
		new ArrayList<PropertyState>();

	/** UUID of root node in Jackrabbit */
	private final static String JR_ROOT_ID =
		"cafebabe-cafe-babe-cafe-babecafebabe";

	/** UUID of system node in Jackrabbit */
	private final static String JR_SYSTEM_ID =
		"deadbeef-cafe-babe-cafe-babecafebabe";

	/** namespace URI for the fedora prefix */
	private final static String FEDORA_NAMESPACE_URI = "";
		// "http://www.fedora.info/";

	/**
	 * Creates a new <code>FedoraPersistenceManager</code> instance.
	 */
	public FedoraPersistenceManager() throws RepositoryException {
		initialized = false;

		boolean property = true;
		boolean useREST = true;
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("fedora.properties"));
		} catch(IOException e) {
			property = false;
		}

		if (property && ! props.getProperty("rest").equals("y")) {
			useREST = false;
		}

		try {
			if (useREST) {
				fc = new FedoraConnectorREST();
			}
			else {
				fc = new FedoraConnectorAPIX();
			}
		} catch (Exception e) {
			String msg = "error connecting to the Fedora server";
            log.error(msg);
            throw new RepositoryException(msg, e);
		}
	}


	// ---------------------------------< PersistenceManager >
	/**
	 * Initializes the persistence manager. Not used.
	 */
	public void init(PMContext context) throws Exception {
		if (initialized) {
			throw new IllegalStateException("already initialized");
		}

		// to be implemented

		initialized = true;
	}

	/**
	 * Closes the persistence manager. Not used.
	 */
	public synchronized void close() throws Exception {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented		
	}

	/**
	 * Converts a Fedora pid to relative JCR path.
	 *
	 * @param pid pid in Fedora
	 * @return relative path in JCR
	 */
	public static String escapePID(String pid)
	{
		String id;
		String [] parts;
		int i;

		if (isSlingNode(pid)) {
			// Convert Fedora pid to JCR path name for sling objects
			// id is xxxxx in sling:xxxxx
			// distinguish objects created in sling with other objects
			// sling:untitled_folder
			pid = pid.substring(pid.indexOf(":") + 1);	
			// untitled_folder

			i = pid.lastIndexOf("%57");
			pid = pid.substring(i + 3);

			// untitled folder
			parts = pid.split("__");
			id = "";

			for (i = 0; i < parts.length; ++i) {
				id += parts[i].replaceAll("_", " ");

				if (i < parts.length - 1) {
					id += "_";
				}
			}				
		}		
		else {
			// Fedora objects
			// escape :
			id = pid.replace("_", "__");
			id = id.replace(":", "_");
		}

		return id;
	}

	/**
	 * Restores Fedora pid from JCR path for native Fedora objects.
	 * 
	 * @param id relative JCR path
	 * @return Fedora PID
	 */
	public static String unescapePID(String id)
	{
		String [] parts = id.split("__");
		String pid = "";
		boolean colon = false;

		for (int i = 0; i < parts.length; ++i) {
			if (! colon) {
				pid += parts[i].replaceFirst("_", ":");
			}
			else {
				pid += parts[i];
			}

			if (pid.contains(":")) {
				colon = true;
			}

			if (i < parts.length - 1) {
				pid += "_";
			}
		}				

		return pid;
	}


	/** 
	 * Converts JCR path name to Fedora pid for sling objects (sling prefix
	 * is not included).
	 *
	 * @param id relative JCR path
	 * @return the part after : of the Fedora pid
	 */
	public static String unescapePIDSling(String id)
	{
		String pid;

		pid = id.replaceAll("_", "__");
		pid = pid.replaceAll("\\s+", "_");

		return pid;
	}

	/**
	 * Returns the Fedora pid of a JCR node.
	 *
	 * @param nodeID String UUID of the JCR node
	 * @return pid of the corresponding Fedora object
	 */
	private String getPID(String nodeID)
	{
		String pid;

		pid = getJCRPath(nodeID);

		if (pid == null) {
			return null;
		}

		log.debug("escaped pid: " + pid);

		// recover Fedora PID from escaped relative path
		if (! fedoraNodeList.contains(nodeID)) {
			// attach the hash code of the full path to distinguish
			// nodes of the same name at the same level
			pid = uuidMap.get(nodeID).hashCode() + "%57" + pid;
			pid = "sling" + getJCRLevel(nodeID) + ":" + unescapePIDSling(pid);
		}
		else {
			pid = unescapePID(pid);
		}

		if (pid.length() > 64) {
			pid = pid.substring(0, 64);
		}

		return pid;
	}

	/**
	 * Converts Fedora data stream ID to relative JCR path.
	 *
	 * @param dsID id of the data stream
	 * @return relative JCR path of the corresponding node
	 */
	private String escapeDSID(String dsID)
	{
		String id;
		String [] parts;
		int i;

		// test___jpg
		parts = dsID.split("__");
		id = "";

		for (i = 0; i < parts.length; ++i) {
			id += parts[i].replaceAll("_", ".");

			if (i < parts.length - 1) {
				id += "_";
			}
		}				
		
		// test_.jpg
	
		// remove "DS" prefix
		if (id.startsWith("DS")) {
			id = id.substring(2);
		}

		return id;
	}

	/**
	 * Converts relative JCR path to Fedora data stream ID.
	 *
	 * @param dsID relative JCR path
	 * @return id of the corresponding data stream
	 */
	private String unescapeDSID(String dsID)
	{
		String id;

		id = dsID.replace("_", "__");
		id = id.replace(".", "_");

		// attach "DS" in case it starts with digit
		return "DS" + id;
	}

	/**
	 * Associates the relative path of a node with its uuid.
	 *
	 * @param id string UUID of the node
	 * @param relativePath relative JCR path of the node
	 * @param parentID string UUID of the parent node
	 */
	private void putJCRPath(String id, String relativePath, String parentID)
	{
		String parentPath;

		if (id.equals(JR_ROOT_ID)) {
			uuidMap.put(id, "/");
			return;
		}

		if (parentID == null) {
			parentPath = "";
			parentID = JR_ROOT_ID;
		}
		else {
			parentPath = uuidMap.get(parentID);
		}

		doMap.put(id, parentID);

		if (uuidMap.get(id) == null) {
			uuidMap.put(id, parentPath + "/" + relativePath);
		}
	}

	/**
	 * Gets the relative JCR path of a node given its uuid.
	 *
	 * @param uuid string UUID of the node
	 * @return relative JCR path of the node
	 */
	private String getJCRPath(String uuid)
	{
		String fullPath;

		fullPath = uuidMap.get(uuid);

		if (fullPath == null) {
			return null;
		}

		log.debug("uuid: " + uuid);
		log.debug("full path: " + fullPath);

		// return empty string for root node
		return fullPath.substring(fullPath.lastIndexOf('/') + 1);
	}

	/**
	 * Determine if the two nodes are related.
	 */
	private boolean isParentChild(String parentPath, String childPath)
	{
		if (parentPath.equals("/")) {
			parentPath = "";
		}

		if (childPath.startsWith(parentPath + "/") &&
			childPath.substring(parentPath.length() + 1).indexOf("/") == -1) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Creates a map from uuid of its children to relative path.
	 *
	 * @param uuid string UUID of the node
	 */
	private void getChildrenMap(String uuid)
	{
		String path;
		String nodePath;

		path = uuidMap.get(uuid);

		for (String key : uuidMap.keySet()) {
			if (key.equals(uuid)) {
				// skip itself
				continue;
			}

			nodePath = uuidMap.get(key);

			if (isParentChild(path, nodePath)) {
				childrenMap.put(nodePath.substring(nodePath.lastIndexOf('/') + 
												   1), key);
			}
		}
	}

	/**
	 * Gets the UUID of the child node given its name.
	 *
	 * @param name name of the child node
	 * @return string UUID of the child node
	 */
	private String getChildUUID(String name)
	{
		return childrenMap.get(name);
	}

	/**
	 * Cleans the map from uuid of children to relative path.
	 */
	private void cleanChildrenMap()
	{
		childrenMap.clear();
	}

	/**
	 * Gets the level of a node in JCR hieararchy given its uuid.
	 *
	 * @param uuid string UUID of the node
	 * @return level of the node in the JCR tree
	 */
	private int getJCRLevel(String uuid)
	{
		String fullPath;

		fullPath = uuidMap.get(uuid);

		return fullPath.split("/").length - 1;
	}

	/**
	 * Gets parent node ID of a digital object node
	 *
	 * @param id string UUID of the node
	 * @return string UUID of the parent node
	 */
	private String getParentID(String id)
	{
		return doMap.get(id);
	}

	/**
	 * Tests if the digital object represents an object created in Sling
	 * through the JCR adapter (not a native Fedora object), or if the
	 * pid starts with "sling\\d+:".
	 *
	 * @param pid pid of the object
	 * @return whether it is an object created through the JCR adapter
	 */
	private static boolean isSlingNode(String pid)
	{
		Pattern pattern;
		Matcher matcher;

		pattern = Pattern.compile("sling\\d+:");
		matcher = pattern.matcher(pid);
		
		return matcher.find();
	}

	/**
	 * Translates property URI used in Fedora to internal JCR property name, 
	 * for example, {http://www.jcp.org/jcr/1.0}data.
	 *
	 * @param uri URI of the property
	 * @return JCR property name
	 */
	private String getPropertyName(String uri)
	{
		int index;
		String namespaceURI;
		String name;

		index = uri.lastIndexOf("/");
		namespaceURI = uri.substring(0, index);
		if (namespaceURI.equals("http://sling.apache.org/jcr/sling/1.0")) {
			namespaceURI = "";
		}
		name = uri.substring(index + 1);

		return "{" + namespaceURI + "}" + name;
	}

	/**
	 * Translates internal JCR property name to property URI, for example,
	 * http://www.jcp.org/jcr/1.0/data
	 *
	 * @param name JCR property name
	 * @return URI of the property
	 */
	private String getPropertyURI(String name)
	{
		int index;
		String namespaceURI;

		index = name.indexOf("}");

		if (index == 1) {
			// empty URI
			namespaceURI = "http://sling.apache.org/jcr/sling/1.0";
		}
		else {
			namespaceURI = name.substring(1, index);
		}

		return  namespaceURI + "/" + name.substring(index + 1);
	}

	/**
	 * Translates internal property type 
	 * (for example {http://www.jcp.org/jcr/nt/1.0}unstructured)
	 * to JCR property type (for example
	 * nt:unstructured)
	 *
	 * @param name internal property type
	 * @return JCR property type
	 */
	private String getPropertyType(String name)
	{
		int index;

		if (name.startsWith("{http://www.jcp.org/jcr/nt/1.0}")) {
			index = name.indexOf("}");
			return "nt:" + name.substring(index + 1);
		}
		
		return name;
	}

	/**
	 * Sets the node state of a node representing a Fedora digital object.
	 *
	 * @param state the <code>NodeState</code> object to be persisted
	 */
	private void setDONodeState(NodeState state)
	{
		String uuid;
		String pid, escapedPID;
		Name name;
		DataStream [] dsList;
		String nodeID;
		String [] memberList;
		String [] propertyList;

		nodeID = state.getNodeId().toString();
		pid = getPID(nodeID);

		if (fc.existsProperty(pid, "http://www.jcp.org/jcr/1.0/data")) {
			// only nt:resource node has jcr:data
			state.setNodeTypeName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/nt/1.0}resource"));
		}
		else {
			state.setNodeTypeName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/nt/1.0}unstructured"));
		}
		state.setParentId(new NodeId(getParentID(nodeID)));
		// state.setDefinitionId(NodeDefId.valueOf("-1603354723"));
		state.setModCount((short) 0);			
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));

		log.debug("list datastreams of " + pid);
		dsList = fc.listDataStreams(pid);

		if (dsList == null) {
			// policy???
			log.debug("no datastream!");
			return;
		}

		Map<String, String> map;

		map = dsNameMap.get(nodeID);
		if (map == null) {
			map = new HashMap<String, String>();
			dsNameMap.put(nodeID, map);
		}

		for (DataStream ds : dsList) {
			if (ds.id.equals("DC") || ds.id.equals("RELS-EXT")) {
				// skip the built-in datastreams since they should not
				// be visible from JCR API
				continue;
			}

			// add a child node of DC
			if (map.get(ds.id) != null) {
				uuid = map.get(ds.id);
			}
			else {
				uuid = new NodeId().toString(); // UUID.randomUUID();
			}

			// associate the UUID with the data stream object
			if (map.get(ds.id) == null) {
				ds.setUUID(uuid);
				map.put(ds.id, uuid);
			}
			
			// link to its parent (do) ID
			dsMap.put(uuid.toString(), nodeID);

			// name
			name = NameFactoryImpl.getInstance().create("{}" + 
														escapeDSID(ds.id));
			// uuid
			state.addChildNodeEntry(name, new NodeId(uuid));
		}

		log.debug("list members of " + pid);
		memberList = fc.listMembers(pid, null);

		if (getJCRPath(nodeID) != null) {
			// create children map
			cleanChildrenMap();
			getChildrenMap(nodeID);
		}

		if (memberList != null) {
			for (String memberPID : memberList) {
				// add a child node
				if (fc.existsProperty(memberPID, 
									  "http://www.jcp.org/jcr/1.0/uuid")) {
					// use the UUID in the property jcr:uuid

					String propertyValue = fc.getProperty(memberPID, 
														  "http://www.jcp.org/jcr/1.0/uuid");
					String [] parts = propertyValue.split("%57");
					// type, number of values, definitionID, modCount
					propertyValue = parts[4];
							
					uuid = propertyValue;
				}
				else if (getChildUUID(escapePID(memberPID)) != null) {
					uuid = getChildUUID(escapePID(memberPID));
				}
				else {
					uuid = new NodeId().toString(); // UUID.randomUUID();
				}

				String namespaceURI = "";

				// add to sling node list
				// if (isSlingNode(memberPID)) {
				if (! isSlingNode(memberPID)) {				
					// slingNodeList.add(uuid.toString());
					fedoraNodeList.add(uuid.toString());
					namespaceURI = FEDORA_NAMESPACE_URI;
				}

				escapedPID = escapePID(memberPID);
				putJCRPath(uuid.toString(), escapedPID, nodeID);

				log.debug("adding child node: " + escapedPID);

				if (escapedPID.equals("xmltext")) {
					// needs a better way to handle
					// the jcr name space for pid
					namespaceURI = "http://www.jcp.org/jcr/1.0";
				}

				// name
				name = NameFactoryImpl.getInstance().create("{" + namespaceURI +
															"}" + escapedPID);
				// uuid
				state.addChildNodeEntry(name, new NodeId(uuid));
			}

			cleanChildrenMap();
		}

		log.debug("list properties of " + pid);
		propertyList = fc.listProperties(pid);

		if (propertyList == null) {
			return;
		}

		for (String propertyURI : propertyList) {
			if (propertyURI.equals("http://sling.apache.org/jcr/sling/1.0/NodeReferences")) {
				// reserved for node references
				continue;
			}
			if (propertyURI.equals("http://sling.apache.org/jcr/sling/1.0/MixinTypes")) {
				// reserved for mixin types
				String s = 
					fc.getProperty(pid, 
								   "http://sling.apache.org/jcr/sling/1.0/MixinTypes");
				String [] parts = s.split("%57");
				int numTypes= Integer.parseInt(parts[0].replaceAll("\"", ""));
				Set set = new HashSet(numTypes);
				for (int i = 0; i < numTypes; ++i) {
					set.add(NameFactoryImpl.getInstance().create(parts[i + 1]));
				}
				state.setMixinTypeNames(set);

				// not a property
				continue;
			}

			state.addPropertyName(NameFactoryImpl.getInstance().create(getPropertyName(propertyURI)));
		}
	}

	/**
	 * Sets the node state of a node representing a Fedora data stream.
	 *
	 * @param state the <code>NodeState</code> object to be persisted
	 * @param parentID string UUID of the parent node
	 */
	private void setDSNodeState(NodeState state, String parentID)
	{
		String uuid = null;
		Name name;
		String id;

		// of node type nt:file
		state.setNodeTypeName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/nt/1.0}file"));
		try {
			state.setParentId(new NodeId(parentID));
		} catch (Exception e) {
			System.err.println("parentID: " + parentID);
		}

		// state.setDefinitionId(NodeDefId.valueOf("-1603354723"));
		state.setModCount((short) 0);	
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));

		id = state.getNodeId().toString();

		// add a child node of jcr:content

		for (String key : contentMap.keySet()) {
			if (contentMap.get(key).equals(id)) {
				uuid = key;
				break;
			}
		}

		if (uuid == null) {
			uuid = new NodeId().toString(); // UUID.randomUUID();
		}

		// link to its parent (DS) ID
		contentMap.put(uuid.toString(), id);

		// name
		name = NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}content");
		// uuid
		state.addChildNodeEntry(name, new NodeId(uuid));
	}

	/**
	 * Sets the node state of a jcr:content node representing content of
	 * a Fedora data stream.
	 *
	 * @param state the <code>NodeState</code> object to be persisted
	 * @param parentID string UUID of the parent node
	 */
	private void setContentNodeState(NodeState state, String parentID)
	{
		String uuid;
		Name name;

		state.setNodeTypeName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/nt/1.0}resource"));
		state.setParentId(new NodeId(parentID));
		// state.setDefinitionId(NodeDefId.valueOf("-1603354723"));
		state.setModCount((short) 0);	
		// state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}data"));
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}encoding"));
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}mimeType"));
	}

	/**
	 * Loads the specified node into a <code>NodeState</code> object.
	 *
	 * @param id the <code>NodeId</code> object representing the node
	 * @return the <code>NodeState</code> object
	 */
	public synchronized NodeState load(NodeId id)
			throws NoSuchItemStateException, ItemStateException {
		String doUUID;
		String dsUUID;
		String [] pidList;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		NodeState state = createNew(id);
		log.debug("loading node: " + state.getNodeId());
		String nodeID = state.getNodeId().toString();

		// load from pending node if pending
		for (String s : pendingNodeMap.keySet()) {
			NodeState ns = pendingNodeMap.get(s);
			if (ns.getNodeId().toString().equals(id.toString())) {
				state.setNodeTypeName(ns.getNodeTypeName());
				state.setParentId(ns.getParentId());
				// state.setDefinitionId(ns.getDefinitionId());
				state.setModCount((short) 0);			
				
				Collection c = ns.getChildNodeEntries();
				for (Iterator iter = c.iterator(); iter.hasNext();) {
					ChildNodeEntry entry = (ChildNodeEntry) iter.next();
					NodeId nodeid = entry.getId();
					Name name = entry.getName();

					state.addChildNodeEntry(name, nodeid);
				}

				return state;
			}
		}


		if (nodeID.equals(JR_SYSTEM_ID)) {
			// fabricate a system node in momory

			state.setNodeTypeName(NameFactoryImpl.getInstance().create("{internal}system"));
			state.setParentId(new NodeId(JR_ROOT_ID));
			// state.setDefinitionId(NodeDefId.valueOf("-1971945898"));
			state.setModCount((short) 0);			
			state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));
			// name
			Name name = NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}versionStorage");
			// uuid
			state.addChildNodeEntry(name, new NodeId("deadbeef-face-babe-cafe-babecafebabe"));
			name = NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}nodeTypes");
			// uuid
			state.addChildNodeEntry(name, new NodeId("deadbeef-cafe-cafe-cafe-babecafebabe"));
		}

		else if (nodeID.equals(JR_ROOT_ID)) {
			// fabricate a root node in momory

			state.setNodeTypeName(NameFactoryImpl.getInstance().create("{internal}root"));
			// state.setDefinitionId(NodeDefId.valueOf("-1537436024"));
			state.setModCount((short) 0);			
			state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));
			// name
			Name name = NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}system");
			// uuid
			state.addChildNodeEntry(name, new NodeId("deadbeef-cafe-babe-cafe-babecafebabe"));

			// get the list of all first-level objects
			pidList = null;
			try {
				pidList = fc.listObjectsRI(null);
			} catch (Exception e) {

			}

			if (getJCRPath(nodeID) != null) {
				// create children map
				cleanChildrenMap();
				getChildrenMap(nodeID);
			}

			// put each of the Fedora digital objects as child node of root
			for (String pid : pidList) {
				String uuid;

				if (fc.existsProperty(pid, 
									  "http://www.jcp.org/jcr/1.0/uuid")) {
					// use the UUID in the property jcr:uuid

					String propertyValue = fc.getProperty(pid, 
														  "http://www.jcp.org/jcr/1.0/uuid");
					String [] parts = propertyValue.split("%57");
					// type, number of values, definitionID, modCount
					propertyValue = parts[4];

					uuid = propertyValue;
				}
				else if (getChildUUID(escapePID(pid)) != null) {
					// make sure the UUID stays the same over multiple loading
					uuid = getChildUUID(escapePID(pid));
				}
				else {
					uuid = new NodeId().toString(); // UUID.randomUUID();
				}

				String namespaceURI = "";

				if (! isSlingNode(pid)) {
					fedoraNodeList.add(uuid.toString());
					namespaceURI = FEDORA_NAMESPACE_URI;
				}

				pid = escapePID(pid);
				putJCRPath(uuid.toString(), pid, null);

				// name
				name = NameFactoryImpl.getInstance().create("{" + namespaceURI +
															"}" + pid);
				// uuid
				state.addChildNodeEntry(name, new NodeId(uuid));
			}

			// put the root node
			putJCRPath(nodeID, "", null);
			cleanChildrenMap();
		}
		else {
			// regular nodes (non-system and non-root)
			String path = getJCRPath(nodeID);
			
			if (path != null) {
				log.debug("JCR path: " + path);
				// a digital object node
				setDONodeState(state);
			}
			else {
				// not a digital object node
				doUUID = dsMap.get(nodeID);

				if (doUUID != null) {
					// a data stream node
					setDSNodeState(state, doUUID);
				}
				else {
					// not a data stream node
					dsUUID = contentMap.get(nodeID);

					if (dsUUID != null) {
						// a jcr:content:node
						setContentNodeState(state, dsUUID);
					}
					else {
						// should not happen
					}
				}
			}
		}
		
		log.info("node loaded");
		
		return state;
	}

	/**
	 * Loads the specified property into a <code>PropertyState</code> object.
	 *
	 * @param id the <code>PropertyId</code> object representing the property
	 * @return the <code>PropertyState</code> object
	 */
	public synchronized PropertyState load(PropertyId id)
			throws NoSuchItemStateException, ItemStateException 
	{
		int index;
		String nodeID;
		String propertyName;
		String pid; // Fedora pid
		String dsID; // "DC" for example
		String dsNodeID; // UUID of the DS Node
		DataStream dataStream; // DataStream object
		byte [] bytes;
		String s;
		PropertyState state;
		InternalValue[] values;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		log.debug("loading property: " + id.toString());

		state = createNew(id);

		// populate member variables
		state.setType(1);
		state.setMultiValued(false);
		// state.setDefinitionId(PropDefId.valueOf("806470580"));
		state.setModCount((short) 0);

		index = id.toString().indexOf("/");
		// jcr:content node
		nodeID = id.toString().substring(0, index);
		propertyName = id.toString().substring(index + 1);

		values = new InternalValue[1];

		// load from pending node if pending
		for (PropertyState ps : pendingProperties) {
			if (ps.getPropertyId().toString().equals(id.toString())) {
				InternalValue[] v = ps.getValues();
				if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data")) {
					// BLOBFileValue blobVal = v[0].getBLOBFileValue();
					try {
						InputStream in = v[0].getStream(); // blobVal.getStream();
						values[0] = InternalValue.create(in);
					} catch (Exception e) {
						
					}
				}
				else {
					values[0] = InternalValue.valueOf(v[0].toString(), 1);
				}
				
				state.setValues(values);
				
				return state;
			}
		}

		if (nodeID.equals(JR_ROOT_ID) &&
			propertyName.equals("{http://www.jcp.org/jcr/1.0}primaryType")) {
			state.setType(PropertyType.NAME);
			values[0] = InternalValue.valueOf("rep:root", 1);
			state.setValues(values);

			log.debug("property loaded");
			
			return state;
		}


		if (getJCRPath(nodeID) != null) {
			// digital object node
			pid = getPID(nodeID);
			s = fc.getProperty(pid, getPropertyURI(propertyName));

			if (propertyName.contains("http://purl.org/dc/elements/1.1")) {
				// Dublin Core properties
				state.setType(PropertyType.STRING);
				state.setModCount((short) 0);
				values[0] = InternalValue.valueOf(s, PropertyType.STRING);
				state.setValues(values);

				log.debug("property loaded");
			
				return state;
			}


			if (propertyName.equals("{http://www.jcp.org/jcr/1.0}primaryType")) {
				if (s == null) {
					// existing Fedora objects - immitate a string
					s = "7%571%570%570%57{http://www.jcp.org/jcr/nt/1.0}unstructured";
				}
				else {
					s = getPropertyType(s);
				}
			}

			int type = 1;
			String [] parts = s.split("%57");
			int numValues;

			// type
			type = Integer.parseInt(parts[0].replaceAll("\"", ""));
			state.setType(type);
			// number of values
			numValues = Integer.parseInt(parts[1].replaceAll("\"", ""));

			if (numValues > 1) {
				// multi-valued property
				state.setMultiValued(true);
			}

			// definitionId
			// state.setDefinitionId(PropDefId.valueOf(parts[2]));
			// modCount
			state.setModCount(Short.parseShort(parts[3]));

			// values
			values = new InternalValue[numValues];

			for (int i = 4; i < parts.length; ++i) {
				if (type != PropertyType.BINARY) {
					values[i - 4] = InternalValue.valueOf(parts[i], type);
				}
				else {
					try {
						InputStream is = 
							new ByteArrayInputStream(parts[i].getBytes("UTF-8"));

					values[i - 4] = InternalValue.create(is);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			state.setValues(values);

			log.debug("property loaded");
			
			return state;
		}

		// data stream node (parent of jcr:content)
		dsNodeID = contentMap.get(nodeID);

		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data") ||
			propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding") ||
			propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType") ) {
			// digital object node (parent of data stream node)
			nodeID = dsMap.get(dsNodeID);
		}

		dataStream = DataStream.getDSFromUUID(dsNodeID);

		if (dataStream == null) {
			log.debug("");
			log.debug("property: " + propertyName);
			log.debug("DO node: " + nodeID);
			log.debug("DS node: " + dsNodeID);
			log.debug("");

		}		

		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data")) {
			dsID = dataStream.id;
			state.setType(PropertyType.BINARY);
		}
		else if (propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding")) {
			dsID = "";

			// set the defulat encoding to UTF-8
			values[0] = InternalValue.valueOf("UTF-8", 1);
		}
		else if (propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType")) {
			dsID = "";

			values[0] = InternalValue.valueOf(dataStream.mimeType, 1);
		}
		else {
			dsID = propertyName.substring(propertyName.indexOf("}") + 1);
		}

		if (! dsID.equals("") &&
			! propertyName.equals("{http://www.jcp.org/jcr/1.0}primaryType")) {
			// get data stream
			pid = getPID(nodeID);

			bytes = fc.getDataStream(pid, dsID);

			try {
				if (dataStream.mimeType.startsWith("text/")) {
					// text stream such as DC
					s = new String(bytes, "UTF-8");
					values[0] = InternalValue.valueOf(s, 1);
				}
				else {
					// other (image, audio, etc.)
					values[0] = InternalValue.create(bytes);
				}
			} catch (Exception e) {

			} 
		}

		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}primaryType")) {
			if (nodeID.equals(JR_ROOT_ID)) {
				// root
				// values[0] = InternalValue.valueOf("{internal}root", 1);
			}
			else if (getJCRPath(nodeID) != null) {
				// digital object node
				values[0] = InternalValue.valueOf("{http://www.jcp.org/jcr/nt/1.0}unstructured", PropertyType.NAME);
			}
			else if (dsMap.get(nodeID) != null) {
				// data stream node
				values[0] = InternalValue.valueOf("{http://www.jcp.org/jcr/nt/1.0}file", PropertyType.NAME);
			}
			else if (contentMap.get(nodeID) != null) {
				// content node
				values[0] = InternalValue.valueOf("{http://www.jcp.org/jcr/nt/1.0}resource", PropertyType.NAME);
			}
		}

		state.setValues(values);

		log.debug("property loaded");

		return state;
	}

    /**
     * {@inheritDoc}
     */
    public synchronized NodeReferences loadReferencesTo(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
		String nodeID, pid;
		String propertyURI;
		String s;
		String [] parts;
		int count;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		NodeReferences refs = new NodeReferences(id);
		
        refs.clearAllReferences();

        // references
		// target node ID
		nodeID = id.toString();
		// pid of the digital object
		pid = getPID(nodeID);
		// URI of the property
		propertyURI = "http://sling.apache.org/jcr/sling/1.0/NodeReferences";
		
		s = fc.getProperty(pid, propertyURI);

		if (s == null) {
			// no such reference
			throw new NoSuchItemStateException(id.toString());
		}

		parts = s.split("%57");
		count = Integer.parseInt(parts[0]);
        for (int i = 0; i < count; i++) {
			// propertyId
            refs.addReference(PropertyId.valueOf(parts[i + 1]));    
        }

		return refs;
	}

	/**
	 * Persists the <code>NodeState</code> object.
	 *
	 * @param state the <code>NodeState</code> object
	 */
	protected void store(NodeState state) throws ItemStateException {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		String pid;
		String nodeID = state.getNodeId().toString();
		String nodeType = state.getNodeTypeName().toString();
		
		if (nodeID.equals(JR_SYSTEM_ID)) {
			// do not store system node in Fedora
			return;
		}
		if (nodeID.equals(JR_ROOT_ID)) {
			// root node
			// child nodes (list of name/uuid pairs)
			Collection c = state.getChildNodeEntries();
			for (Iterator iter = c.iterator(); iter.hasNext();) {
				ChildNodeEntry entry = (ChildNodeEntry) iter.next();
				String uuid = entry.getId().toString();
				String escapedPID = entry.getName().toString();

				putJCRPath(uuid, escapedPID.substring(escapedPID.indexOf("}") 
													  + 1), null);

				// flush if pending
				NodeState st = pendingNodeMap.get(uuid);
				if (st != null) {
					store(st);
					pendingNodeMap.remove(uuid);
				}
			}

			log.debug("storing root node");

			return;
		}

		log.debug("storing node type " + nodeType + 
						   " (" + nodeID + ")");

		// non-root and non-system
		if (// native Fedora objects
			nodeType.equals("{http://www.jcp.org/jcr/nt/1.0}unstructured") ||
			// newly created folder in Sling WebDAV drive
			nodeType.equals("{http://sling.apache.org/jcr/sling/1.0}Folder") ||
			// nt:resource node that is not a child of nt:file
			(nodeType.equals("{http://www.jcp.org/jcr/nt/1.0}resource") &&
			 getJCRPath(nodeID) != null)) {
			// digital object node

			// update data stream map
			// child nodes (list of name/uuid pairs)
			Collection c = state.getChildNodeEntries();
			log.debug("Child nodes: " + c.size()); // count
			for (Iterator iter = c.iterator(); iter.hasNext();) {
				ChildNodeEntry entry = (ChildNodeEntry) iter.next();
				String uuid = entry.getId().toString();
				String dsID = entry.getName().toString();

				log.debug("child " + dsID);
				if (getJCRPath(uuid) == null) {
					if (dsMap.get(uuid) != null) {
						// data stream node
						// associate the ds node with its parent do node
						dsMap.put(uuid, nodeID);

						// create a data stream
						if (DataStream.getDSFromUUID(uuid) == null) {
							DataStream dataStream = new DataStream(dsID);
							dataStream.setUUID(uuid);
						}
					}
				}
			}

			pid = getPID(nodeID);

			log.debug("pid: " + pid);

			if (pid == null) {
				// not in map yet, add to the pending node list
				pendingNodeMap.put(nodeID, state);
			
				return;
			}

			// persist (the digital object) to Fedora repository if it does
			// not exist already
			try {
				if (! fc.existsObject(pid)) {
					fc.createObject(pid);
				}
			} catch (Exception e) {
				String msg = "error connecting to the Fedora server";
				log.error(msg);
				throw new ItemStateException(msg, e);
			}

			// flush its children (DS nodes)
			for (Iterator iter = c.iterator(); iter.hasNext();) {
				ChildNodeEntry entry = (ChildNodeEntry) iter.next();
				String uuid = entry.getId().toString();
				String childID = entry.getName().toString();
				NodeState st = pendingNodeMap.get(uuid);


				if (getJCRPath(uuid) == null && dsMap.get(uuid) == null) {
					// child do node
					putJCRPath(uuid, childID.substring(childID.indexOf("}") + 1),
							   nodeID);
				}

				if (st != null) {
					store(st);
					pendingNodeMap.remove(uuid);
				}

				String cpid = getPID(uuid);

				if (cpid != null) {
					// also a digital object
					// persist (the digital object) to Fedora repository if it does
					// not exist already
					try {
						if (! fc.existsObject(cpid)) {
							fc.createObject(cpid);
						}
					} catch (Exception e) {
						String msg = "error connecting to the Fedora server";
						log.error(msg);
						throw new ItemStateException(msg, e);
					}

					log.debug("add relationship " + pid + ", " + cpid);
					fc.addMember(pid, cpid);
				}
			}

			// mixin types
			c = state.getMixinTypeNames();
			String storedString = c.size() + ""; // count
			for (Iterator iter = c.iterator(); iter.hasNext();) {
				storedString += "%57" + iter.next().toString();   // name
			}

			// URI of the property
			String propertyURI = "http://sling.apache.org/jcr/sling/1.0/MixinTypes";
			if (fc.existsProperty(pid, propertyURI)) {
				fc.deleteProperty(pid, propertyURI);
			}

			fc.addProperty(pid, propertyURI, storedString);

			// flush its children (DO properties)
			for (Iterator<PropertyState> it = pendingProperties.iterator();
				 it.hasNext();) {
				PropertyState st = it.next();
				if (st.getParentId().toString().equals(nodeID)) {
					store(st);
					it.remove();
				}
			}
		}
		else if (nodeType.equals("{http://www.jcp.org/jcr/nt/1.0}file")) {
			// data stream node

			// update content map
			// child nodes (list of name/uuid pairs)
			Collection c = state.getChildNodeEntries();
			// System.out.println("Child nodes: " + c.size()); // count
			for (Iterator iter = c.iterator(); iter.hasNext();) {
				ChildNodeEntry entry = (ChildNodeEntry) iter.next();
				String uuid = entry.getId().toString();

				// associate the content node with its parent ds node
				if (contentMap.get(uuid) == null) {
					contentMap.put(uuid, nodeID);
				}
			}

			log.debug("nt:file node: " + nodeID);

			if (dsMap.get(nodeID) == null) {
				// not hooked up with its parent yet
				dsMap.put(nodeID, "");

				if (pendingNodeMap.get(nodeID) == null) {
					pendingNodeMap.put(nodeID, state);
					
					return;
				}
			}

			// flush its children (jcr:content nodes)
			for (Iterator iter = c.iterator(); iter.hasNext();) {
				ChildNodeEntry entry = (ChildNodeEntry) iter.next();
				String uuid = entry.getId().toString();
				NodeState st = pendingNodeMap.get(uuid);
				if (st != null) {
					store(st);
					pendingNodeMap.remove(uuid);
				}
			}

			// flush its children (nt:file properties)
			for (Iterator<PropertyState> it = pendingProperties.iterator();
				 it.hasNext();) {
				PropertyState st = it.next();
				if (st.getParentId().toString().equals(nodeID)) {
					// simply remove it since no nt:file property is supported
					it.remove();
				}
			}
		}
		else if (nodeType.equals("{http://www.jcp.org/jcr/nt/1.0}resource")) {
			// jcr:content node, do not persist 
			// (only the jcr:data property is persisted)

			if (contentMap.get(nodeID) == null ||
				dsMap.get(contentMap.get(nodeID)).equals("")) {
				// not hooked up with its parent yet
				if (pendingNodeMap.get(nodeID) == null) {
					pendingNodeMap.put(nodeID, state);
				}					
					return;
					// }
			}

			// flush its children (jcr:data properties)
			for (Iterator<PropertyState> it = pendingProperties.iterator();
				 it.hasNext();) {
				PropertyState st = it.next();
				log.debug("flushing: " + st.getId().toString());
				log.debug("DS node ID: " + contentMap.get(nodeID));
				log.debug("DO node ID: " + dsMap.get(contentMap.get(nodeID)));
				if (st.getParentId().toString().equals(nodeID)) {
					log.debug("Storing pending property: " + 
									   st.getId().toString());
					store(st);
					
					it.remove();
				}
			}
		}
	}

	/**
	 * Persists the <code>PropertyState</code> object.
	 *
	 * @param state the <code>PropertyState</code> object
	 */
	protected void store(PropertyState state) throws ItemStateException {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		int index;
		String nodeID, propertyName, pid;
		String dsID;
		String mimeType;
		String propertyURI;
		String storedString;
		InternalValue[] values = state.getValues();
		PropertyId id = state.getPropertyId();
		
		index = id.toString().indexOf("/");
		nodeID = id.toString().substring(0, index);
		// pid = uuidMap.get(nodeID);
		propertyName = id.toString().substring(index + 1);

		log.debug("storing property: " + propertyName);

		if (values.length < 1) {
			return;
		}

		if (nodeID.equals(JR_ROOT_ID)) {
			// cannot store any property of root node
			return;
		}

		if (getJCRPath(nodeID) != null) {
			// digital object node
			pid = getPID(nodeID);
			log.debug("pid: " + pid);
			propertyURI = getPropertyURI(propertyName);
			if (fc.existsProperty(pid, propertyURI)) {
				fc.deleteProperty(pid, propertyURI);
			}

			if (propertyURI.contains("http://purl.org/dc/elements/1.1")) {
				// Dublin Core properties
				index = propertyURI.lastIndexOf("/");
				fc.modifyDCField(pid, propertyURI.substring(index + 1), values[0].toString());
				return;
			}

			// type and number of values
			storedString = state.getType() + "%57" + values.length;
			// definitionID
			storedString += "%57"; // + state.getDefinitionId().toString();
			// modCount
			storedString += "%57" + state.getModCount();

			// actual values
			for (int i = 0; i < values.length; ++i) {
				storedString += "%57" + values[i].toString();
			}

			fc.addProperty(pid, propertyURI, storedString);
			return;
		}

		if (contentMap.get(nodeID) == null &&
			dsMap.get(nodeID) == null) {
			pendingProperties.add(state);
			return;
		}

		if (dsMap.get(nodeID) != null) {
			// nt:file (data stream) node
			return;
		}

		// jcr:content node
		pid = getPID(dsMap.get(contentMap.get(nodeID)));

		log.debug("pid: " + pid);

		// non-root and non-system
		if (pid == null) {
			// not in map yet
			log.debug("should not happen");
			pendingProperties.add(state);
			
			return;
		}

		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding")) {

		}
		else if (propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType") ) {
			DataStream.getDSFromUUID(contentMap.get(nodeID)).setMIMEType(values[0].toString());
		}
		else if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data")) {
			String tmpFile = "fedora-upload-";
			// BLOBFileValue blobVal = values[0].getBLOBFileValue();
			try {
				InputStream in = values[0].getStream(); // blobVal.getStream();
				File f = new File(tmpFile);
				OutputStream out = new FileOutputStream(f);
				byte buf[] = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				out.close();
				in.close();

				dsID = DataStream.getDSFromUUID(contentMap.get(nodeID)).id;
				dsID = dsID.substring(dsID.indexOf("}") + 1);

				if (dsID.startsWith("._")) {
					// ignore
					log.debug("ignore data stream " + dsID);
					return;
				}

				dsID = unescapeDSID(dsID);
				mimeType = 
					DataStream.getDSFromUUID(contentMap.get(nodeID)).mimeType;

				log.debug("adding data stream " + dsID + " of MIME type " + mimeType);

				if (fc.existsDataStream(pid, dsID)) {
					// do not overwrite existing data stream
					// return;
					log.debug("deleting data stream: " + dsID);
					fc.deleteDataStream(pid, dsID);
				}

				fc.addDataStream(pid, 
								 dsID,
								 mimeType,
								 // values[0].toString().getBytes());
								 tmpFile);
			} catch (Exception e) {
				String msg = "error adding data stream";
				log.error(msg);
				throw new ItemStateException(msg, e);
			}
                
		}
		else {
			// not supported
		}
	}

	/**
	 * Persists the <code>NodeReferences</code> object.
	 *
	 * @param refs the <code>NodeReferences</code> object
	 */
	protected void store(NodeReferences refs) throws ItemStateException {
		String pid, nodeID;
		String propertyURI;
		String storedString;
		Collection c;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// target node ID
		nodeID = refs.getTargetId().toString();
		// pid of the digital object
		pid = getPID(nodeID);
		// URI of the property
		propertyURI = "http://sling.apache.org/jcr/sling/1.0/NodeReferences";
		if (fc.existsProperty(pid, propertyURI)) {
			fc.deleteProperty(pid, propertyURI);
		}

        // references
        c = refs.getReferences();

		// count
		storedString = c.size() + "";

        for (Iterator iter = c.iterator(); iter.hasNext();) {
            PropertyId propId = (PropertyId) iter.next();
            storedString += "%57" + propId.toString();   // propertyId
        }

		fc.addProperty(pid, propertyURI, storedString);
	}

	/**
	 * Destroys a node.
	 *
	 * @param state the <code>NodeState</code> object
	 */
	protected void destroy(NodeState state) throws ItemStateException {
		String pid;
		String nodeID = state.getNodeId().toString();
		String nodeType = state.getNodeTypeName().toString();
		
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented
		log.debug("destroying node: " + nodeID);

		// non-root and non-system
		if (nodeType.equals("{http://www.jcp.org/jcr/nt/1.0}unstructured") ||
			// newly created folder in Sling WebDAV drive
			nodeType.equals("{http://sling.apache.org/jcr/sling/1.0}Folder")) {
			// digital object node

			pid = getPID(nodeID);

			if (pid == null) {
				// should not happen
			
				return;
			}

			// deal with "untitled folder" in WebDAV drive
			if (pid.indexOf(":") < 0) {
				pid = "sling:" + pid;
			}

			// delete from Fedora repository if it exists
			if (fc.existsObject(pid)) {
				log.debug("deleting digital object: " + pid);
				fc.deleteObject(pid);
			}
		}
		else if (nodeType.equals("{http://www.jcp.org/jcr/nt/1.0}file")) {
			// data stream node
		}
		else if (nodeType.equals("{http://www.jcp.org/jcr/nt/1.0}resource")) {
			// jcr:content node
			// (only the jcr:data property is deleted)
		}
	}

	/**
	 * Destroys a property.
	 *
	 * @param state the <code>PropertyState</code> object
	 */
	protected void destroy(PropertyState state) throws ItemStateException {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		int index;
		String nodeID, propertyName, pid;
		String dsID;
		String mimeType;
		PropertyId id = state.getPropertyId();
		String propertyURI;

		index = id.toString().indexOf("/");
		nodeID = id.toString().substring(0, index);
		// pid = uuidMap.get(nodeID);
		propertyName = id.toString().substring(index + 1);

		log.debug("destroying property: " + propertyName);

		if (getJCRPath(nodeID) != null) {
			// digital object node
			pid = getPID(nodeID);
			propertyURI = getPropertyURI(propertyName);
			fc.deleteProperty(pid, propertyURI);
			return;
		}

		if (dsMap.get(nodeID) != null) {
			// nt:file (data stream node)
			return;
		}

		// jcr:content node
		pid = getPID(dsMap.get(contentMap.get(nodeID)));

		// non-root and non-system
		if (pid == null) {
			// not in map yet
			// should not happen
			// pendingProperties.add(state);
			log.debug("null pid!!!");
			
			return;
		}

		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data")) {
			dsID = DataStream.getDSFromUUID(contentMap.get(nodeID)).id;
			dsID = dsID.substring(dsID.indexOf("}") + 1);

			if (dsID.startsWith("._")) {
				// ignore
				log.debug("ignore data stream " + dsID);
				return;
			}

			dsID = dsID.replaceAll("\\.", "");

			if (dsID.equals("DC") || dsID.equals("RELS-EXT")) {
				// cannot delete DC data stream
				// do not delete RELS-EXT stream to avoid losing properties
				return;
			}

			try {
				if (fc.existsDataStream(pid, dsID)) {
					log.debug("deleting data stream: " + dsID);
					fc.deleteDataStream(pid, dsID);
				}
			} catch (Exception e) {
				log.error("error deleting data stream " + dsID, e);
			}
		}
	}

	/**
	 * Destroys a node reference.
	 *
	 * @param refs the <code>NodeReferences</code> object
	 */
	protected void destroy(NodeReferences refs) throws ItemStateException {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented
	}

	/**
	 * Tests if a property exists.
	 *
	 * @param id the <code>PropertyId</code> object that represents the property
	 * @return whether the property exists
	 */
	public synchronized boolean exists(PropertyId id) throws ItemStateException
	{
		int index;
		String nodeID, propertyName;
		String pid;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		log.debug("check property existence: " + id.toString());

		index = id.toString().indexOf("/");
		nodeID = id.toString().substring(0, index);
		propertyName = id.toString().substring(index + 1);

		if (propertyName.equals("{http://sling.apache.org/jcr/sling/1.0}NodeReferences") ||
			propertyName.equals("NodeReferences")) {
			// reserved for references
			return false;
		}

		if (contentMap.get(nodeID) != null) {
			// jcr:content node
			if ((propertyName.equals("{http://www.jcp.org/jcr/1.0}data") ||
				 propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding") ||
				 propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType"))) {
				return true;
			}
			else {
				return false;
			}
		}
		if (dsMap.get(nodeID) != null) {
			// data stream node
			if (propertyName.equals("{http://www.jcp.org/jcr/1.0}primaryType")) {
				return true;
			}
			else {
				return false;
			}
		}
		if (getJCRPath(nodeID) != null) {
			// digital object node
			pid = getPID(nodeID);

			if (propertyName.equals("{http://www.jcp.org/jcr/1.0}primaryType")) {
				return true;
			}
			
			if (fc.existsProperty(pid, getPropertyURI(propertyName))) {
				return true;
			}
		}

		if (nodeID.equals(JR_ROOT_ID) &&
			propertyName.equals("{http://www.jcp.org/jcr/1.0}primaryType")) {
			return true;
		}

		return false;
	}

	/**
	 * Tests if a node exists.
	 *
	 * @param id the <code>NodeId</code> object that represents the node
	 * @return whether the nodes exists
	 */
	public synchronized boolean exists(NodeId id) throws ItemStateException {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		log.info("check node existence: " + id.toString());

		if (id.toString().equals(JR_ROOT_ID) ||
			id.toString().equals(JR_SYSTEM_ID) ) {
			return true;
		}

		if (getPID(id.toString()) != null) {
			return true;
		}
		else if (dsMap.get(id.toString()) != null) {
			return true;
		}
		else if (contentMap.get(id.toString()) != null) {
			return true;
		}
		else {
			return false;
		}
	}

    /**
     * {@inheritDoc}
     */
    public synchronized boolean existsReferencesTo(NodeId id)
			throws ItemStateException {
		String nodeID, pid;
		String propertyURI;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// target node ID
		nodeID = id.toString();
		// pid of the digital object
		pid = getPID(nodeID);
		// URI of the property
		propertyURI = "http://sling.apache.org/jcr/sling/1.0/NodeReferences";
		
		if (fc.existsProperty(pid, propertyURI)) {
			return true;
		}
		else {
			return false;
		}
	}
}
