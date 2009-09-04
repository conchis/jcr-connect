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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.HierarchyManagerImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.query.AndQueryNode;
import org.apache.jackrabbit.spi.commons.query.DefaultQueryNodeVisitor;
import org.apache.jackrabbit.spi.commons.query.DerefQueryNode;
import org.apache.jackrabbit.spi.commons.query.ExactQueryNode;
import org.apache.jackrabbit.spi.commons.query.LocationStepQueryNode;
import org.apache.jackrabbit.spi.commons.query.NodeTypeQueryNode;
import org.apache.jackrabbit.spi.commons.query.NotQueryNode;
import org.apache.jackrabbit.spi.commons.query.OrQueryNode;
import org.apache.jackrabbit.spi.commons.query.OrderQueryNode;
import org.apache.jackrabbit.spi.commons.query.PathQueryNode;
import org.apache.jackrabbit.spi.commons.query.PropertyFunctionQueryNode;
import org.apache.jackrabbit.spi.commons.query.QueryConstants;
import org.apache.jackrabbit.spi.commons.query.QueryNode;
import org.apache.jackrabbit.spi.commons.query.QueryNodeVisitor;
import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
import org.apache.jackrabbit.spi.commons.query.RelationQueryNode;
import org.apache.jackrabbit.spi.commons.query.TextsearchQueryNode;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.XMLChar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.query.lucene.*;

import edu.northwestern.jcr.adapter.fedora.query.FedoraQuery;

/**
 * Implements a query builder that takes an abstract query tree and creates
 * a {@link FedoraQuery} object that can be executed on the resource index.
 *
 * <p>The SPARQL expressions are generated and executed on a step by step
 * basis. Once the translation is finished the {@link FedoraQuery} object
 * returned by {@link #createFedoraQuery} will contain the list of pid along 
 * with full path as a result of the query.
 */
public class FedoraQueryBuilder implements QueryNodeVisitor {

    /**
     * log4j logger for this class.
     */
    private static final Logger log = 
		LoggerFactory.getLogger(FedoraQueryBuilder.class);

    /**
     * Root node of the abstract query tree.
     */
    private final QueryRootNode root;

    /**
     * Session of the user executing this query.
     */
    private final SessionImpl session;

    /**
     * The shared item state manager of the workspace.
     */
    private final ItemStateManager sharedItemMgr;

    /**
     * A hierarchy manager based on {@link #sharedItemMgr} to resolve paths.
     */
    private final HierarchyManager hmgr;

    /**
     * Namespace mappings to internal prefixes.
     */
    private final NamespaceMappings nsMappings;

    /**
     * Name and Path resolver.
     */
    private final NamePathResolver resolver;

    /**
     * The property type registry.
     */
    private final PropertyTypeRegistry propRegistry;

    /**
     * Exceptions thrown during tree translation.
     */
    private final List exceptions = new ArrayList();

    /**
     * Creates a new <code>FedoraQueryBuilder</code> instance.
     *
     * @param root               the root node of the abstract query tree.
     * @param session            of the user executing this query.
     * @param sharedItemMgr      the shared item state manager of the
     *                           workspace.
     * @param hmgr               a hierarchy manager based on sharedItemMgr.
     * @param nsMappings         namespace resolver for internal prefixes.
     * @param propReg            the property type registry.
     */
    private FedoraQueryBuilder(QueryRootNode root,
                               SessionImpl session,
                               ItemStateManager sharedItemMgr,
                               HierarchyManager hmgr,
                               NamespaceMappings nsMappings,
                               PropertyTypeRegistry propReg
							   ) {
        this.root = root;
        this.session = session;
        this.sharedItemMgr = sharedItemMgr;
        this.hmgr = hmgr;
        this.nsMappings = nsMappings;
        this.propRegistry = propReg;
        this.resolver = NamePathResolverImpl.create(nsMappings);
    }

