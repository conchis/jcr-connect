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
import java.util.ArrayList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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

	/** map UUID of the digital object node to fedora PID */
	private static Map<String, String> uuidMap = new HashMap<String, String>();

	/** map data stream node ID to its parent digital object node ID */
	private static Map<String, String> dsMap = new HashMap<String, String>();

	/** map jcr:content node ID to its parent data stream node ID */
	private static Map<String, String> contentMap = 
		new HashMap<String, String>();

	/** list of IDs of nodes reprenting Sling folders */
	private static List<String> slingNodeList = new ArrayList<String>();

	/** fedora client handle */
	private FedoraConnector fc;

	/** list of nodes that are pending to persist */
	private static Map<String, NodeState> pendingNodeMap = 
		new HashMap<String, NodeState>(); 
	// = new ArrayList<NodeState>();

	/** list of properties that are pending to persist */
	private static List<PropertyState> pendingProperties = 
		new ArrayList<PropertyState>();

	/**
	 * Creates a new <code>FedoraPersistenceManager</code> instance.
	 */
	public FedoraPersistenceManager() {
		initialized = false;

		fc = new FedoraConnector();
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
	private String escapePID(String pid)
	{
		String id;

		id = pid.replace("_", "__");
		id = id.replace(":", "_");

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
	 * Convert Fedora pid to JCR path name for sling objects
	 * id is xxxxx in sling:xxxxx
	 */
	private String escapePIDSling(String id)
	{
		String [] parts = id.split("__");
		String pid = "";

		for (int i = 0; i < parts.length; ++i) {
			pid += parts[i].replaceAll("_", " ");

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
	private String unescapePIDSling(String id)
	{
		String pid;

		pid = id.replaceAll("_", "__");
		pid = pid.replaceAll("\\s+", "_");

		return pid;
	}

	/**
	 * Set the node state of nodes representing Fedora digital object.
	 */
	private void setDONodeState(NodeState state, String nodeID)
	{
		UUID uuid;
		String pid;
		Name name;
		DataStream [] dsList;

		state.setNodeTypeName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/nt/1.0}unstructured"));
		state.setParentId(new NodeId(new UUID("cafebabe-cafe-babe-cafe-babecafebabe")));
		state.setDefinitionId(NodeDefId.valueOf("-1603354723"));
		state.setModCount((short) 0);			
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));

		pid = uuidMap.get(nodeID);

		if (pid == null) {
			return;
		}

		if (pid.indexOf(":") < 0) {
			pid = "sling:" + pid;
		}

		System.out.println("list datastreams of " + pid);
		dsList = fc.listDataStreams(pid);
		
		if (dsList == null) {
			return;
		}
		
		for (DataStream ds : dsList) {
			// add a child node of DC
			uuid = UUID.randomUUID();

			// associate the UUID with the data stream object
			ds.setUUID(uuid.toString());
			
			// link to its parent (do) ID
			dsMap.put(uuid.toString(), nodeID);

			// name
			name = NameFactoryImpl.getInstance().create("{}" + ds.id);
			// uuid
			state.addChildNodeEntry(name, new NodeId(uuid));
		}
	}

	/**
	 * Set the node state of nodes representing Fedora data streams.
	 */
	private void setDSNodeState(NodeState state, String id, String parentID)
	{
		UUID uuid;
		Name name;

		// of node type nt:file
		state.setNodeTypeName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/nt/1.0}file"));
		state.setParentId(new NodeId(new UUID(parentID)));
		state.setDefinitionId(NodeDefId.valueOf("-1603354723"));
		state.setModCount((short) 0);	
		state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));

		// add a child node of jcr:content
		uuid = UUID.randomUUID();

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
	private void setContentNodeState(NodeState state, 
									 String id, String parentID)
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

		if (nodeID.equals("deadbeef-cafe-babe-cafe-babecafebabe")) {
			// fabricate a system node in momory
			// System.out.println("system node!");

			state.setNodeTypeName(NameFactoryImpl.getInstance().create("{internal}system"));
			state.setParentId(new NodeId(new UUID("cafebabe-cafe-babe-cafe-babecafebabe")));
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

		else if (nodeID.equals("cafebabe-cafe-babe-cafe-babecafebabe")) {
			// fabricate a root node in momory
			// System.out.println("root node!");

			state.setNodeTypeName(NameFactoryImpl.getInstance().create("{internal}root"));
			state.setDefinitionId(NodeDefId.valueOf("-1537436024"));
			state.setModCount((short) 4);			
			state.addPropertyName(NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}primaryType"));
			// name
			Name name = NameFactoryImpl.getInstance().create("{http://www.jcp.org/jcr/1.0}system");
			// uuid
			state.addChildNodeEntry(name, new NodeId(new UUID("deadbeef-cafe-babe-cafe-babecafebabe")));

			// get the list of all objects
			pidList = fc.listObjects();

			// put each of the Fedora digital objects as child node of root
			for (String pid : pidList) {
				UUID uuid = UUID.randomUUID();

				if (pid.startsWith("sling:")) {
					// distinguish objects created in sling with other objects
					// sling:untitled_folder
					pid = pid.substring(6);	// untitled_folder
					// put the part without sling: prefix
					uuidMap.put(uuid.toString(), pid);
					pid = escapePIDSling(pid); // untitled folder

					// add to sling node list
					if (! slingNodeList.contains(uuid.toString())) {
						slingNodeList.add(uuid.toString());
					}
				}
				else {
					// put orignial pid to access datastreams
					uuidMap.put(uuid.toString(), pid);

					// escape :
					pid = escapePID(pid);
				}

				// name
				name = NameFactoryImpl.getInstance().create("{}" + pid);
				// uuid
				state.addChildNodeEntry(name, new NodeId(uuid));
			}
		}
		else {
			// regular nodes (non-system and non-root)
			String pid = uuidMap.get(nodeID);
			
			if (pid != null) {
				// a digital object node
				setDONodeState(state, nodeID);
			}
			else {
				// not a digital object node
				doUUID = dsMap.get(nodeID);

				if (doUUID != null) {
					// a data stream node
					setDSNodeState(state, nodeID, doUUID);
				}
				else {
					// not a data stream node
					dsUUID = contentMap.get(nodeID);

					if (dsUUID != null) {
						// a jcr:content:node
						setContentNodeState(state, nodeID, dsUUID);
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

		// data stream node (parent of jcr:content)
		dsNodeID = contentMap.get(nodeID);

		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data") ||
			propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding") ||
			propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType") ) {
			// digital object node (parent of data stream node)
			nodeID = dsMap.get(dsNodeID);
		}

		values = new InternalValue[1];

		dataStream = DataStream.getDSFromUUID(dsNodeID);

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
			pid = uuidMap.get(nodeID);
			if (pid.indexOf(":") < 0) {
				pid = "sling:" + pid;
			}

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
			if (nodeID.equals("cafebabe-cafe-babe-cafe-babecafebabe")) {
				// root
				values[0] = InternalValue.valueOf("{internal}root", 1);
			}
			else if (uuidMap.get(nodeID) != null) {
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

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented

		return null;
	}

	private BufferedWriter getWriter(String nodeID)
	{
		return null;
	}

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
		
		if (nodeID.equals("deadbeef-cafe-babe-cafe-babecafebabe")) {
			// do not store system node in Fedora
			return;
		}
		if (nodeID.equals("cafebabe-cafe-babe-cafe-babecafebabe")) {
			// root node
			// child nodes (list of name/uuid pairs)
			Collection c = state.getChildNodeEntries();
			// System.out.println("Child nodes: " + c.size()); // count
			for (Iterator iter = c.iterator(); iter.hasNext();) {
				ChildNodeEntry entry = (ChildNodeEntry) iter.next();
				String uuid = entry.getId().toString();
				String escapedPID = entry.getName().toString();
				if (slingNodeList.contains(uuid) ||
					// must be a sling folder if the name contains space
					escapedPID.contains(" ")) {
					pid = unescapePIDSling(escapedPID);
				}
				else {
					pid = unescapePID(escapedPID);
				}

				if (uuidMap.get(uuid) == null) {
					uuidMap.put(uuid, pid.substring(pid.indexOf("}") + 1));
				}

				// flush if pending
				NodeState st = pendingNodeMap.get(uuid);
				if (st != null) {
					store(st);
					pendingNodeMap.remove(uuid);
				}
			}

			System.out.println("storing root node");

			// // store all pending nodes - assuming a single-level hieararchy
			// for (NodeState ns : pendingNodes) {
			// 	store(ns);
			// }
			// 
			// pendingNodes.clear();
			// 
			// // store all pending properties
			// for (PropertyState ps : pendingProperties) {
			// 	store(ps);
			// }
			// 
			// pendingProperties.clear();

			return;
		}

		System.out.println("storing node type " + nodeType);

		// non-root and non-system
		if (nodeType.equals("{http://www.jcp.org/jcr/nt/1.0}unstructured") ||
			// newly created folder in Sling WebDAV drive
			nodeType.equals("{http://sling.apache.org/jcr/sling/1.0}Folder")) {
			// digital object node

			// update data stream map
			// child nodes (list of name/uuid pairs)
			Collection c = state.getChildNodeEntries();
			// System.out.println("Child nodes: " + c.size()); // count
			for (Iterator iter = c.iterator(); iter.hasNext();) {
				ChildNodeEntry entry = (ChildNodeEntry) iter.next();
				String uuid = entry.getId().toString();
				String dsID = entry.getName().toString();

				// associate the ds node with its parent do node
				if (dsMap.get(uuid) == null) {
					dsMap.put(uuid, nodeID);
				}

				// create a data stream
				if (DataStream.getDSFromUUID(uuid) == null) {
					DataStream dataStream = new DataStream(dsID);
					dataStream.setUUID(uuid);
				}
			}

			pid = uuidMap.get(nodeID);

			if (pid == null) {
				// not in map yet, add to the pending node list
				pendingNodeMap.put(nodeID, state);
			
				return;
			}

			// deal with "untitled folder" in WebDAV drive
			if (pid.indexOf(":") < 0) {
				pid = "sling:" + pid;
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
				NodeState st = pendingNodeMap.get(uuid);
				if (st != null) {
					store(st);
					pendingNodeMap.remove(uuid);
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

			if (dsMap.get(nodeID) == null) {
				// not hooked up with its parent yet
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
		}
		else if (nodeType.equals("{http://www.jcp.org/jcr/nt/1.0}resource")) {
			// jcr:content node, do not persist 
			// (only the jcr:data property is persisted)

			if (contentMap.get(nodeID) == null) {
				// not hooked up with its parent yet
				if (pendingNodeMap.get(nodeID) == null) {
					pendingNodeMap.put(nodeID, state);
					
					return;
				}
			}

			// flush its children (jcr:data properties)
			for (Iterator<PropertyState> it = pendingProperties.iterator();
				 it.hasNext();) {
				PropertyState st = it.next();
				if (st.getParentId().toString().equals(nodeID)) {
					store(st);
					it.remove();
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
		InternalValue[] values = state.getValues();
		PropertyId id = state.getPropertyId();
		
		index = id.toString().indexOf("/");
		nodeID = id.toString().substring(0, index);
		// pid = uuidMap.get(nodeID);
		propertyName = id.toString().substring(index + 1);
		pid = uuidMap.get(dsMap.get(contentMap.get(nodeID)));

		if (!propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding") &&
			!propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType") &&
			!propertyName.equals("{http://www.jcp.org/jcr/1.0}data") &&
			!propertyName.equals("{http://www.jcp.org/jcr/1.0}lastModified")) {
			// ignore other properties
			System.out.println("ignoring property: " + propertyName);
			return;
		}

		System.out.println("storing property: " + propertyName);
		System.out.println("pid: " + pid);

		// non-root and non-system
		if (pid == null) {
			// not in map yet
			pendingProperties.add(state);
			
			return;
		}

		if (pid.indexOf(":") < 0) {
			pid = "sling:" + pid;
		}

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
			// propertyName = 
			// 	propertyName.substring(propertyName.indexOf("}") + 1);
			// 
			// if (propertyName.equals("primaryType")) {
			// 	return;
			// }
			// 
			// System.out.println("updating " + propertyName + " stream in " + 
			// 				   pid);
			// System.out.println(propertyName + ": " + values[0].toString());

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

				System.out.println("adding data stream " + dsID);

				dsID = dsID.replaceAll("\\.", "");
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
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented
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

			pid = uuidMap.get(nodeID);

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
		
		index = id.toString().indexOf("/");
		nodeID = id.toString().substring(0, index);
		// pid = uuidMap.get(nodeID);
		propertyName = id.toString().substring(index + 1);
		pid = uuidMap.get(dsMap.get(contentMap.get(nodeID)));

		if (!propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding") &&
			!propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType") &&
			!propertyName.equals("{http://www.jcp.org/jcr/1.0}data") &&
			!propertyName.equals("{http://www.jcp.org/jcr/1.0}lastModified")) {
			// ignore other properties
			System.out.println("ignoring property: " + propertyName);
			return;
		}

		System.out.println("destroying property: " + propertyName);

		// non-root and non-system
		if (pid == null) {
			// not in map yet
			// should not happen
			// pendingProperties.add(state);
			System.out.println("null pid!!!");
			
			return;
		}

		if (pid.indexOf(":") < 0) {
			pid = "sling:" + pid;
		}

		if (propertyName.equals("{http://www.jcp.org/jcr/1.0}data")) {
			dsID = DataStream.getDSFromUUID(contentMap.get(nodeID)).id;
			dsID = dsID.substring(dsID.indexOf("}") + 1);

			if (dsID.startsWith("._")) {
				// ignore
				System.out.println("ignore data stream " + dsID);
				return;
			}

			dsID = dsID.replaceAll("\\.", "");

			if (dsID.equals("DC")) {
				// cannot delete DC data stream
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

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		System.out.println("check property existence: " + id.toString());

		index = id.toString().indexOf("/");
		nodeID = id.toString().substring(0, index);
		propertyName = id.toString().substring(index + 1);

		// if (id.toString().equals("cafebabe-cafe-babe-cafe-babecafebabe") ||
		// 	id.toString().equals("deadbeef-cafe-babe-cafe-babecafebabe") ) {
		// 	return true;
		// }

		if (contentMap.get(id.toString().substring(0, index)) != null &&
				 (propertyName.equals("{http://www.jcp.org/jcr/1.0}data") ||
				  propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding") ||
				  propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType"))
			) {
			return true;
		}
		else if (propertyName.equals("{http://www.jcp.org/jcr/1.0}primaryType") &&
				 // ! nodeID.equals("cafebabe-cafe-babe-cafe-babecafebabe") &&
				 ! nodeID.equals("deadbeef-cafe-babe-cafe-babecafebabe")) {
			System.out.println("returning true");
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized boolean exists(NodeId id) throws ItemStateException {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		System.out.println("check node existence: " + id.toString());

		if (id.toString().equals("cafebabe-cafe-babe-cafe-babecafebabe") ||
			id.toString().equals("deadbeef-cafe-babe-cafe-babecafebabe") ) {
			return true;
		}

		if (uuidMap.get(id.toString()) != null) {
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

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented
		
		return false;
	}
}
