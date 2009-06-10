import java.io.*;
import java.util.*;

import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import org.apache.log4j.PropertyConfigurator;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;

import javax.jcr.*;
import javax.jcr.nodetype.*;
import javax.jcr.version.OnParentVersionAction;

/**	A play-pen for various quick and dirty tests.
 */

public class Test {

	private static Repository repository;
	
	private static Session session;
	
	private static void initLog4j ()
		throws Exception
	{
		File configFile = new File("log4j.properties");
		InputStream configFileInputStream = new FileInputStream(configFile);
		Properties log4jProperties = new Properties();
		log4jProperties.load(configFileInputStream);
		configFileInputStream.close();
		PropertyConfigurator.configure(log4jProperties);
	}
	
	private static void initRepository ()
		throws Exception
	{
		File configFile = new File("repository.xml");
		InputStream configFileInputStream = new FileInputStream(configFile);
		InputSource configFileInputSource = new InputSource(configFileInputStream);
		File repositoryHome = new File("jackrabbit");
		RepositoryConfig config = 
			RepositoryConfig.create(configFileInputSource, repositoryHome.getAbsolutePath());
		repository = RepositoryImpl.create(config);
		configFileInputStream.close();
	}
	
	private static void login (String workspace, String username, String password)
		throws Exception
	{
		System.out.println("");
		System.out.println("===== Login =====");
		Credentials credentials = new SimpleCredentials(username, password.toCharArray());
		session = repository.login(credentials, workspace);
		System.out.println("Logged in to workspace " + 
			workspace +
			" as user " +
			username +
			" with password " +
			password);
	}
	private static void dumpProperties (Node node, int level) 
		throws Exception
	{
		for (PropertyIterator it = node.getProperties(); it.hasNext(); ) {
			Property prop = it.nextProperty();
			for (int i = 0; i < level; i++) System.out.print(" ");
			System.out.print("Property: ");
			System.out.print(prop.getName());
			System.out.print(": ");
			System.out.print(PropertyType.nameFromValue(prop.getType()));
			System.out.println();
		}
	}
	
	private static void dumpChildren (Node node, int level) 
		throws Exception
	{
		for (NodeIterator it = node.getNodes(); it.hasNext(); ) {
			Node child = it.nextNode();
			dumpNode(child, level);
		}
	}
	
	private static void dumpNode (Node node, int level)
		throws Exception
	{
		for (int i = 0; i < level; i++) System.out.print(" ");
		System.out.print("Node: ");
		System.out.print(node.getName());
		System.out.print(": ");
		System.out.print(node.getPrimaryNodeType().getName());
		System.out.println();
		dumpProperties(node, level+3);
		dumpChildren(node, level+3);
	}
	
	private static void dumpTree ()
		throws Exception
	{
		System.out.println("");
		System.out.println("===== Item Tree =====");
		Node root = session.getRootNode();
		dumpNode(root, 0);
	}
	
	private static void dumpNodeTypes ()
		throws Exception
	{
		System.out.println("");
		System.out.println("===== Node Types =====");
		NodeTypeManager mgr = session.getWorkspace().getNodeTypeManager();
		for (NodeTypeIterator it = mgr.getAllNodeTypes(); it.hasNext(); ) {
			NodeType nt = it.nextNodeType();
			System.out.print(nt.getName());
			System.out.print(" (");
			System.out.print(nt.isMixin() ? "mixin" : "primary");
			System.out.println(")");
		}
	}
	
	private static void dumpNamespaceRegistry ()
		throws Exception
	{
		System.out.println("");
		System.out.println("===== Namespace Registry =====");
		NamespaceRegistry nr = session.getWorkspace().getNamespaceRegistry();
		String [] prefixes = nr.getPrefixes();
		System.out.println(prefixes.length + " namespace registry entries:");
		for (String prefix : prefixes) {
			String uri = nr.getURI(prefix);
			System.out.print("'" + prefix + "'");
			System.out.print(" --> ");
			System.out.print("'" + uri + "'");
			System.out.println();
		}
	}
	