    /**
     * Creates a <code>FedoraQuery</code> object from an
     * abstract query tree.
     *
     * @param root            the root node of the abstract query tree.
     * @param session         of the user executing the query.
     * @param sharedItemMgr   the shared item state manager of the workspace.
     * @param nsMappings      namespace resolver for internal prefixes.
     * @param propReg         the property type registry to lookup type
     *                        information.
     * @return the FedoraQuery object.
     * @throws RepositoryException if an error occurs during the translation.
     */
    public static FedoraQuery createQuery(QueryRootNode root,
										  SessionImpl session,
										  ItemStateManager sharedItemMgr,
										  NamespaceMappings nsMappings,
										  PropertyTypeRegistry propReg
									 )
            throws RepositoryException 
	{
        HierarchyManager hmgr = new HierarchyManagerImpl(
                RepositoryImpl.ROOT_NODE_ID, sharedItemMgr);
        FedoraQueryBuilder builder = 
			new FedoraQueryBuilder(
								   root, session, sharedItemMgr, hmgr, 
								   nsMappings, 
								   propReg
								   );

		FedoraQuery q = builder.createFedoraQuery();
        if (builder.exceptions.size() > 0) {
            StringBuffer msg = new StringBuffer();
            for (Iterator it = builder.exceptions.iterator(); it.hasNext();) {
                msg.append(it.next().toString()).append('\n');
            }
            throw new RepositoryException("Exception building query: " + msg.toString());
        }
        return q;
    }

    /**
     * Starts the tree traversal and returns the 
     * {@link edu.northwestern.jcr.adapter.fedora.query.FedoraQuery}.
     *
     * @return the <code>FedoraQuery</code> object.
     * @throws RepositoryException
     */
    private FedoraQuery createFedoraQuery() throws RepositoryException {
		return (FedoraQuery) root.accept(this, null);
    }

    //---------------------< QueryNodeVisitor interface >-----------------------

	/**
	 * Visits the root node.
	 *
	 * @param node the query root node
	 * @param data not used in this implementation
	 * @return the <code>FedoraQuery</code> object
	 */
    public Object visit(QueryRootNode node, Object data) 
		throws RepositoryException 
	{
		FedoraQuery wrapped = null;
        if (node.getLocationNode() != null) {
			wrapped = (FedoraQuery) node.getLocationNode().accept(this, root);
        }

        return wrapped;
    }

	/**
	 * Visits an OR-type query node.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 * @return a <code>String</code> array containing two elements:
	 * a list of properties referenced and filter expression in SPARQL
	 */
    public Object visit(OrQueryNode node, Object data) 
		throws RepositoryException 
	{
		List<String> uriList = new ArrayList<String>();
		String query = "";
		String uri = "";
        Object [] result = node.acceptOperands(this, null);
        for (int i = 0; i < result.length; i++) {
            String [] operand = (String []) result[i];
			if (i > 0) {
				query += " || ";
			}
			query += operand[1];

			String [] l = operand[0].split(",");

			for (String v : l) {
				if (! uriList.contains(v)) {
					uriList.add(v);
				}
			}
        }

		for (int i = 0; i < uriList.size(); ++i) {
			if (i > 0) {
				uri += ",";
			}
			uri += uriList.get(i);
		}

        return new String [] {uri, query};
    }

	/**
	 * Visits an AND-type query node.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 * @return a <code>String</code> array containing two elements:
	 * a list of properties referenced and filter expression in SPARQL
	 */
    public Object visit(AndQueryNode node, Object data) 
		throws RepositoryException 
	{
		List<String> uriList = new ArrayList<String>();
		String query = "";
		String uri = "";
        Object [] result = node.acceptOperands(this, null);
        for (int i = 0; i < result.length; i++) {
            String [] operand = (String []) result[i];
			if (i > 0) {
				query += " && ";
			}
			query += operand[1];

			String [] l = operand[0].split(",");

			for (String v : l) {
				if (! uriList.contains(v)) {
					uriList.add(v);
				}
			}
        }

		for (int i = 0; i < uriList.size(); ++i) {
			if (i > 0) {
				uri += ",";
			}
			uri += uriList.get(i);
		}

        return new String [] {uri, query};
    }

	/**
	 * Visits a NOT-type query node. Not implemented.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 */
    public Object visit(NotQueryNode node, Object data) 
		throws RepositoryException 
	{
		return null;
    }

	/**
	 * Visits an exact query node. Not implemented.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 */
    public Object visit(ExactQueryNode node, Object data) 
	{
        String field = "";
        String value = "";
        try {
            field = resolver.getJCRName(node.getPropertyName());
            value = resolver.getJCRName(node.getValue());
        } catch (NamespaceException e) {
            // will never happen, prefixes are created when unknown
        }

		log.debug("field: " + field);
		log.debug("value: " + value);

		return null;
    }

