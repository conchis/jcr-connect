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

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeIdIterator;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.query.AbstractQueryHandler;
import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.NodeStateIterator;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.extractor.DefaultTextExtractor;
import org.apache.jackrabbit.extractor.TextExtractor;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.DefaultQueryNodeFactory;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.uuid.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.collections.iterators.AbstractIteratorDecorator;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.query.InvalidQueryException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.Calendar;
import org.apache.jackrabbit.core.query.AbstractQueryHandler;
import org.apache.jackrabbit.util.ISO8601;

import edu.northwestern.jcr.adapter.xtf.query.XTFQuery;
import edu.northwestern.jcr.adapter.xtf.query.ScoreNode;
import edu.northwestern.jcr.adapter.xtf.persistence.XTFDoc;

import java.io.InputStream;
import java.io.FileInputStream;
import org.xml.sax.InputSource;
import javax.jcr.*;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TransientRepository;

/**
 * <p>Implements a <a href="http://jackrabbit.apache.org/api/1.5/org/apache/jackrabbit/core/query/AbstractQueryHandler.html">Jackrabbit query handler</a> using
 * Fedora.</p>
 *
 * <p>Both the XPath and SQL queries are translated by the Jackrabbit core 
 * to the Abstract Query Tree (AQT), the common query description format 
 * that allows Jackrabbit to implement a query engine which is (to a 
 * certain extent) independent of the query syntax used (XPath or SQL). 
 * The <a href="http://jackrabbit.apache.org/search-implementation.html">
 * "Search Implementation" section of the Jackrabbit website</a> details the 
 * implementation of searches in Jackrabbit along with the default search 
 * index being used.</p>
 * 
 * <p>The search index in Jackrabbit is pluggable and has a default 
 * implementation based on Apache Lucene. The default implementation is 
 * independent of the persistence manager being used. To connect to an 
 * existing XTF repository, this search index manager is written to
 * interpret the AQT and generate raw XML queries that can be executed 
 * against the raw servlet of the XTF repository.</p>
 *
 * <p>In the default workspace configuration file 
 * <code>jackrabbit/workspaces/default/workspace.xml</code> this XTF
 * search index
 * is set as the search index used by the Jackrabbit repository
 * (in place of the default Lucene search index).
 *
 * <p>The default Jackrabbit search index actually maintains the index 
 * using Apache Lucene as well as running the parsed queries against the 
 * Lucene index. This search index, on the other hand, only handles 
 * queries since the "index" for XTF documents is maintained by
 * XTF repository.
 *
 * <p>ORDER BY clause is handled here by the {@link #sort} method. Other
 * than that this class is little more than a wrapper of query handling
 * classes.
 *
 * @author Xin Xiang
 */
public class SearchIndex extends AbstractQueryHandler {

    public static final List VALID_SYSTEM_INDEX_NODE_TYPE_NAMES
        = Collections.unmodifiableList(Arrays.asList(new Name[]{
            NameConstants.NT_CHILDNODEDEFINITION,
            NameConstants.NT_FROZENNODE,
            NameConstants.NT_NODETYPE,
            NameConstants.NT_PROPERTYDEFINITION,
            NameConstants.NT_VERSION,
            NameConstants.NT_VERSIONEDCHILD,
            NameConstants.NT_VERSIONHISTORY,
            NameConstants.NT_VERSIONLABELS,
            NameConstants.REP_NODETYPES,
            NameConstants.REP_SYSTEM,
            NameConstants.REP_VERSIONSTORAGE,
            // Supertypes
            NameConstants.NT_BASE,
            NameConstants.MIX_REFERENCEABLE
        }));

    private static final DefaultQueryNodeFactory 
		DEFAULT_QUERY_NODE_FACTORY = new DefaultQueryNodeFactory(
            VALID_SYSTEM_INDEX_NODE_TYPE_NAMES);

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(SearchIndex.class);

    /**
     * Name of the file to persist search internal namespace mappings.
     */
    private static final String NS_MAPPING_FILE = "ns_mappings.properties";

    /**
     * The default value for property {@link #minMergeDocs}.
     */
    public static final int DEFAULT_MIN_MERGE_DOCS = 100;

    /**
     * The default value for property {@link #maxMergeDocs}.
     */
    public static final int DEFAULT_MAX_MERGE_DOCS = Integer.MAX_VALUE;

    /**
     * the default value for property {@link #mergeFactor}.
     */
    public static final int DEFAULT_MERGE_FACTOR = 10;

