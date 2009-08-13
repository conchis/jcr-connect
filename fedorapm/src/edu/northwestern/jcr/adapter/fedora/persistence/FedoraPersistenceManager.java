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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.persistence.AbstractPersistenceManager;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;

// new
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;

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
 * <code>FedoraPersistenceManager</code> is a Fedora-based
 * <code>PersistenceManager</code> that persists <code>ItemState</code>
 * and <code>NodeReferences</code> objects using Fedora API-A and API-M.
 *
 * @author Xin Xiang
 */
public class FedoraPersistenceManager extends AbstractPersistenceManager {

	private static Logger log = 
		LoggerFactory.getLogger(FedoraPersistenceManager.class);

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
	public FedoraPersistenceManager() {
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

		if (useREST) {
			fc = new FedoraConnectorREST();
		}
		else {
			fc = new FedoraConnectorAPIX();
		}
	}


	// ---------------------------------< PersistenceManager >
	/**
	 * {@inheritDoc}
	 */
	public void init(PMContext context) throws Exception {
		if (initialized) {
			throw new IllegalStateException("already initialized");
		}

		// to be implemented

		initialized = true;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void close() throws Exception {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented		
	}

	/**
	 * Replace ":" with "_" in Fedora pid
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
	 * Restore Fedora pid
	 */
	private String unescapePID(String id)
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
	 * Convert JCR path name to Fedora pid for sling objects
	 * return xxxxx in sling:xxxxx
	 */
	public static String unescapePIDSling(String id)
	{
		String pid;

		pid = id.replaceAll("_", "__");
		pid = pid.replaceAll("\\s+", "_");

		return pid;
	}

	private String getPID(String nodeID)
	{
		String pid;

		pid = getJCRPath(nodeID);

		if (pid == null) {
			return null;
		}

		System.out.println("escaped pid: " + pid);

		// recover Fedora PID from escaped relative path
		// if (slingNodeList.contains(nodeID) || ! pid.contains("_")) {
			// must be a sling folder if the name does not contain underscore
		if (! fedoraNodeList.contains(nodeID)) {
			// pid = uuidMap.get(nodeID).replace("/", "%57");
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
	 * Recover JCR path from Fedora data stream ID
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
		return id.substring(2);
	}

	/**
	 * Replace "." in JCR path with "_" to form a valid Fedora data stream ID
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
	 * Associate the relative path of a node with its uuid
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
	 * Get the relative path of a node given its uuid
	 */
	private String getJCRPath(String uuid)
	{
		String fullPath;

		fullPath = uuidMap.get(uuid);

		if (fullPath == null) {
			return null;
		}

		System.out.println("uuid: " + uuid);
		System.out.println("full path: " + fullPath);

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
	 * Get the UUID of the child given its name
	 */
	private String getChildUUID(String name)
	{
		return childrenMap.get(name);
	}

	/**
	 * Clean the HashMap.
	 */
	private void cleanChildrenMap()
	{
		childrenMap.clear();
	}

	/**
	 * Get the level of a node in JCR hieararchy given its uuid
	 */
	private int getJCRLevel(String uuid)
	{
		String fullPath;

		fullPath = uuidMap.get(uuid);

		return fullPath.split("/").length - 1;
	}

	/**
	 * Get parent node ID of a digital object node
	 */
	private String getParentID(String id)
	{
		return doMap.get(id);
	}

	/**
	 * Test if the digital object represents a directory created in Sling
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
	 * Translate property URI to internal property name, for example,
	 * {http://www.jcp.org/jcr/1.0}data
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
	 * Translate internal property name to property URI, for example,
	 * http://www.jcp.org/jcr/1.0/data
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
	 * Translate internal property type 
	 * {http://www.jcp.org/jcr/nt/1.0}unstructured
	 * to JCR property type
	 * nt:unstructured
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
	 * Set the node state of nodes representing Fedora digital object.
	 */
	private void setDONodeState(NodeState state)
	{
		UUID uuid;
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
		state.setParentId(new NodeId(new UUID(getParentID(nodeID))));
		state.setDefinitionId(NodeDefId.valueOf("-1603354723"));
		state.setModCount((short) 0);			
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));

		System.out.println("list datastreams of " + pid);
		dsList = fc.listDataStreams(pid);

		if (dsList == null) {
			// policy???
			System.out.println("no datastream!");
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
				uuid = new UUID(map.get(ds.id));
			}
			else {
				uuid = UUID.randomUUID();
			}

			// associate the UUID with the data stream object
			if (map.get(ds.id) == null) {
				ds.setUUID(uuid.toString());
				map.put(ds.id, uuid.toString());
			}
			
			// link to its parent (do) ID
			dsMap.put(uuid.toString(), nodeID);

			// name
			name = NameFactoryImpl.getInstance().create("{}" + 
														escapeDSID(ds.id));
			// uuid
			state.addChildNodeEntry(name, new NodeId(uuid));
		}

		System.out.println("list members of " + pid);
		memberList = fc.listMembers(pid);

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
					// int index = propertyValue.indexOf("%57");
					// if (index >= 0) {
					// 	propertyValue = propertyValue.substring(index + 3);
					// }
							
					uuid = new UUID(propertyValue);
				}
				else if (getChildUUID(escapePID(memberPID)) != null) {
					uuid = new UUID(getChildUUID(escapePID(memberPID)));
				}
				else {
					uuid = UUID.randomUUID();
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

				System.out.println("adding child node: " + escapedPID);

				// name
				name = NameFactoryImpl.getInstance().create("{" + namespaceURI +
															"}" + escapedPID);
				// uuid
				state.addChildNodeEntry(name, new NodeId(uuid));
			}

