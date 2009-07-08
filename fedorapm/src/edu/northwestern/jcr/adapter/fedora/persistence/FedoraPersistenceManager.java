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

	/** fedora client handle */
	private FedoraConnector fc;

	/** list of nodes that are pending to persist */
	private static List<NodeState> pendingNodes = new ArrayList<NodeState>();

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
		id = pid.replace(":", "_");

		return id;
	}

	/**
	 * Restore Fedora pid
	 */
	private String unescapePID(String id)
	{
		String [] parts = id.split("__");
		String pid = "";

		for (int i = 0; i < parts.length; ++i) {
			pid += parts[i].replaceAll("_", ":");

			if (i < parts.length - 1) {
				pid += "_";
			}
		}				

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

				// put orignial pid to access datastreams
				uuidMap.put(uuid.toString(), pid);

				// escape :
				pid = escapePID(pid);

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

		if (! dsID.equals("")) {
			// get data stream
			pid = uuidMap.get(nodeID);
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

		String nodeID = state.getNodeId().toString();


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
				String pid = unescapePID(escapedPID);

				if (uuidMap.get(uuid) == null) {
					uuidMap.put(uuid, pid.substring(pid.indexOf("}") + 1));
				}

			}

			// store all pending nodes - assuming a single-level hieararchy
			for (NodeState ns : pendingNodes) {
				store(ns);
			}

			pendingNodes.clear();

			// store all pending properties
			for (PropertyState ps : pendingProperties) {
				store(ps);
			}

			pendingProperties.clear();

			return;
		}

		// non-root and non-system
		if (uuidMap.get(nodeID) == null) {
			// not in map yet, add to the pending node list
			pendingNodes.add(state);
			
			return;
		}

		// persist to Fedora repository
		fc.createFedoraObject(uuidMap.get(nodeID));
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
		InternalValue[] values = state.getValues();
		PropertyId id = state.getPropertyId();

		index = id.toString().indexOf("/");
		nodeID = id.toString().substring(0, index);
		propertyName = id.toString().substring(index + 1);
		pid = uuidMap.get(nodeID);

		// non-root and non-system
		if (pid == null) {
			// not in map yet
			pendingProperties.add(state);
			
			return;
		}

		if (propertyName.equals("{}DC")) {
			System.out.println("updating DC stream in " + pid);

			fc.modifyDCDataStream(pid, values[0].toString().getBytes());
		}
		else {
			propertyName = 
				propertyName.substring(propertyName.indexOf("}") + 1);

			if (propertyName.equals("primaryType")) {
				return;
			}

			System.out.println("updating " + propertyName + " stream in " + 
							   pid);

			fc.addDataStream(pid, propertyName, 
							 values[0].toString().getBytes());
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
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented
	}

	/**
	 * {@inheritDoc}
	 */
	protected void destroy(PropertyState state) throws ItemStateException {
		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// to be implemented
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
		String propertyName;

		if (!initialized) {
			throw new IllegalStateException("not initialized");
		}

		// System.out.println("check property existence: " + id.toString());

		index = id.toString().indexOf("/");
		propertyName = id.toString().substring(index + 1);

		// if (id.toString().equals("cafebabe-cafe-babe-cafe-babecafebabe") ||
		// 	id.toString().equals("deadbeef-cafe-babe-cafe-babecafebabe") ) {
		// 	return true;
		// }

		if (contentMap.get(id.toString().substring(0, index)) != null &&
				 (propertyName.equals("{http://www.jcp.org/jcr/1.0}data") ||
				  propertyName.equals("{http://www.jcp.org/jcr/1.0}encoding") ||
				  propertyName.equals("{http://www.jcp.org/jcr/1.0}mimeType")))
			{
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

		// System.out.println("check node existence: " + id.toString());

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
		
		return true;
	}
}