    /**
     * the default value for property {@link #maxFieldLength}.
     */
    public static final int DEFAULT_MAX_FIELD_LENGTH = 10000;

    /**
     * The default value for property {@link #extractorPoolSize}.
     * @deprecated this value is not used anymore. Instead the default value
     * is calculated as follows: 2 * Runtime.getRuntime().availableProcessors().
     */
    public static final int DEFAULT_EXTRACTOR_POOL_SIZE = 0;

    /**
     * The default value for property {@link #extractorBackLog}.
     */
    public static final int DEFAULT_EXTRACTOR_BACK_LOG = Integer.MAX_VALUE;

    /**
     * The default timeout in milliseconds which is granted to the text
     * extraction process until fulltext indexing is deferred to a background
     * thread.
     */
    public static final long DEFAULT_EXTRACTOR_TIMEOUT = 100;

    /**
     * The path of the root node.
     */
    private static final Path ROOT_PATH;

    /**
     * The path <code>/jcr:system</code>.
     */
    private static final Path JCR_SYSTEM_PATH;

    static {
        PathFactory factory = PathFactoryImpl.getInstance();
        ROOT_PATH = factory.create(NameConstants.ROOT);
        try {
            JCR_SYSTEM_PATH = factory.create(ROOT_PATH, NameConstants.JCR_SYSTEM, false);
        } catch (RepositoryException e) {
            // should never happen, path is always valid
            throw new InternalError(e.getMessage());
        }
    }

    /**
     * The namespace mappings used internally.
     */
    private NamespaceMappings nsMappings;

    /**
     * The name and path resolver used internally.
     */
    private NamePathResolver npResolver;

    /**
     * minMergeDocs config parameter.
     */
    private int minMergeDocs = DEFAULT_MIN_MERGE_DOCS;

    /**
     * volatileIdleTime config parameter.
     */
    private int volatileIdleTime = 3;

    /**
     * maxMergeDocs config parameter
     */
    private int maxMergeDocs = DEFAULT_MAX_MERGE_DOCS;

    /**
     * mergeFactor config parameter
     */
    private int mergeFactor = DEFAULT_MERGE_FACTOR;

    /**
     * maxFieldLength config parameter
     */
    private int maxFieldLength = DEFAULT_MAX_FIELD_LENGTH;

    /**
     * extractorPoolSize config parameter
     */
    private int extractorPoolSize = 2 * Runtime.getRuntime().availableProcessors();

    /**
     * extractorBackLog config parameter
     */
    private int extractorBackLog = DEFAULT_EXTRACTOR_BACK_LOG;

    /**
     * extractorTimeout config parameter
     */
    private long extractorTimeout = DEFAULT_EXTRACTOR_TIMEOUT;

    /**
     * Number of documents that are buffered before they are added to the index.
     */
    private int bufferSize = 10;

    /**
     * Compound file flag
     */
    private boolean useCompoundFile = true;

    /**
     * Flag indicating whether document order is enabled as the default
     * ordering.
     * <p/>
     * Default value is: <code>false</code>.
     */
    private boolean documentOrder = false;

    /**
     * If set <code>true</code> the index is checked for consistency on startup.
     * If <code>false</code> a consistency check is only performed when there
     * are entries in the redo log on startup.
     * <p/>
     * Default value is: <code>false</code>.
     */
    private boolean forceConsistencyCheck = false;

    /**
     * If set <code>true</code> the index is checked for consistency depending
     * on the {@link #forceConsistencyCheck} parameter. If set to
     * <code>false</code>, no consistency check is performed, even if the redo
     * log had been applied on startup.
     * <p/>
     * Default value is: <code>false</code>.
     */
    private boolean consistencyCheckEnabled = false;

    /**
     * If set <code>true</code> errors detected by the consistency check are
     * repaired. If <code>false</code> the errors are only reported in the log.
     * <p/>
     * Default value is: <code>true</code>.
     */
    private boolean autoRepair = true;

    /**
     * The uuid resolver cache size.
     * <p/>
     * Default value is: <code>1000</code>.
     */
    private int cacheSize = 1000;

    /**
     * The number of documents that are pre fetched when a query is executed.
     * <p/>
     * Default value is: {@link Integer#MAX_VALUE}.
     */
    private int resultFetchSize = Integer.MAX_VALUE;

    /**
     * If set to <code>true</code> the fulltext field is stored and and a term
     * vector is created with offset information.
     * <p/>
     * Default value is: <code>false</code>.
     */
    private boolean supportHighlighting = false;