	private static void exportDocView () 
		throws Exception
	{
		System.out.println("");
		System.out.println("===== Export Doc View =====");
		String workspaceName = session.getWorkspace().getName();
		String fileName = workspaceName + ".xml";
		File file = new File(fileName);
		FileOutputStream fos = new FileOutputStream(file);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		session.exportDocumentView("/", bos, false, false);
		bos.close();
		System.out.println("Workspace " +
			workspaceName +
			" exported in document view to file " +
			fileName);
	}
	
	private static void dumpDocViewNamespaces ()
		throws Exception
	{
		System.out.println("");
		System.out.println("===== Doc View Root Element Attributes =====");
		File file = new File("default.xml");
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(fis);
		StreamSource source = new StreamSource(bis);
		DOMResult result = new DOMResult();
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		transformer.transform(source, result);
		Document doc = (Document)result.getNode();
        Element root = doc.getDocumentElement();
        NamedNodeMap attrs = root.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
        	Attr attribute = (Attr)attrs.item(i);
        	System.out.println("name = '" + attribute.getName() + "'");
        	System.out.println("   prefix = '" + attribute.getPrefix() + "'");
        	System.out.println("   namespace URI = '" + attribute.getNamespaceURI() + "'");
        	System.out.println("   local name = '" + attribute.getLocalName() + "'");
        	System.out.println("   value = '" + attribute.getValue() + "'");
        }
        System.out.println("----- AttributeSeparator Bug Fix Test");
        Properties nameSpaces = new AttributeSeparator(root).getNsAttrs();
        for (Enumeration e = nameSpaces.keys(); e.hasMoreElements(); ) {
        	String prefix = (String)e.nextElement();
        	String URI = nameSpaces.getProperty(prefix);
        	System.out.println("'" + prefix + "' --> '" + URI + "'");
        }
	}
	
	public static void testPredefinedNodeTypeFix () 
		throws Exception
	{
		System.out.println("");
		System.out.println("===== Test Predefined Node Type Fix =====");
		testPredefinedNodeType("nt:file");
	}

	public static void main (String[] args) {
		try {
			initLog4j();
			initRepository();
			login("default", "xxx", "xxxx");
			//dumpNamespaceRegistry();
			//dumpNodeTypes();
			//dumpTree();
			//exportDocView();
			//login("test", "xxx", "xxx");
			//exportDocView();
			//dumpDocViewNamespaces();
			testPredefinedNodeTypeFix();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//	Code from here to the end of this file copied from the TCK source.
	
	//	From the TCK unit test ExportDocView, with bug fixed.
	
    private static class AttributeSeparator {
        private static final String xmlnsURI = "http://www.w3.org/2000/xmlns/";
        private static final String xmlnsPrefix = "xmlns";

        Element elem;
        NamedNodeMap attrs;
        Properties nsAttrs;
        Properties nonNsAttrs;

        AttributeSeparator(Element elem) {
            this.elem = elem;
            nsAttrs = new Properties();
            nonNsAttrs = new Properties();
            attrs = elem.getAttributes();
            separateAttrs();
        }

        public Properties getNsAttrs() {
            return nsAttrs;
        }

        public Properties getNonNsAttrs() {
            return nonNsAttrs;
        }

        private void separateAttrs() {
            for (int i = 0; i < attrs.getLength(); i++) {
                Attr attribute = (Attr) attrs.item(i);
                String URI = attribute.getNamespaceURI();
                if (xmlnsURI.equals(URI)) {
                    nsAttrs.put(attribute.getLocalName(), attribute.getValue());
                } else if (URI == null && xmlnsPrefix.equals(attribute.getName())) {
                	// empty prefix
                	nsAttrs.put("", attribute.getValue());
                } else {
                    nonNsAttrs.put(attribute.getName(), attribute.getValue());
                }
            }
        }
    }
    
    //	From the TCK unit test PredefinedNodeTypeTest, with bug fixed.

    /**
     * Tests that the named node type matches the JSR 170 specification.
     * The test is performed by genererating a node type definition spec
     * string in the format used by the JSR 170 specification, and comparing
     * the result with a static spec file extracted from the specification
     * itself.
     * <p>
     * Note that the extracted spec files are not exact copies of the node
     * type specification in the JSR 170 document. Some formatting and
     * ordering changes have been made to simplify the test code, but the
     * semantics remain the same.
     *
     * @param name node type name
     */
    private static void testPredefinedNodeType(String name) 
    	throws Exception
    {
		StringBuffer spec = new StringBuffer();
		String fileName = name.replace(':', '-') + ".txt";
		Reader reader = new InputStreamReader(new FileInputStream(fileName));
		for (int ch = reader.read(); ch != -1; ch = reader.read()) {
			spec.append((char) ch);
		}

		NodeType type = session.getWorkspace().getNodeTypeManager().getNodeType(name);
		
		String expected = normalizeLineSeparators(spec.toString());
		String got = normalizeLineSeparators(getNodeTypeSpec(type));
		
		if (expected.equals(got)) {
			System.out.println("test passed");
		} else {
			System.out.println("test failed");
		}
    }
    
    private static String normalizeLineSeparators (String str) {
    	return str.replaceAll("[\\n\\r]*", "\\n");
    }

    /**
     * Creates and returns a spec string for the given node type definition.
     * The returned spec string follows the node type definition format
     * used in the JSR 170 specification.
     *
     * @param type node type definition
     * @return spec string
     * @throws RepositoryException on repository errors
     */
    private static String getNodeTypeSpec(NodeType type)
            throws RepositoryException {
        StringWriter buffer = new StringWriter();

        PrintWriter writer = new PrintWriter(buffer);
        writer.println("NodeTypeName");
        writer.println("  " + type.getName());
        writer.println("Supertypes");
        NodeType[] supertypes = type.getDeclaredSupertypes();
        if (supertypes.length > 0) {
            Arrays.sort(supertypes, NODE_TYPE_COMPARATOR);
            for (int i = 0; i < supertypes.length; i++) {
                writer.println("  " + supertypes[i].getName());
            }
        } else {
            writer.println("  []");
        }
        writer.println("IsMixin");
        writer.println("  " + type.isMixin());
        writer.println("HasOrderableChildNodes");
        writer.println("  " + type.hasOrderableChildNodes());
        writer.println("PrimaryItemName");
        writer.println("  " + type.getPrimaryItemName());
        NodeDefinition[] nodes = type.getDeclaredChildNodeDefinitions();
        Arrays.sort(nodes, ITEM_DEF_COMPARATOR);
        for (int i = 0; i < nodes.length; i++) {
            writer.print(getChildNodeDefSpec(nodes[i]));
        }
        PropertyDefinition[] properties = type.getDeclaredPropertyDefinitions();
        Arrays.sort(properties, ITEM_DEF_COMPARATOR);
        for (int i = 0; i < properties.length; i++) {
            writer.print(getPropertyDefSpec(properties[i]));
        }

        return buffer.toString();
    }

    /**
     * Creates and returns a spec string for the given node definition.
     * The returned spec string follows the child node definition format
     * used in the JSR 170 specification.
     *
     * @param node child node definition
     * @return spec string
     */
    private static String getChildNodeDefSpec(NodeDefinition node) {
        StringWriter buffer = new StringWriter();

        PrintWriter writer = new PrintWriter(buffer);
        writer.println("ChildNodeDefinition");
        if (node.getName().equals("*")) {
            writer.println("  Name \"*\"");
        } else {
            writer.println("  Name " + node.getName());
        }
        writer.print("  RequiredPrimaryTypes [");
        NodeType[] types = node.getRequiredPrimaryTypes();
        Arrays.sort(types, NODE_TYPE_COMPARATOR);
        for (int j = 0; j < types.length; j++) {
            if (j > 0) {
                writer.print(',');
            }
            writer.print(types[j].getName());
        }
        writer.println("]");
        if (node.getDefaultPrimaryType() != null) {
            writer.println("  DefaultPrimaryType "
                    + node.getDefaultPrimaryType().getName());
        } else {
            writer.println("  DefaultPrimaryType null");
        }
        writer.println("  AutoCreated " + node.isAutoCreated());
        writer.println("  Mandatory " + node.isMandatory());
        writer.println("  OnParentVersion "
                + OnParentVersionAction.nameFromValue(node.getOnParentVersion()));
        writer.println("  Protected " + node.isProtected());
        writer.println("  SameNameSiblings " + node.allowsSameNameSiblings());

        return buffer.toString();
    }

    /**
     * Creates and returns a spec string for the given property definition.
     * The returned spec string follows the property definition format
     * used in the JSR 170 specification.
     *
     * @param property property definition
     * @return spec string
     * @throws RepositoryException on repository errors
     */
    private static String getPropertyDefSpec(PropertyDefinition property)
            throws RepositoryException {
        StringWriter buffer = new StringWriter();

        PrintWriter writer = new PrintWriter(buffer);
        writer.println("PropertyDefinition");
        if (property.getName().equals("*")) {
            writer.println("  Name \"*\"");
        } else {
            writer.println("  Name " + property.getName());
        }
        String type = PropertyType.nameFromValue(property.getRequiredType());
        writer.println("  RequiredType " + type.toUpperCase());
        writer.print("  ValueConstraints [");
        String[] constraints = property.getValueConstraints();
        for (int i = 0; i < constraints.length; i++) {
            if (i > 0) {
                writer.print(',');
            }
            writer.print(constraints[i]);
        }
        writer.println("]");
        Value[] values = property.getDefaultValues();
        if (values != null && values.length > 0) {
            writer.print("  DefaultValues [");
            for (int j = 0; j < values.length; j++) {
                if (j > 0) {
                    writer.print(',');
                }
                writer.print(values[j].getString());
            }
            writer.println("]");
        } else {
            writer.println("  DefaultValues null");
        }
        writer.println("  AutoCreated " + property.isAutoCreated());
        writer.println("  Mandatory " + property.isMandatory());
        String action = OnParentVersionAction.nameFromValue(
                property.getOnParentVersion());
        writer.println("  OnParentVersion " + action);
        writer.println("  Protected " + property.isProtected());
        writer.println("  Multiple " + property.isMultiple());

        return buffer.toString();
    }

    /**
     * Comparator for ordering property and node definition arrays. Item
     * definitions are ordered by name, with the wildcard item definition
     * ("*") ordered last.
     */
    private static final Comparator ITEM_DEF_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            ItemDefinition ida = (ItemDefinition) a;
            ItemDefinition idb = (ItemDefinition) b;
            if (ida.getName().equals("*") && !idb.getName().equals("*")) {
                return 1;
            } else if (!ida.getName().equals("*") && idb.getName().equals("*")) {
                return -1;
            } else {
                return ida.getName().compareTo(idb.getName());
            }
        }
    };

    /**
     * Comparator for ordering node type arrays. Node types are ordered by
     * name, with all primary node types ordered before mixin node types.
     */
    private static final Comparator NODE_TYPE_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            NodeType nta = (NodeType) a;
            NodeType ntb = (NodeType) b;
            if (nta.isMixin() && !ntb.isMixin()) {
                return 1;
            } else if (!nta.isMixin() && ntb.isMixin()) {
                return -1;
            } else {
                return nta.getName().compareTo(ntb.getName());
            }
        }
    };
	
}