	/**
	 * Visits a node type query node.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 * @return a <code>String</code> array containing two elements:
	 * the property referenced and filter expression in SPARQL
	 */
    public Object visit(NodeTypeQueryNode node, Object data) 
	{
		Name field = node.getPropertyName();
		Name value = node.getValue();
		String filter;
		log.debug("field: " + field.getNamespaceURI() + "/" + 
				  field.getLocalName() + ", value: " + 
				  value.getNamespaceURI() + "/" + value.getLocalName());

		filter =  "regex($" + field.getLocalName() + ", '%57" + "\\\\{" + 
			value.getNamespaceURI() + "\\\\}" + value.getLocalName() + "$')";

		return new String [] {
			field.getNamespaceURI() + "/" + field.getLocalName(),
			filter
		};
    }

	/**
	 * Visits a text search query node. Not implemented.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 */
    public Object visit(TextsearchQueryNode node, Object data) 
	{
		return null;
    }

	/**
	 * Visits the path query node. All the location steps are handled
	 * here instead of in the <code>visit</code> method for
	 * <code>LocationStepQueryNode</code>. Each location step
	 * is resolved and then triggers the operations in
	 * {@link edu.northwestern.jcr.adapter.fedora.query.FedoraQuery} 
	 * which generates the SPARQL expressions and executes.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 * @return the <code>FedoraQuery</code> object
	 */
    public Object visit(PathQueryNode node, Object data) 
		throws RepositoryException 
	{
		FedoraQuery query = new FedoraQuery();
		int type;
		String name;
        LocationStepQueryNode[] steps = node.getPathSteps();

		String orderbyVariables = "";
		String orderbyClause = "";
		String namespaceURI;
		String localName;
		QueryRootNode root = (QueryRootNode) node.getParent();
        OrderQueryNode orderNode = root.getOrderNode();

        OrderQueryNode.OrderSpec[] orderSpecs;
        if (orderNode != null) {
            orderSpecs = orderNode.getOrderSpecs();
        } else {
            orderSpecs = new OrderQueryNode.OrderSpec[0];
        }
        Name[] orderProperties = new Name[orderSpecs.length];
        boolean[] ascSpecs = new boolean[orderSpecs.length];
        for (int i = 0; i < orderSpecs.length; i++) {
            orderProperties[i] = orderSpecs[i].getProperty();
            ascSpecs[i] = orderSpecs[i].isAscending();
			log.debug(orderProperties[i] + ", " + ascSpecs[i]);

			namespaceURI = orderProperties[i].getNamespaceURI();
			localName = orderProperties[i].getLocalName();

			if (namespaceURI.equals("")) {
				namespaceURI = "http://sling.apache.org/jcr/sling/1.0";
			}

			orderbyVariables += "$s <" + namespaceURI + "/" + localName + 
				"> $" + localName + ". ";
			if (i == 0) {
				orderbyClause += "order by ";
			}

			if (ascSpecs[i]) {
				orderbyClause += "asc(";
			}
			else {
				orderbyClause += "dsc(";
			}

			orderbyClause += "$" + localName + ") ";
        }


		for (int i = 0; i < steps.length; i++) {
			Name nameTest = steps[i].getNameTest();
			String propertyURI;
			String nameURI;

			if (steps[i] instanceof DerefQueryNode) {
				// deref function

				namespaceURI = 
					((DerefQueryNode) steps[i]).getRefProperty().getNamespaceURI();
				localName = 
					((DerefQueryNode) steps[i]).getRefProperty().getLocalName();

				if (namespaceURI.equals("")) {
					namespaceURI = "http://sling.apache.org/jcr/sling/1.0";
				}

				propertyURI = namespaceURI + "/" + localName;

				if (nameTest == null) {
					nameURI = "*";
				}
				else {
					namespaceURI = nameTest.getNamespaceURI();
					localName = nameTest.getLocalName();

					if (namespaceURI.equals("")) {
						namespaceURI = "http://sling.apache.org/jcr/sling/1.0";
					}

					// use local name only
					nameURI = localName;
				}

				// put property URI as filter
				query.addStep(nameURI, FedoraQuery.DEREF,
							  propertyURI);
				query.execute();
				continue;
			}

			if (nameTest != null &&
				nameTest.getNamespaceURI().equals("") &&
				nameTest.getLocalName().equals("")) {
				// jcr:root
				query.addStep("/", FedoraQuery.EXACT, null);
				continue;
			}

			if (nameTest != null) {
				name = nameTest.getLocalName();		 
			}
			else {
				name = "*";
			}

			if (steps[i].getIncludeDescendants()) {
				type = FedoraQuery.DESCENDANTS;
			}
			else if (nameTest != null) {
				type = FedoraQuery.EXACT;
			}
			else {
				// wildcard
				type = FedoraQuery.CHILDREN;
			}


			String filter = null;
			String uri = null;
			String prefix = "";
			QueryNode[] pred = steps[i].getPredicates();
			if (pred != null) {
				log.debug("number of predicates: " + pred.length);
				for (int j = 0; j < pred.length; ++j) {
					String [] s = (String []) pred[j].accept(this, null);

					if (j == 0) {
						filter = "";
					}
					else {
						filter += " && ";
					}

					uri = s[0];
					filter += "(" + s[1] + ")";

					String [] parts = uri.split(",");
					for (String v : parts) {
						int index = v.lastIndexOf("/");
						localName = v.substring(index + 1);
						prefix = "$s <" + v + "> $" + localName + ". " + prefix;
					}
				}

				if (pred.length > 0) {
					filter = prefix + " FILTER( " + filter + " ) ";
				}
			}

			if (i == steps.length - 1 && !orderbyVariables.equals("")) {
				// add order by clause
				filter += orderbyVariables;
			}
			
			log.debug("filter: " + filter);
 
			query.addStep(name, type, filter);
			query.execute();
		}
		
		return query;
    }