    /**
     * Indicates if this <code>SearchIndex</code> is closed and cannot be used
     * anymore.
     */
    private boolean closed = false;

    /**
     * Default constructor.
     */
    public SearchIndex() {
    }

    /**
     * Initializes this <code>QueryHandler</code>. This implementation requires
     * that a path parameter is set in the configuration. If this condition
     * is not met, a <code>IOException</code> is thrown.
     *
     * @throws IOException if an error occurs while initializing this handler.
     */
    protected void doInit() throws IOException {
        QueryHandlerContext context = getContext();
        // if (path == null) {
        //     throw new IOException("SearchIndex requires 'path' parameter in configuration!");
        // }

        Set excludedIDs = new HashSet();
        if (context.getExcludedNodeId() != null) {
            excludedIDs.add(context.getExcludedNodeId());
        }

        nsMappings = 
			new NSRegistryBasedNamespaceMappings(
												 context.getNamespaceRegistry());
    }

    /**
     * Adds the <code>node</code> to the search index.
     * @param node the node to add.
     * @throws RepositoryException if an error occurs while indexing the node.
     * @throws IOException if an error occurs while adding the node to the index.
     */
    public void addNode(NodeState node) throws RepositoryException, IOException {
        throw new UnsupportedOperationException("addNode");
    }

    /**
     * Removes the node with <code>uuid</code> from the search index.
     * @param id the id of the node to remove from the index.
     * @throws IOException if an error occurs while removing the node from
     * the index.
     */
    public void deleteNode(NodeId id) throws IOException {
        throw new UnsupportedOperationException("deleteNode");
    }

    /**
     * Ignore the updates since the resource index is managed by
	 * Fedora.
	 *
     * @param remove uuids of nodes to remove.
     * @param add    NodeStates to add. Calls to <code>next()</code> on this
     *               iterator may return <code>null</code>, to indicate that a
     *               node could not be indexed successfully.
     */
    public void updateNodes(NodeIdIterator remove, NodeStateIterator add)
	{
    }

    /**
     * Creates a new query by specifying the query statement itself and the
     * language in which the query is stated.  If the query statement is
     * syntactically invalid, given the language specified, an
     * InvalidQueryException is thrown. <code>language</code> must specify a query language
     * string from among those returned by QueryManager.getSupportedQueryLanguages(); if it is not
     * then an <code>InvalidQueryException</code> is thrown.
     *
     * @param session the session of the current user creating the query object.
     * @param itemMgr the item manager of the current user.
     * @param statement the query statement.
     * @param language the syntax of the query statement.
     * @throws InvalidQueryException if statement is invalid or language is unsupported.
     * @return A <code>Query</code> object.
     */
    public ExecutableQuery createExecutableQuery(SessionImpl session,
                                             ItemManager itemMgr,
                                             String statement,
                                             String language)
            throws InvalidQueryException {
        QueryImpl query = new QueryImpl(session, itemMgr, this,
                getContext().getPropertyTypeRegistry(), statement, language, getQueryNodeFactory());
        query.setRespectDocumentOrder(documentOrder);
        return query;
    }

    /**
     * Creates a new query by specifying the query object model. If the query
     * object model is considered invalid for the implementing class, an
     * InvalidQueryException is thrown.
     *
     * @param session the session of the current user creating the query
     *                object.
     * @param itemMgr the item manager of the current user.
     * @param qomTree query query object model tree.
     * @return A <code>Query</code> object.
     * @throws javax.jcr.query.InvalidQueryException
     *          if the query object model tree is invalid.
     * @see QueryHandler#createExecutableQuery(SessionImpl, ItemManager, QueryObjectModelTree)
     */
    public ExecutableQuery createExecutableQuery(
            SessionImpl session,
            ItemManager itemMgr,
            QueryObjectModelTree qomTree) throws InvalidQueryException {
		return null;
    }

    /**
     * This method returns the QueryNodeFactory used to parse Queries. This method
     * may be overridden to provide a customized QueryNodeFactory
     */
    protected DefaultQueryNodeFactory getQueryNodeFactory() {
        return DEFAULT_QUERY_NODE_FACTORY;
    }

    /**
     * Closes this <code>QueryHandler</code> and frees resources attached
     * to this handler.
     */
    public void close() {
    }