			cleanChildrenMap();
		}

		System.out.println("list properties of " + pid);
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
				
				String s = fc.getProperty(pid, 
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
	 * Set the node state of nodes representing Fedora data streams.
	 */
	private void setDSNodeState(NodeState state, String parentID)
	{
		UUID uuid = null;
		Name name;
		String id;

		// of node type nt:file
		state.setNodeTypeName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/nt/1.0}file"));
		try {
			state.setParentId(new NodeId(new UUID(parentID)));
		} catch (Exception e) {
			System.err.println("parentID: " + parentID);
		}

		state.setDefinitionId(NodeDefId.valueOf("-1603354723"));
		state.setModCount((short) 0);	
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));

		id = state.getNodeId().toString();

		// add a child node of jcr:content

		for (String key : contentMap.keySet()) {
			if (contentMap.get(key).equals(id)) {
				uuid = new UUID(key);
				break;
			}
		}

		if (uuid == null) {
			uuid = UUID.randomUUID();
		}

		// link to its parent (DS) ID
		contentMap.put(uuid.toString(), id);

		// name
		name = NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}content");
		// uuid
		state.addChildNodeEntry(name, new NodeId(uuid));
	}

	/**
	 * Set the node state of jcr:content nodes representing content of
	 * Fedora data streams.
	 */
	private void setContentNodeState(NodeState state, String parentID)
	{
		UUID uuid;
		Name name;

		state.setNodeTypeName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/nt/1.0}resource"));
		state.setParentId(new NodeId(new UUID(parentID)));
		state.setDefinitionId(NodeDefId.valueOf("-1603354723"));
		state.setModCount((short) 0);	
		// state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}data"));
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}encoding"));
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}mimeType"));
	}

	/**
	 * {@inheritDoc}
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
		System.out.println("loading node: " + state.getNodeId());
		String nodeID = state.getNodeId().toString();


		// load from pending node if pending
		for (String s : pendingNodeMap.keySet()) {
			NodeState ns = pendingNodeMap.get(s);
			if (ns.getNodeId().toString().equals(id.toString())) {
				state.setNodeTypeName(ns.getNodeTypeName());
				state.setParentId(ns.getParentId());
				state.setDefinitionId(ns.getDefinitionId());
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
			state.setParentId(new NodeId(new UUID(JR_ROOT_ID)));
			state.setDefinitionId(NodeDefId.valueOf("-1971945898"));
			state.setModCount((short) 0);			
			state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));
			// name
			Name name = NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}versionStorage");
			// uuid
			state.addChildNodeEntry(name, new NodeId(new UUID("deadbeef-face-babe-cafe-babecafebabe")));
			name = NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}nodeTypes");
			// uuid
			state.addChildNodeEntry(name, new NodeId(new UUID("deadbeef-cafe-cafe-cafe-babecafebabe")));
		}

		else if (nodeID.equals(JR_ROOT_ID)) {
			// fabricate a root node in momory

			state.setNodeTypeName(NameFactoryImpl.getInstance().create("{internal}root"));
			state.setDefinitionId(NodeDefId.valueOf("-1537436024"));
			state.setModCount((short) 4);			
			state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));
			// name
			Name name = NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}system");
			// uuid
			state.addChildNodeEntry(name, new NodeId(new UUID("deadbeef-cafe-babe-cafe-babecafebabe")));

			// get the list of all first-level objects
			pidList = fc.listObjectsRI();

			if (getJCRPath(nodeID) != null) {
				// create children map
				cleanChildrenMap();
				getChildrenMap(nodeID);
			}

			// put each of the Fedora digital objects as child node of root
			for (String pid : pidList) {
				UUID uuid;

				if (fc.existsProperty(pid, 
									  "http://www.jcp.org/jcr/1.0/uuid")) {
					// use the UUID in the property jcr:uuid

					String propertyValue = fc.getProperty(pid, 
														  "http://www.jcp.org/jcr/1.0/uuid");
					String [] parts = propertyValue.split("%57");
					// type, number of values, definitionID, modCount
					propertyValue = parts[4];
					// int index = propertyValue.indexOf("%57");
					// if (index >= 0) {
					// 	propertyValue = propertyValue.substring(index + 3);
					// }

					uuid = new UUID(propertyValue);
				}
				else if (getChildUUID(escapePID(pid)) != null) {
					// make sure the UUID stays the same over multiple loading
					uuid = new UUID(getChildUUID(escapePID(pid)));
				}
				else {
					uuid = UUID.randomUUID();
				}

				// add to sling node list
				// if (isSlingNode(pid) &&
				// 	! slingNodeList.contains(uuid.toString())) {
				// 	slingNodeList.add(uuid.toString());
				// }

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
				System.out.println("JCR path: " + path);
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
		
		System.out.println("node loaded");
		
		return state;
	}

	/**
	 * {@inheritDoc}
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

		System.out.println("loading property: " + id.toString());

		state = createNew(id);

		// populate member variables
		state.setType(1);
		state.setMultiValued(false);
		state.setDefinitionId(PropDefId.valueOf("806470580"));
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
					BLOBFileValue blobVal = v[0].getBLOBFileValue();
					try {
						InputStream in = blobVal.getStream();
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
			values[0] = InternalValue.valueOf("rep:root", 1);
			state.setValues(values);

			System.out.println("property loaded");
			
			return state;
		}


		if (getJCRPath(nodeID) != null) {
			// digital object node
			pid = getPID(nodeID);
			s = fc.getProperty(pid, getPropertyURI(propertyName));

			if (propertyName.equals("{http://www.jcp.org/jcr/1.0}primaryType")) {
				s = getPropertyType(s);
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
			state.setDefinitionId(PropDefId.valueOf(parts[2]));
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

			System.out.println("property loaded");
			
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
			System.out.println("");
			System.out.println("property: " + propertyName);
			System.out.println("DO node: " + nodeID);
			System.out.println("DS node: " + dsNodeID);
			System.out.println("");

		}		

		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data")) {
			dsID = dataStream.id;
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
				values[0] = InternalValue.valueOf("{internal}root", 1);
			}
			else if (getJCRPath(nodeID) != null) {
				// digital object node
				values[0] = InternalValue.valueOf("{http://www.jcp.org/jcr/nt/1.0}unstructured", 1);
			}
			else if (dsMap.get(nodeID) != null) {
				// data stream node
				values[0] = InternalValue.valueOf("{http://www.jcp.org/jcr/nt/1.0}file", 1);
			}
			else if (contentMap.get(nodeID) != null) {
				// content node
				values[0] = InternalValue.valueOf("{http://www.jcp.org/jcr/nt/1.0}resource", 1);
			}
		}

		state.setValues(values);

		System.out.println("property loaded");

		return state;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized NodeReferences load(NodeReferencesId id)
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
		parts = s.split("%57");
		count = Integer.parseInt(parts[0]);
        for (int i = 0; i < count; i++) {
			// propertyId
            refs.addReference(PropertyId.valueOf(parts[i + 1]));    
        }

		return refs;
	}

	// private BufferedWriter getWriter(String nodeID)
	// {
	// 	return null;
	// }

	/**
	 * {@inheritDoc}
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
				// if (slingNodeList.contains(uuid) ||
				// 	// must be a sling folder if the name contains space
				// 	escapedPID.contains(" ")) {
				// if (! fedoraNodeList.contains(uuid)) {
				// 	pid = unescapePIDSling(escapedPID);
				// }
				// else {
				// 	pid = unescapePID(escapedPID);
				// }

				// putJCRPath(uuid, pid.substring(pid.indexOf("}") + 1), null);
				putJCRPath(uuid, escapedPID.substring(escapedPID.indexOf("}") 
													  + 1), null);

				// flush if pending
				NodeState st = pendingNodeMap.get(uuid);
				if (st != null) {
					store(st);
					pendingNodeMap.remove(uuid);
				}
			}

			System.out.println("storing root node");

			return;
		}

		System.out.println("storing node type " + nodeType + 
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
			System.out.println("Child nodes: " + c.size()); // count
			for (Iterator iter = c.iterator(); iter.hasNext();) {
				ChildNodeEntry entry = (ChildNodeEntry) iter.next();
				String uuid = entry.getId().toString();
				String dsID = entry.getName().toString();

				System.out.println("child " + dsID);
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

			System.out.println("pid: " + pid);

			if (pid == null) {
				// not in map yet, add to the pending node list
				pendingNodeMap.put(nodeID, state);
			
				return;
			}

			// persist (the digital object) to Fedora repository if it does
			// not exist already
			if (! fc.existsObject(pid)) {
				fc.createObject(pid);
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
					if (! fc.existsObject(cpid)) {
						fc.createObject(cpid);
					}

					System.out.println("add relationship " + pid + ", " + cpid);
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

			System.out.println("nt:file node: " + nodeID);

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
				System.out.println("flushing: " + st.getId().toString());
				System.out.println("DS node ID: " + contentMap.get(nodeID));
				System.out.println("DO node ID: " + dsMap.get(contentMap.get(nodeID)));
				if (st.getParentId().toString().equals(nodeID)) {
					System.out.println("Storing pending property: " + 
									   st.getId().toString());
					store(st);
					
					// try {
					it.remove();
					// } catch (Exception e) {
					// 	System.err.println("\n");
					// 	System.err.println("Exception thrown while storing " +
					// 					   st.getId().toString());
					// 	System.err.println("\n");
					// }
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
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

		// if (propertyName.equals("{http://www.jcp.org/jcr/1.0}mixinTypes")
		// 	// propertyName.equals("{http://www.jcp.org/jcr/1.0}lockOwner") ||
		// 	// propertyName.equals("{http://www.jcp.org/jcr/1.0}lockIsDeep")
		// 	) {
		// 	// ignore mixinTypes
		// 	return;
		// }

		System.out.println("storing property: " + propertyName);

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
			System.out.println("pid: " + pid);
			propertyURI = getPropertyURI(propertyName);
			if (fc.existsProperty(pid, propertyURI)) {
				fc.deleteProperty(pid, propertyURI);
			}

			// type and number of values
			storedString = state.getType() + "%57" + values.length;
			// definitionID
			storedString += "%57" + state.getDefinitionId().toString();
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

		// if (!propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding") &&
		// 	!propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType") &&
		// 	!propertyName.equals("{http://www.jcp.org/jcr/1.0}data") &&
		// 	!propertyName.equals("{http://www.jcp.org/jcr/1.0}lastModified")) {
		// 	// ignore other properties
		// 	System.out.println("ignoring property: " + propertyName);
		// 	return;
		// }

		if (dsMap.get(nodeID) != null) {
			// nt:file (data stream) node
			return;
		}

		// jcr:content node
		pid = getPID(dsMap.get(contentMap.get(nodeID)));

		System.out.println("pid: " + pid);

		// non-root and non-system
		if (pid == null) {
			// not in map yet
			System.out.println("should not happen");
			pendingProperties.add(state);
			
			return;
		}

		// if (pid.indexOf(":") < 0) {
		// 	pid = "sling:" + pid;
		// }

		// if (propertyName.equals("{}DC")) {
		// 	System.out.println("updating DC stream in " + pid);
		// 
		// 	fc.modifyDCDataStream(pid, values[0].toString().getBytes());
		// }
		// else 
		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding")) {

		}
		else if (propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType") ) {
			DataStream.getDSFromUUID(contentMap.get(nodeID)).setMIMEType(values[0].toString());
		}
		else if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data")) {
			String tmpFile = "fedora-upload-";
			BLOBFileValue blobVal = values[0].getBLOBFileValue();
			try {
				InputStream in = blobVal.getStream();
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
					System.out.println("ignore data stream " + dsID);
					return;
				}

				if (pid.equals("sling2:-1265638226%57canberra")) {
					System.out.println("/////////////////////////////////////");
					System.out.println("/////////////////////////////////////");
					System.out.println("DS ID: " + dsID);
					System.out.println("node ID: " + nodeID);
					System.out.println("DS node ID: " + contentMap.get(nodeID));
					System.out.println("/////////////////////////////////////");
					System.out.println("/////////////////////////////////////");
				}

				System.out.println("adding data stream " + dsID);

				dsID = unescapeDSID(dsID);
				mimeType = 
					DataStream.getDSFromUUID(contentMap.get(nodeID)).mimeType;

				if (fc.existsDataStream(pid, dsID)) {
					// do not overwrite existing data stream
					// return;
					System.out.println("deleting data stream: " + dsID);
					fc.deleteDataStream(pid, dsID);
				}

				// fc.addDataStream(pid, propertyName, 
				fc.addDataStream(pid, 
								 dsID,
								 mimeType,
								 // values[0].toString().getBytes());
								 tmpFile);
			} catch (Exception e) {

			}
                
		}
		else {
			// not supported
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void store(NodeReferences refs) throws ItemStateException {
		String pid, nodeID;
		String propertyURI;
		String storedString;
		Collection c;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		System.out.println("//////////////////////////////");
		System.out.println(refs.getId());
		System.out.println("//////////////////////////////");

		// target node ID
		nodeID = refs.getId().toString();
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
	 * {@inheritDoc}
	 */
	protected void destroy(NodeState state) throws ItemStateException {
		String pid;
		String nodeID = state.getNodeId().toString();
		String nodeType = state.getNodeTypeName().toString();
		
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented
		System.out.println("destroying node: " + nodeID);

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
				System.out.println("deleting digital object: " + pid);
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
	 * {@inheritDoc}
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

		// if (!propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding") &&
		// 	!propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType") &&
		// 	!propertyName.equals("{http://www.jcp.org/jcr/1.0}data") &&
		// 	!propertyName.equals("{http://www.jcp.org/jcr/1.0}lastModified")) {
		// 	// ignore other properties
		// 	System.out.println("ignoring property: " + propertyName);
		// 	return;
		// }

		// if (propertyName.equals("{http://www.jcp.org/jcr/1.0}mixinTypes")
		// 	// propertyName.equals("{http://www.jcp.org/jcr/1.0}lockOwner") ||
		// 	// propertyName.equals("{http://www.jcp.org/jcr/1.0}lockIsDeep")
		// 	) {
		// 	// ignore mixinTypes
		// 	return;
		// }

		System.out.println("destroying property: " + propertyName);

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
			System.out.println("null pid!!!");
			
			return;
		}

		// if (pid.indexOf(":") < 0) {
		// 	pid = "sling:" + pid;
		// }

		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data")) {
			dsID = DataStream.getDSFromUUID(contentMap.get(nodeID)).id;
			dsID = dsID.substring(dsID.indexOf("}") + 1);

			if (dsID.startsWith("._")) {
				// ignore
				System.out.println("ignore data stream " + dsID);
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
					System.out.println("deleting data stream: " + dsID);
					fc.deleteDataStream(pid, dsID);
				}
			} catch (Exception e) {

			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected void destroy(NodeReferences refs) throws ItemStateException {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized boolean exists(PropertyId id) throws ItemStateException
	{
		int index;
		String nodeID, propertyName;
		String pid;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		System.out.println("check property existence: " + id.toString());

		index = id.toString().indexOf("/");
		nodeID = id.toString().substring(0, index);
		propertyName = id.toString().substring(index + 1);

		if (propertyName.equals("{http://sling.apache.org/jcr/sling/1.0}NodeReferences") ||
			propertyName.equals("NodeReferences")) {
			// reserved for references
			return false;
		}

		// if (propertyName.equals("{http://www.jcp.org/jcr/1.0}mixinTypes")
		// 	// propertyName.equals("{http://www.jcp.org/jcr/1.0}lockOwner") ||
		// 	// propertyName.equals("{http://www.jcp.org/jcr/1.0}lockIsDeep")
		// 	) {
		// 	// ignore mixinTypes
		// 	return false;
		// }

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
	 * {@inheritDoc}
	 */
	public synchronized boolean exists(NodeId id) throws ItemStateException {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		System.out.println("check node existence: " + id.toString());

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
	public synchronized boolean exists(NodeReferencesId id)
			throws ItemStateException {
		String nodeID, pid;
		String propertyURI;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented

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