	/**
	 * Visits the location step query node. Not used since all the 
	 * location steps are handled in the <code>visit</code> method for
	 * <code>PathQueryNode</code>.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 */
    public Object visit(LocationStepQueryNode node, Object data) 
		throws RepositoryException 
	{
		return null;
    }

	/**
	 * Visits the dereference query node. Not used since all the 
	 * dereference functions are resolved in
	 * {@link edu.northwestern.jcr.adapter.fedora.query.FedoraQuery}.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 */
    public Object visit(DerefQueryNode node, Object data) 
		throws RepositoryException 
	{
		return null;
    }

	/**
	 * Visits a relation query node. Currently only three operators
	 * are supported: equal, not equal and not null.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 * @return a <code>String</code> array containing two elements:
	 * the property referenced and filter expression in SPARQL
	 */
    public Object visit(RelationQueryNode node, Object data) 
		throws RepositoryException 
	{
        Name propertyName = 
			node.getRelativePath().getNameElement().getName();
        String [] stringValues = null;

		if (node.getOperation() != QueryConstants.OPERATION_NOT_NULL) {
			stringValues = 
				getStringValues(propertyName, node.getStringValue());
		}
		String namespaceURI = propertyName.getNamespaceURI();
		String localName = propertyName.getLocalName();
		String op;
		String filter;

		if (namespaceURI.equals("")) {
			namespaceURI = "http://sling.apache.org/jcr/sling/1.0";
		}

		if (node.getOperation() == QueryConstants.OPERATION_EQ_GENERAL
			|| node.getOperation() == QueryConstants.OPERATION_EQ_VALUE) {
			op = "=";
			filter = "regex($" + localName + ", '%57" + stringValues[0] + "$')";
		}
		else if (node.getOperation() == QueryConstants.OPERATION_NE_GENERAL
				 || node.getOperation() == QueryConstants.OPERATION_NE_VALUE) {
			op = "<>";
			filter = "!regex($" + localName + ", '%57" + stringValues[0] + "$')";
		}
		else if (node.getOperation() == QueryConstants.OPERATION_NOT_NULL) {
			op = "NN";
			filter = "bound($" + localName + ")";
		}
		else {
			op = "";
			filter = "";
		}

		return new String [] {
			namespaceURI + "/" + localName,
			filter
		};
    }

	/**
	 * Visits the order query node. Not used since all the 
	 * ordering are implemented in
	 * {@link edu.northwestern.jcr.adapter.fedora.query.SearchIndex}.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 */
    public Object visit(OrderQueryNode node, Object data) 
	{
        return data;
    }

	/**
	 * Visits a property function query node. Not implemented.
	 *
	 * @param node the query node
	 * @param data not used in this implementation
	 */
    public Object visit(PropertyFunctionQueryNode node, Object data) 
	{
        return data;
    }

    //---------------------------< internal >-----------------------------------

