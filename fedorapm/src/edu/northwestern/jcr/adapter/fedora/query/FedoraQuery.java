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
package edu.northwestern.jcr.adapter.fedora.query;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.northwestern.jcr.adapter.fedora.persistence.FedoraPersistenceManager;
import edu.northwestern.jcr.adapter.fedora.persistence.FedoraConnector;

/**
 * <p><code>FedoraQuery</code> processes the query, stores the filter 
 * conditions and temporary results on a step by step basis.</p>
 *
 * <p>Iterating over the location steps in the AQT (Abstract Query Tree),
 * {@link FedoraQueryBuilder} calls the {@link #addStep} method
 * with appropriate type and filter information and then calls the
 * {@link #execute} method. Current results (pid) are stored in
 * the array {@link #current} while the array {@link #path}
 * contains lists of pid along the paths to the results.
 * The pid in the array <code>current</code> after all the steps are
 * processed will be the query results.</p>
 *
 * <p>The <a href="http://jackrabbit.apache.org/search-implementation.html">"Search Implementation" section of the Jackrabbit website</a> gives a short
 * introduction to query handling in Jackrabbit.
 *
 * @author Xin Xiang
 */
class FedoraQuery {
	/** log4j logger. */
	private static Logger log = 
		LoggerFactory.getLogger(FedoraQuery.class);
	
	/** filter (property and node type constraints). */
	private String filter;
	/** result at current location step. */
	private String [] current;
	/** path to current result. */
	private String [] path;
	/** current level in the tree. */
	private int level;
	/** type of current step: exact, children, descendants or dereference 
		function. */
	private int type;
	/** node local name at this level. */
	private String name;

	/** type of the step: exact match. */
	public static final int EXACT = 0;
	/** type of the step: children of a node. */
	public static final int CHILDREN = 1;
	/** type of the step: descendants of a node. */
	public static final int DESCENDANTS = 2;
	/** type of the step: dereference function. */
	public static final int DEREF = 3;

    /**
     * Public constructor.
     */
	public FedoraQuery()
	{
		level = 0;
	}

	/**
	 * Adds one location step.
	 *
	 * @param name relative JCR path
	 * @param type type of this location step (exact, children, descendants)
	 * @param filter filter condition (property and node type constraints)
	 */
	public void addStep(String name, int type, String filter)
	{
		this.name = name;
		this.type = type;
		this.filter = filter;

		level++;

		log.info("level " + level + ": " + name + ", " + type);

		if (name != null && name.equals("/")) {
			current = new String [] {""};
			// reset level
			level = 0;
		}
	}

	/**
	 * Returns the result at current level.
	 *
	 * @return list of pid as the query result
	 */
	public String [] getCurrentResult()
	{
		return current;
	}

