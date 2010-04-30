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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedInputStream;

import java.util.Properties;
import java.util.Calendar;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NodeIterator;
import javax.jcr.Workspace;
import javax.jcr.Value;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.NamespaceException;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.xml.sax.InputSource;
import org.apache.log4j.PropertyConfigurator;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TransientRepository;

import edu.northwestern.jcr.adapter.fedora.persistence.FedoraConnector;
import edu.northwestern.jcr.adapter.fedora.persistence.FedoraConnectorAPIX;
import edu.northwestern.jcr.adapter.fedora.persistence.FedoraConnectorREST;

/**
 *	A play-pen for various quick and dirty tests.
 */

public class Test {

	private static Repository repository;
	
	private static Session session;

	private static FedoraConnector fc;

	private static final int bulkSize = 16384;
	
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
		InputStream configFileInputStream = 
			new FileInputStream(configFile);
		InputSource configFileInputSource = 
			new InputSource(configFileInputStream);
		File repositoryHome = new File("jackrabbit");
		RepositoryConfig config = 
			RepositoryConfig.create(configFileInputSource, 
									repositoryHome.getAbsolutePath());
		// repository = RepositoryImpl.create(config);
		repository = new TransientRepository(config);
		configFileInputStream.close();
	}

	private static void initFedoraConnector()
	{
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
	
	private static Session login (String workspace, String username, 
							   String password)
		throws Exception
	{
		System.out.println("");
		System.out.println("===== Login =====");
		Credentials credentials = 
			new SimpleCredentials(username, password.toCharArray());
		session = repository.login(credentials, workspace);
		System.out.println("Logged in to workspace " + 
			workspace +
			" as user " +
			username +
			" with password " +
			password);

		session.setNamespacePrefix("fedora", "http://www.fedora.info/");
		session.setNamespacePrefix("sling", "http://sling.apache.org/jcr/sling/1.0");
		try {
			// register namespace
			session.getWorkspace().getNamespaceRegistry().registerNamespace("dc", "http://purl.org/dc/elements/1.1");
		} catch (NamespaceException e) {

		}
		session.setNamespacePrefix("dc", "http://purl.org/dc/elements/1.1");

		return session;
	}

	private static void writeStream(InputStream is, String fileName)
	{
		try {
			File f = new File(fileName);
			OutputStream out = new FileOutputStream(f);
			byte buf [] = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			is.close();
			System.out.println("Image file is created");
		}
		catch (IOException e) {
			
		}
	}

	private static void testWrite() throws Exception
	{
		Node root, test;

		initRepository();

		try {
			session = login("default", "superuser", "");
			root = session.getRootNode();

			/////////////////////////////////////////////////////////////
			// write digital objects and streams to Fedora

			// System.out.println(root.getProperty("jcr:primaryType").getString());

			try {
				test = root.getNode("test_1");
			} catch (PathNotFoundException e) {
				test = root.addNode("test_1"); // , "nt:unstructured");
			}
			

			try {
				test = test.getNode("test_2");
			} catch (PathNotFoundException e) {
				test = test.addNode("test_2", "nt:unstructured");
			}
			
			// test.setProperty("image", new FileInputStream(new File("test.jpg")));
			test = test.addNode("image", "nt:file");
			
			test = test.addNode("jcr:content", "nt:resource");
			// test.setProperty("jcr:data", "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"><dc:title>Updated DC from JCR</dc:title><dc:identifier>test:test</dc:identifier></oai_dc:dc>");
			// test.setProperty("jcr:data", session.getValueFactory().createValue("this is the content", PropertyType.STRING));
			ValueFactory factory = session.getValueFactory();
			test.setProperty("jcr:data", factory.createValue(factory.createBinary(new FileInputStream(new File("resource/test.jpg")))));
			test.setProperty("jcr:mimeType", "image/jpeg");
			test.setProperty("jcr:lastModified", "2009-07-08T00:00:00.000Z");
			
			session.save();
		} finally {
			session.logout();
		}
	}

	private static void testWriteABCD() throws Exception
	{
		Node root, test;

		initRepository();

		try {
			session = login("default", "superuser", "");
			root = session.getRootNode();

			/////////////////////////////////////////////////////////////
			// write digital objects and streams to Fedora

			try {
				test = root.getNode("a");
			} catch (PathNotFoundException e) {
				test = root.addNode("a");
			}
			
			try {
				test.getNode("c");
			} catch (PathNotFoundException e) {
				test.addNode("c");
			}
			
			try {
				test.getNode("d");
			} catch (PathNotFoundException e) {
				test.addNode("d");
			}

			try {
				root.getNode("b");
			} catch (PathNotFoundException e) {
				root.addNode("b");
			}
			
			session.save();
		} finally {
			session.logout();
		}
	}

	private static void testRead() throws Exception
	{
		Node root, test;

		initRepository();

		try {
			session = login("default", "superuser", "");
			root = session.getRootNode();

			// test = root.getNode("testdata/node");
			test = root.getNode("nonexistingnode");
		} finally {
			if (session != null) {
				session.logout();
			}
		}
	}

	private static void testDelete() throws Exception
	{
		Node root, test;

		try {
			session = login("default", "superuser", "");
			root = session.getRootNode();

			/////////////////////////////////////////////////////////////
			// delete data streams from Fedora
			test = root.getNode("test_writer/image");
			test.remove();
			
			session.save();
			/////////////////////////////////////////////////////////////
		} finally {
			session.logout();
		}
	}

	private static void testImport() throws Exception
	{
		Node root, test;
		
		initRepository();

		try {
			session = login("default", "superuser", "");

			// File file = new File("testdata-sysview.xml");
			File file = new File("resource/testroot.xml");
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			session.importXML("/", bis, 
							  ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

			file = new File("resource/test-import.xml");
			fis = new FileInputStream(file);
			bis = new BufferedInputStream(fis);
			session.importXML("/", bis, 
							  ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
			
			session.save();
			/////////////////////////////////////////////////////////////
		} finally {
			if (session != null) {
				session.logout();
			}
		}
	}

	private static void getCanberraContent() throws Exception
	{
		Node root, test1;
		int i;
		InputStream is;

		try {
			session = login("default", "superuser", "");
			root = session.getRootNode();

			/////////////////////////////////////////////////////////////
			// read digital objects and streams from Fedora

			for (i = 1; i <= 12; ++i) {
				test1 = root.getNode("canberra_" + i + "/DC/jcr:content");
				// Node test2 = root.getNode("test_image1");
				System.out.println(test1.getProperty("jcr:data").getString());
			
				test1 = root.getNode("canberra_" + i + "/image/jcr:content");
				is = test1.getProperty("jcr:data").getBinary().getStream();
				writeStream(is, "canberra/canberra_" + i + ".jpg");
			
				test1 = root.getNode("canberra_" + i + "/thumb/jcr:content");
				is = test1.getProperty("jcr:data").getBinary().getStream();
				writeStream(is, "canberra/canberra_" + i + "_thumb.jpg");
			
				if (i == 6) {
					i = 8;
				}
			}			

			//////////////////////////////////////////////////////////////

		} finally {
			session.logout();
		}
	}

	private static void testMultipleSessions()
		throws Exception
	{
		Node root, test1;
		Session session2 = null;

		initRepository();

		try {
			session = login("default", "superuser", "");
			session2 = login("default", "superuser", "");
			
			if (session == session2) {
				System.out.println("same!!!");
			}
		} finally {
			session.logout();
			session2.logout();
		}
	}


	private static void testListProperty()
	{
		String [] pList;

		initFedoraConnector();

		// fc.addProperty("test:2", "http://sling.apache.org/jcr/sling/1.0/test", "abc");
		pList = fc.listProperties("canberra:2");

		for (String property : pList) {
			System.out.println(property);
		}
	}

	private static void testGSearch()
	{
		String [] pList;

		initFedoraConnector();

		pList = fc.searchFullText("rss");

		for (String property : pList) {
			System.out.println(property);
		}
	}

	private static void testAddProperty()
	{
		initFedoraConnector();

		// fc.addProperty("test:2", "http://sling.apache.org/jcr/sling/1.0/test", "abc");
		fc.addProperty("test:2", "http://www.jcp.org/jcr/1.0/primaryType", "defghi");
	}

	private static void testGetProperty()
	{
		String literal;

		initFedoraConnector();

		// literal = fc.getProperty("week1", "http://www.jcp.org/jcr/1.0/primaryType");
		literal = fc.getProperty("canberra:2", "http://purl.org/dc/elements/1.1/identifier");

		System.out.println(literal);
	}

	private static void testDeleteProperty()
	{
		String literal;

		initFedoraConnector();

		fc.deleteProperty("test:2", "http://sling.apache.org/jcr/sling/1.0/myProperty");
	}

	private static void testReadFedora()
	{
		String s = "";
		byte [] b;

		initFedoraConnector();

		b = fc.getDataStream("sling2:untitled_folder", "DC");
		try {
			s = new String(b, "UTF-8");
		} catch (Exception e) {
			System.err.println("Error creating String");
		}
		System.out.println(s);
	}

	private static void testAddPropertyJCR()
		throws Exception
	{
		Node root, test;

		initRepository();

		try {
			session = login("default", "superuser", "");
			root = session.getRootNode();

			test = root.getNode("test_1/test_2");

			// // test.setProperty("myProperty", "myValue3");
			// test.setProperty("double", 
			// 				 session.getValueFactory().createValue(4.0)); 
			// //, PropertyType.DOUBLE));
			// test.setProperty("long", 
			// 				 session.getValueFactory().createValue(90834953485278298L)); 
			// test.setProperty("calendar", 
			// 				 session.getValueFactory().createValue(Calendar.getInstance())); 
			// test.setProperty("boolean", 
			// 				 session.getValueFactory().createValue(false)); 
			
			test.setProperty("dc:title", "test2");

			session.save();
		} 
		finally {
			session.logout();
		}
	}

	private static void testGetPropertyJCR()
		throws Exception
	{
		Node root, test;
		Property property;
		String nodePath, propertyName;
		initRepository();

		try {
			session = login("default", "superuser", "");
			root = session.getRootNode();

			// nodePath = "testdata/docViewTest/bigNode";
			// propertyName = "propName2";
			// nodePath = "canberra_2";
			// nodePath = "test_1/test_2";
			nodePath = "changeme_13/HTML/jcr:content";
			// propertyName = "dc:description";
			// propertyName = "jcr:primaryType";
			// propertyName = "dc:title";
			// propertyName = "jcr:mimeType";
			propertyName = "jcr:data";

			test = root.getNode(nodePath);
			property = test.getProperty(propertyName);

			if (property.getDefinition().isMultiple()) {
				System.out.println("multiple!");
				System.out.println(property.getType());
				Value [] values = property.getValues();

				for (Value value : values) {
					System.out.println(value.getString());
				}
			}
			else {
				System.out.println("not multiple!");

				System.out.println(test.getProperty(propertyName).getString());
				
				// InputStream is = test.getProperty(propertyName).getBinary().getStream();
				// writeStream(is, "test.html");
			}
		} 
		finally {
			if (session != null) {
				session.logout();
			}
		}
	}

	private static void testReadJCR()
		throws Exception
	{
		Node root, test1;

		initRepository();

		try {
			session = login("default", "superuser", "");
			root = session.getRootNode();

			// test1 = root.getNode("testdata/node/myResource/DC/jcr:content");
			test1 = root.getNode("canberra_1/image/jcr:content");

			// System.out.println(test1.getProperty("jcr:data").getString());
			// System.out.println(test1.getProperty("jcr:encoding").getString());
			System.out.println(test1.getProperty("jcr:mimeType").getString());
		} finally {
			if (session != null) {
				session.logout();
			}
		}
	}

	private static void testGetNodesJCR()
		throws Exception
	{
		Node root, test1;

		initRepository();

		try {
			session = login("default", "superuser", "");
			root = session.getRootNode();

			test1 = root.getNode("canberra_1");

			NodeIterator it = test1.getNodes();
			int i = 0;
			System.out.println("number of items: " + it.getSize());

			while (it.hasNext()) {
				Node n = it.nextNode();

				i++;

				System.out.println();
				System.out.println("item " + i);
				System.out.println("path: " + n.getPath());
			}
		} finally {
			if (session != null) {
				session.logout();
			}
		}
	}

	private static void testBulkWriteFedora() throws Exception
	{
		initFedoraConnector();

		int i;

		for (i = 0; i < bulkSize; ++i) {
			fc.createObject("sling1:bulk" + i);
		}
	}

	private static void testBulkDelete() throws Exception
	{
		initFedoraConnector();

		int i;

		for (i = 0; i < bulkSize; ++i) {
			fc.deleteObject("sling1:bulk" + i);
		}
	}

	private static void testBulkWriteJCR()
		throws Exception
	{
		Node root, test1;
		int i;

		initRepository();

		try {
			session = login("default", "superuser", "");

			root = session.getRootNode();

			for (i = 0; i < bulkSize; ++i) {
				test1 = root.addNode("bulk" + i);
			}

			session.save();
		} finally {
			session.logout();
		}
	}

	private static void testQuery()
		throws Exception
	{
		Node root, test1;

		initRepository();

		try {
			session = login("default", "superuser", "");
			Workspace ws = session.getWorkspace();
			QueryManager qm = ws.getQueryManager();

			String [] languages = qm.getSupportedQueryLanguages();
			System.out.println("supported languages: ");
			for (String language : languages) {
				System.out.println(language);
			}

			Query q = qm.createQuery 
				(
				 // Exact path constraint
				 // "/jcr:root",
				 // "/test_1",
				 // "/fedora:test_1//fedora:test_3",
				 // "/testdata",
				 // "/canberra_1",
				 // "/canberra_12",
				 // "/fedora-system_ContentModel-3.0",
				 // "/testdata/node",
				 // "/testdata/query",
				 // "/testdata/element(node, nt:unstructured)",
				 // "/testdata/node/element(myResource, nt:resource)",
				 // "/testdata/node/element(myResource, nt:unstructured)",

				 // child nodes path constraint
				 // "/testdata/*",
				 // "/*",
				 // "/element(*, nt:unstructured)",
				 // "/element(*, nt:unstructured)[@boolean = 'true']",
				 // "/element(*, sling:Folder)",
				 // "/element(*, nt:resource)",
				 // "/testdata/node/*",
				 // "/testdata/node/element(*, nt:unstructured)",
				 // "/testdata/node/element(*, nt:resource)",
				 // "/*/node",
				 
				 // Descendants or self path constraint
				 // "//*",
				 // "//node",
				 // "//element(node, nt:unstructured)",
				 // "//element(node, nt:resource)",
				 // "//properties",
				 // "/testdata//*",
				 // "/testdata/node//*",
				 // "/testdata/node//element(*, nt:unstructured)",
				 // "/testdata/node//element(*, nt:resource)",
				 // "/testdata//element(*, nt:unstructured)",
				 // "/testdata//element(*, nt:resource)",
				 // "/testdata/query//*",
				 // "/testdata/property//*",
				 // "/testdata/docViewTest//*",
				 // "//*/node",
				 
				 // extensions
				 // "/jcr:root/testdata[@jcr:primaryType]",
				 // "//*[@boolean]",
				 // "/testdata/query",
				 // "/testdata/query[@jcr:primaryType]",
				 // "/jcr:root/testdata/query[@jcr:primaryType]",
				 // "//node/element(*, nt:unstructured)/jcr:deref(@author, 'person')/address",
				 // "/testdata/node/reference",
				 // "/testdata/node/reference/jcr:deref(@ref, '*')",
				 // "/testdata/node/reference/jcr:deref(@ref, 'myResource')",
				 // "/testdata/node/multiReference/jcr:deref(@ref, '*')",
				 // "/testdata/node/multiReference/jcr:deref(@ref, 'reference')",
				 // "/testdata/query/*[@prop1] order by @prop1 descending",
				 // "/testdata/*[@prop1] order by @prop1",
				 // "//*[@boolean = 'true']",
				 // "//*[@double = '3.141592653589793']",
				 // "//*[@boolean] order by @boolean ascending",
				 // "//*[@double != '3.14'] order by @double ascending",
				 // "//*[@long] order by @long descending",
				 // "//*[@calendar and @long] order by @long, @calendar descending",
				 // "/testdata//*[@boolean = 'true']",
				 // "/testdata//*[@boolean = 'true' and @double = '3.14' or @long = '12345']",
				 // "//element(*, nt:unstructured)[@boolean = 'true']",
				 // "//element(*, nt:unstructured)[@boolean != 'true']",
				 // "//element(*, nt:unstructured)[@boolean != 'false']",
				 // "//element(*, nt:resource)[@boolean = 'true']",
				 // "//element(*, nt:unstructured)[@double = '3.141592653589793']",
				 // "//*[jcr:contains(@dc:identifier, 'canberra')]",
				 // "//*[jcr:contains(@dc:title, 'Griffin')]",
				 // "//*[jcr:contains(@jcr:data, 'rss')]",
				 // "//*[jcr:contains(@jcr:data, 'podcast')]",
				 // "//*[jcr:contains(@jcr:data, 'Plone')]",
				 // "//*[jcr:contains(@jcr:data, 'css')]",
				 // "//*[jcr:contains(@jcr:data, 'embed')]",
				 "//*[jcr:contains(@jcr:data, 'asia')]",
				 Query.XPATH);

				 // "SELECT * FROM [nt:base]",
				 // Query.JCR_SQL2);

			QueryResult result = q.execute();

			if (result == null) {
				System.out.println("null result!");
			}

			NodeIterator it = result.getNodes();
			int i = 0;
			System.out.println("number of items: " + it.getSize());

			while (it.hasNext()) {
				Node n = it.nextNode();

				i++;

				System.out.println();
				System.out.println("item " + i);
				System.out.println("path: " + n.getPath());
				// System.out.println(n.getProperty("prop1").getString());
				// System.out.println("primary type: " + n.getProperty("jcr:primaryType").getString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} finally {
			if (session != null) {
				session.logout();
			}
		}
	}

	public static void main (String[] args) {
		Session session;

		try {
			initLog4j();

			// testAddProperty();
			// testListProperty();
			// testGetProperty();
			// testDeleteProperty();
			// testBulkDelete();
			// testReadFedora();
			// testBulkWriteFedora();
			// testGSearch();

			// testMultipleSessions();
			// testAddPropertyJCR();
			// testGetPropertyJCR();
			// testReadJCR();
			// testGetNodesJCR();
			// testBulkWriteJCR();
			// testWrite();
			// testRead();
			// testImport();
			// testWriteABCD();
			testQuery();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