	/**
	 * Sorts the query results based on the value of the properties.
	 * The ORDER BY clause is actually handled here.
	 *
	 * @param lines array of comma-separated full paths to the query results
	 * @param valueMap maps pid to comma-separated values
	 * @param orderSpecs true for ascending, false for descending
	 * @return array of sorted results
	 */
	private String [] sort(String [] lines, 
						   final Map<String, String> valueMap,
						   final boolean [] orderSpecs)
	{
		
		Comparator<String> result_order = new Comparator<String> () {
			public int compare(String line1, String line2) 
			{
				String pid1, pid2;
				String valueList1, valueList2;
				String [] parts;
				String [] valueString1, valueString2;
				String value1, value2;
				Double double1, double2;
				Long long1, long2;
				Calendar calendar1, calendar2;
				Boolean boolean1, boolean2;
				int i;
				int compare = 0;

				// get pid of the last object in the list
				parts = line1.split(",");
				pid1 = parts[parts.length - 1];
				parts = line2.split(",");
				pid2 = parts[parts.length - 1];

				// get comma-separated the value list
				valueList1 = valueMap.get(pid1);
				valueList2 = valueMap.get(pid2);

				valueString1 = valueList1.split(",");
				valueString2 = valueList2.split(",");

				for (i = 0; i < valueString1.length; ++i) {
					// loop over the values
					value1 = valueString1[i];
					parts = value1.split("%57");
					value1 = parts[parts.length - 1];

					value2 = valueString2[i];
					parts = value2.split("%57");
					value2 = parts[parts.length - 1];

					switch (Integer.parseInt(parts[0])) {
					case PropertyType.STRING:
						if (orderSpecs[i]) {
							compare = value1.compareTo(value2);
						}
						else {
							compare = value2.compareTo(value1);
						}
						break;
					case PropertyType.DOUBLE:
						double1 = Double.parseDouble(value1);
						double2 = Double.parseDouble(value2);
						if (orderSpecs[i]) {
							compare = double1.compareTo(double2);
						}
						else {
							compare = double2.compareTo(double1);
						}
						break;
					case PropertyType.LONG:
						long1 = Long.parseLong(value1);
						long2 = Long.parseLong(value2);
						if (orderSpecs[i]) {
							compare = long1.compareTo(long2);
						}
						else {
							compare = long2.compareTo(long1);
						}
						break;
					case PropertyType.DATE:
						calendar1 = ISO8601.parse(value1);
						calendar2 = ISO8601.parse(value2);
						if (orderSpecs[i]) {
							compare = calendar1.compareTo(calendar2);
						}
						else {
							compare = calendar2.compareTo(calendar1);
						}
						break;
					case PropertyType.BOOLEAN:
						boolean1 = Boolean.valueOf(value1);
						boolean2 = Boolean.valueOf(value2);
						if (orderSpecs[i]) {
							compare = boolean1.compareTo(boolean2);
						}
						else {
							compare = boolean2.compareTo(boolean1);
						}
						break;
						// not supported
					case PropertyType.BINARY:
					case PropertyType.REFERENCE:
					case PropertyType.NAME:
					case PropertyType.PATH:
						compare = 0;
					}

					if (compare != 0) {
						return compare;
					}

					// more comparison
				}

				return compare;
			}
		};

		List<String> resultList = Arrays.asList(lines);
		Collections.sort(resultList, result_order);
		return resultList.toArray(new String[0]);
	}


	private Session loginRepository()
		throws Exception
	{
		File configFile = new File("repository.xml");
		InputStream configFileInputStream = 
			new FileInputStream(configFile);
		InputSource configFileInputSource = 
			new InputSource(configFileInputStream);
		File repositoryHome = new File("jackrabbit");
		RepositoryConfig config = 
			RepositoryConfig.create(configFileInputSource, 
									repositoryHome.getAbsolutePath());
		// repository = RepositoryImpl.create(config);
		Repository repository = new TransientRepository(config);
		configFileInputStream.close();

		Credentials credentials = 
			new SimpleCredentials("user", "".toCharArray());
		Session session = repository.login(credentials, "default");

		return session;
	}