	/**
	 * Processes current step and executes the created SPARQL query
	 * against the resource index.
	 */
	public void execute() throws Exception
	{
		int i, j;
		String pid;
		int index;
		String [] parts;
		String [] result;
		String prefix;
		boolean hit;
		String query;
		String currentPath;
		String [] temp;
		List<String> resultList;
		List<String> pathList;
		FedoraConnector fc;
		String uuid;

		fc = FedoraPersistenceManager.fc;

		if (type == DEREF) {
			resultList = new ArrayList<String>();

			for (i = 0; i < current.length; ++i) {
				parts = current[i].split(",");
				pid = parts[parts.length - 1];

				// get UUID of the target node
				uuid = fc.getProperty(pid, filter);
				parts = uuid.split("%57");
				
				for (j = 0; j < Integer.parseInt(parts[1]); ++j) {
					uuid = parts[parts.length - j - 1];
					query = "select $id from <#ri> { $s <http://purl.org/dc/elements/1.1/identifier> $id . $s <http://www.jcp.org/jcr/1.0/uuid> $uuid . ";

					query += "FILTER ( regex($uuid, '%57" + uuid + "$' ) ";

					// restrict name
					if (! name.equals("*")) {
						query += " && regex($id, '%57" + name + "$')";
					}

					query += ") }";

					log.debug(query);
					current = fc.searchObjects(query, "sparql");

					for (String s : current) {
						resultList.add(s);
					}
				}
			}

			current = resultList.toArray(new String[0]);
			current = fc.getPath(current);
			path = new String [current.length];

			for (i = 0; i < current.length; ++i) {
				parts = current[i].split(",");

				prefix = "";
				for (j = 0; j < parts.length; ++j) {
					prefix += "/" + 
						FedoraPersistenceManager.escapePID(parts[j]);
				}

				path[i] = prefix;
			}
			
			return;
		}

		if (level == 1 && name.equals("*")) {
			if (type == CHILDREN) {
				// first level objects
				current = fc.listObjectsRI(filter);
			}
			else { // type == DESCENDANT
				// all objects
				current = fc.listDescendantsRI(null,
											   filter);
			}

			path = new String [current.length];

			for (i = 0; i < current.length; ++i) {
				if (type == CHILDREN) {
					path[i] = "/" + 
						FedoraPersistenceManager.escapePID(current[i]);
				}
				else {
					parts = current[i].split(",");

					prefix = "";
					for (j = 0; j < parts.length; ++j) {
						prefix += "/" + 
							FedoraPersistenceManager.escapePID(parts[j]);
					}

					path[i] = prefix;
				}
			}

			return;
		}

		resultList = new ArrayList<String>();
		pathList = new ArrayList<String>();

		if ((type == DESCENDANTS || type == CHILDREN)
			&& name.equals("*")) {
			// descendants of a low level node
			// create one query for each current result

			for (i = 0; i < current.length; ++i) {
				parts = current[i].split(",");
				pid = parts[parts.length - 1];

				if (type == DESCENDANTS) {
					// search for descendants of this object
					result = fc.listDescendantsRI(pid,
												  filter);
				}
				else {
					// search for members of this object
					result = fc.listMembers(pid, 
											filter);
				}

				// add to the result list
				for (String r : result) {
					resultList.add(current[i] + "," + r);

					parts = r.split(",");

					prefix = path[i];
					for (j = 0; j < parts.length; ++j) {
						prefix += "/" + 
							FedoraPersistenceManager.escapePID(parts[j]);
					}

					pathList.add(prefix);
				}
			}
			
			current = resultList.toArray(new String[0]);
			path = pathList.toArray(new String[0]);

			return;
		}

		if (type == DESCENDANTS && ! name.equals("*")) {
			// exact match in descendants

			if (level == 1) {
				// no current node yet
				current = new String[1];
				path = new String[1];
				current[0] = path[0] = "";
			}

			// search for the objects
			try {
				result = fc.listObjects("*57" + name);
			} catch (Exception e) {
				String msg = "error listing objects";
				log.error(msg);
				throw new Exception(msg, null);
			}

			for (String t : result) {
				log.debug("s: " + t);
			}

			// filter the objects
			if (filter != null) {
				result = fc.filterObjects(result, filter);
			}

			// get the full path (in CSV format)
			result = fc.getPath(result);

			for (String s : result) {
				hit = false;

				for (String t : current) {
					if (s.startsWith(t)) {
						hit = true;
						break;
					}
				}

				if (hit) {
					resultList.add(s);

					parts = s.split(",");

					prefix = "";
					for (j = 0; j < parts.length; ++j) {
						prefix += "/" + 
							FedoraPersistenceManager.escapePID(parts[j]);
					}

					pathList.add(prefix);
				}

 			}

			current = resultList.toArray(new String[0]);
			path = pathList.toArray(new String[0]);

			return;
		} // end of exact match in descendants

		// exact match
		if (level == 1) {
			currentPath = "/" + name;

			query = "select $t from <#ri> { $s <http://purl.org/dc/elements/1.1/identifier> $t . FILTER ( regex($t, '^sling1:" +
				FedoraPersistenceManager.unescapePIDSling(currentPath.hashCode() + "%57" + name) + "$') || regex($t, '^" +
				FedoraPersistenceManager.unescapePID(name) + "$') )";
			if (filter != null) {
				// query += " and " + filter;
				query += " " + filter;
			}

			query += "}";

			log.debug(query);
			current = fc.searchObjects(query, "sparql");
			path = new String[1];
			path[0] = currentPath;
			return;
		}

		for (i = 0; i < current.length; ++i) {
			parts = current[i].split(",");
			pid = parts[parts.length - 1];

			currentPath = path[i] + "/" + name;
			log.debug(currentPath);

			query = "select $b from <#ri> { $s <info:fedora/fedora-system:def/relations-external#isMemberOfCollection> $r . $r <http://purl.org/dc/elements/1.1/identifier> $a . FILTER ( regex($a, '^" + pid + "$')) $s <http://purl.org/dc/elements/1.1/identifier> $b . FILTER ( regex($b, '^" + "sling" + level + ":" +

				FedoraPersistenceManager.unescapePIDSling(currentPath.hashCode() + "%57" + name) + "$') || regex($t, '^" +
				FedoraPersistenceManager.unescapePID(name) + "$') )";
			if (filter != null) {
				// query += " and " + filter;
				query += " " + filter;
			}

			query += "}";

			log.debug(query);
			temp = fc.searchObjects(query, "sparql");
			if (temp.length > 0) {
				resultList.add(current[i] + "," + temp[0]);
				pathList.add(currentPath);
			}
		}

		current = resultList.toArray(new String[0]);
		path = pathList.toArray(new String[0]);
	}
}
