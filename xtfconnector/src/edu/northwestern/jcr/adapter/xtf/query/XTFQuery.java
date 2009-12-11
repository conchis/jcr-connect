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
package edu.northwestern.jcr.adapter.xtf.query;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.northwestern.jcr.adapter.xtf.persistence.XTFClient;
import edu.northwestern.jcr.adapter.xtf.persistence.XTFDoc;

/**
 * <p><code>XTFQuery</code> processes the query, stores the filter 
 * conditions and temporary results on a step by step basis.</p>
 *
 * <p>Iterating over the location steps in the AQT (Abstract Query Tree),
 * {@link XTFQueryBuilder} calls the {@link #addStep} method
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
class XTFQuery {
	/** log4j logger. */
	private static Logger log = 
		LoggerFactory.getLogger(XTFQuery.class);
	
	/** filter (property and node type constraints). */
	private String filter;
	/** result at current location step. */
	// private String [] current;
	private XTFDoc [] current;
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
	public XTFQuery()
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
			// current = new String [] {""};
			current = new XTFDoc [] {};
			// reset level
			level = 0;
		}
	}

	/**
	 * Returns the result at current level.
	 *
	 * @return list of pid as the query result
	 */
	public XTFDoc [] getCurrentResult()
	{
		return current;
	}

	/**
	 * Processes current step and executes the created raw XML query
	 * against the raw servlet of the XTF repository.
	 */
	public void execute() throws Exception
	{
		int i, j;
		String pid;
		int index;
		String [] parts;
		// String [] result;
		XTFDoc [] result;
		String prefix;
		boolean hit;
		String query;
		String currentPath;
		String [] temp;
		List<String> resultList;
		List<String> pathList;
		String uuid;

		XTFClient client = new XTFClient();

		result = null;
		resultList = new ArrayList<String>();

		try {
			result = client.getPath(filter);

			if (result == null) {
				result = new XTFDoc [] {};
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error getting facet!");
		}

		// for (String path : result) {
		// 	resultList.add(path.replaceAll("/", ","));
		// }

		// current = resultList.toArray(new String[0]);
		current = result;
	}
}