    /**
     * Sorts the query results and converts them to query hits (in the form
	 * "{} {}path1 {}path2 ..."). Note at this point the 
	 * {@link XTFQuery} object is already populated with query results.
	 *
     * @param session the session that executes the query.
     * @param queryImpl the query impl.
     * @param query the Fedora query with the result list already populated.
     * @param orderProps name of the properties for sort order.
     * @param orderSpecs the order specs for the sort order properties.
     * <code>true</code> indicates ascending order, <code>false</code> indicates
     * descending.
     * @return the query hits.
     * @throws IOException if an error occurs while searching the index.
     */
    public MultiColumnQueryHits executeQuery(SessionImpl session,
											 AbstractQueryImpl queryImpl,
											 XTFQuery query,
											 Name[] orderProps,
											 boolean[] orderSpecs) 
		throws IOException, Exception 
	{
        checkOpen();

		XTFDoc [] docResult;
		String [] result;
		int i, j;
		String s;
		String [] parts;
		Map<String, String> valueMap = new HashMap<String, String>();
		List<String> list = new ArrayList<String>();
		// query has been executes and the result is available
		docResult = query.getCurrentResult();

		for (XTFDoc doc : docResult) {
			list.add(doc.getID().replaceAll("/", ","));
		}
		result = list.toArray(new String[0]);

		if (orderProps != null && orderProps.length > 0 &&
			result.length > 0) {
			// do not bother if there is no result
			String select = "$id";
			String where = "$s <http://purl.org/dc/elements/1.1/identifier> $id .";
			log.debug("order by properties: ");
			for (i = 0; i < orderProps.length; ++i) {
				String localName = orderProps[i].getLocalName();
				String namespaceURI = orderProps[i].getNamespaceURI();
				if (namespaceURI.equals("")) {
					namespaceURI = "http://sling.apache.org/jcr/sling/1.0";
				}

				select += " $" + localName;
				where += " $s <" + namespaceURI + "/" + localName + 
					"> $" + localName + ".";
				log.debug(localName);
			}

			where += " FILTER ( ";

			for (i = 0; i < result.length; ++i) {
				String line = result[i];
				parts = line.split(",");
				String id = parts[parts.length - 1];
				if (i > 0) {
					where += " || ";
				}
				where += "regex($id, '^" + id + "$')";
			}

			where += " )";

			String sparql = "select " + select + " from <#ri> {" +
				where + "}";
			log.debug(sparql);

			String [] resultWithProperty = null;
			// FedoraPersistenceManager.fc.searchObjects(sparql, "sparql");

			// creates value map
			for (String line : resultWithProperty) {
				int index = line.indexOf(",");
				log.debug("putting " + line.substring(0, index));
				valueMap.put(line.substring(0, index),
							 line.substring(index + 1));
				log.debug(line.substring(0, index) + ", " +
								   line.substring(index + 1));
			}

			log.debug("sorting result list ...");
			result = sort(result, valueMap, orderSpecs);
		}

		Session jcrSession = 
			session.impersonate(new SimpleCredentials("superuser", 
											  "".toCharArray()));
		// loginRepository();
		Node node;

		// creates the result in a format similar to full JCR path
		// for (i = 0; i < result.length; ++i) {
		for (i = 0; i < docResult.length; ++i) {
			result[i] = docResult[i].getID().replaceAll("/", ",");

			log.info("result: " + result[i]);
			parts = result[i].split(",");

			s = "{}";
			if (result[i].equals("")) {
				// root
				result[i] = s;
				continue;
			}

						
			node = jcrSession.getRootNode();

			for (j = 0; j < parts.length; ++j) {
				try {
					node = node.getNode(parts[j]);
				} catch (PathNotFoundException e) {
					System.out.println("writing: " + parts[j]);
					node = node.addNode(parts[j]);

					String [] properties = docResult[i].listProperties();
					String [] value;
					for (String property : properties) {
						value = docResult[i].getProperty(property);

						if (value.length == 1) {
							node.setProperty(property, value[0]);
						}
						else {
							node.setProperty(property, value);
						}
					}
				}

				String escapedPID =
				// FedoraPersistenceManager.escapePID(parts[j]);
					parts[j];
				if (escapedPID.equals("xmltext")) {
					// needs a better way to handle the jcr namespace
					s += "\t{http://www.jcp.org/jcr/1.0}xmltext";
				}
				else {
					s += "\t{}" + escapedPID;
				}
			}

			log.info(s);
			result[i] = s;			
		}

		jcrSession.save();

		return new QueryHitsAdapter(new XTFQueryHits(result), 
									QueryImpl.DEFAULT_SELECTOR_NAME);
    }

    /**
     * Returns the namespace mappings for the internal representation.
     * @return the namespace mappings for the internal representation.
     */
    public NamespaceMappings getNamespaceMappings() {
        return nsMappings;
    }
	
    //----------------------------< internal >----------------------------------
	
    /**
     * Checks if this <code>SearchIndex</code> is open, otherwise throws
     * an <code>IOException</code>.
     *
     * @throws IOException if this <code>SearchIndex</code> had been closed.
     */
    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException("query handler closed and cannot be used anymore.");
        }
    }
}