    /**
     * Returns an array of String values to be used as a term to lookup the search index
     * for a String <code>literal</code> of a certain property name. This method
     * will lookup the <code>propertyName</code> in the node type registry
     * trying to find out the {@link javax.jcr.PropertyType}s.
     * If no property type is found looking up node type information, this
     * method will guess the property type.
     *
     * @param propertyName the name of the property in the relation.
     * @param literal      the String literal in the relation.
     * @return the String values to use as term for the query.
     */
    private String[] getStringValues(Name propertyName, String literal) {
        PropertyTypeRegistry.TypeMapping[] types = propRegistry.getPropertyTypes(propertyName);
        List values = new ArrayList();
        for (int i = 0; i < types.length; i++) {
            switch (types[i].type) {
                case PropertyType.NAME:
                    // try to translate name
                    try {
                        Name n = session.getQName(literal);
                        values.add(nsMappings.translatePropertyName(n));
                        log.debug("Coerced " + literal + " into NAME.");
                    } catch (NameException e) {
                        log.debug("Unable to coerce '" + literal + "' into a NAME: " + e.toString());
                    } catch (NamespaceException e) {
                        log.debug("Unable to coerce '" + literal + "' into a NAME: " + e.toString());
                    }
                    break;
                case PropertyType.PATH:
                    // try to translate path
                    try {
                        Path p = session.getQPath(literal);
                        values.add(resolver.getJCRPath(p));
                        log.debug("Coerced " + literal + " into PATH.");
                    } catch (NameException e) {
                        log.debug("Unable to coerce '" + literal + "' into a PATH: " + e.toString());
                    } catch (NamespaceException e) {
                        log.debug("Unable to coerce '" + literal + "' into a PATH: " + e.toString());
                    }
                    break;
                case PropertyType.DATE:
                    // try to parse date
                    Calendar c = ISO8601.parse(literal);
                    if (c != null) {
                        values.add(DateField.timeToString(c.getTimeInMillis()));
                        log.debug("Coerced " + literal + " into DATE.");
                    } else {
                        log.debug("Unable to coerce '" + literal + "' into a DATE.");
                    }
                    break;
                case PropertyType.DOUBLE:
                    // try to parse double
                    try {
                        double d = Double.parseDouble(literal);
                        values.add(DoubleField.doubleToString(d));
                        log.debug("Coerced " + literal + " into DOUBLE.");
                    } catch (NumberFormatException e) {
                        log.debug("Unable to coerce '" + literal + "' into a DOUBLE: " + e.toString());
                    }
                    break;
                case PropertyType.LONG:
                    // try to parse long
                    try {
                        long l = Long.parseLong(literal);
                        values.add(LongField.longToString(l));
                        log.debug("Coerced " + literal + " into LONG.");
                    } catch (NumberFormatException e) {
                        log.debug("Unable to coerce '" + literal + "' into a LONG: " + e.toString());
                    }
                    break;
                case PropertyType.STRING:
                    values.add(literal);
                    log.debug("Using literal " + literal + " as is.");
                    break;
            }
        }
        if (values.size() == 0) {
            // use literal as is then try to guess other types
            values.add(literal);
	
            // try to guess property type
            if (literal.indexOf('/') > -1) {
                // might be a path
                try {
                    values.add(resolver.getJCRPath(session.getQPath(literal)));
                    log.debug("Coerced " + literal + " into PATH.");
                } catch (Exception e) {
                    // not a path
                }
            }
            if (XMLChar.isValidName(literal)) {
                // might be a name
                try {
                    Name n = session.getQName(literal);
                    values.add(nsMappings.translatePropertyName(n));
                    log.debug("Coerced " + literal + " into NAME.");
                } catch (Exception e) {
                    // not a name
                }
            }
            if (literal.indexOf(':') > -1) {
                // is it a date?
                Calendar c = ISO8601.parse(literal);
                if (c != null) {
                    values.add(DateField.timeToString(c.getTimeInMillis()));
                    log.debug("Coerced " + literal + " into DATE.");
                }
            } else {
                // long or double are possible at this point
                try {
                    values.add(LongField.longToString(Long.parseLong(literal)));
                    log.debug("Coerced " + literal + " into LONG.");
                } catch (NumberFormatException e) {
                    // not a long
                    // try double
                    try {
                        values.add(DoubleField.doubleToString(Double.parseDouble(literal)));
                        log.debug("Coerced " + literal + " into DOUBLE.");
                    } catch (NumberFormatException e1) {
                        // not a double
                    }
                }
            }
        }
        // if still no values use literal as is
        if (values.size() == 0) {
            values.add(literal);
            log.debug("Using literal " + literal + " as is.");
        }
        return (String[]) values.toArray(new String[values.size()]);
    }
}
