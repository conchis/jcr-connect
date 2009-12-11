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
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.xml.sax.InputSource;
import org.apache.log4j.PropertyConfigurator;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TransientRepository;

import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

import edu.northwestern.jcr.adapter.xtf.persistence.XTFClient;
import edu.northwestern.jcr.adapter.xtf.util.ApplyXPath;

/**
 *	A play-pen for various quick and dirty tests.
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

		return session;
	}

	public static void testXPath()
	{
		ApplyXPath xpathProc;
		String [] s = null;

		xpathProc = new ApplyXPath();
		try {
			s = xpathProc.evaluateFile("step4b.html", 
								  "/crossQueryResult/facet[@field='facet-date']/group/@value");
		} catch (Exception e) {
			
		}

		for (String value : s) {
			System.out.println(value);
		}
	}

	public static void testDefaultInstallation()
	{
		XTFClient client = new XTFClient();
		ApplyXPath xpathProc;
		String [] s = null;

		try {
			s = client.getFacet(null);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error getting facet!");
		}

		for (String value : s) {
			System.out.println(value);
		}

		try {
			s = client.getFacet(new String [] {"2002"});
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error getting facet!");
		}

		for (String value : s) {
			System.out.println(value);
		}

		try {
			s = client.getFacet(new String [] {"2002", "01"});
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error getting facet!");
		}

		for (String value : s) {
			System.out.println(value);
		}

		try {
			s = client.getFacet(new String [] {"2002", "01", "01"});
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error getting facet!");
		}

		if (s.length == 0) {
			System.out.println("no more facets!");
			try {
				s = client.getFiles(new String [] {"2002", "01", "01"});
				String path =
					new String(
					client.getFileContent(new String [] {"2002", "01", "01"},
									   "Wine, Liquor, Beer, and Mortality"
									   // "Contract use widespread in wine grape industry"
									   )
							   );
				System.out.println("content: " + path);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Error getting files!");
			}
		}

		for (String value : s) {
			System.out.println(value);
		}
	}

	public static void testCDLib()
	{
		String [] s = null;
		XTFClient client = new XTFClient();

		for (String value : s) {
			System.out.println(value);
		}
	}

	public static void testVTD() throws Exception
	{
		String s;
		int n;
		int i;
		VTDGen vg = new VTDGen();
		VTDNav vn;

		if (vg.parseFile("./subject.xml", true)){
			vn = vg.getNav();
		}
		else {
			vn = null;
		}

		n = vn.getTokenCount();
		s = "";

		for (i = 0; i < n; ++i) {
			if (vn.getTokenType(i) == VTDNav.TOKEN_CHARACTER_DATA) {
				System.out.println(vn.toNormalizedString(i));
			}
		}
	}

	private static void testLogin()
		throws Exception
	{
		Node root, test1;

		initRepository();

		try {
			session = login("default", "superuser", "");
			Workspace ws = session.getWorkspace();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} finally {
			if (session != null) {
				session.logout();
			}
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
				 // "//*[@subject = 'renaissance']",
				 // "//*[@title = 'Old Master Prints']",
				 // "//*[@format = '75 items']",
				 // "//*[@subject = 'renaissance' and @format = '75 items']",
				 // "//*[jcr:contains(@subject, 'renaissance') or @format = '75 items']",
				 // "//*[jcr:contains(@subject, 'safari')]",
				 // "//*[jcr:contains(@subject, 'chemistry')]",
				 // "//*[jcr:contains(@subject, 'physics')]",
				 "//*[jcr:contains(@subject, 'renaissance')]",
				 // "//*[@format = '75 items']",
				 // "//*[jcr:contains(@subject, 'renaissance')]",
				 // "//*[@type = 'archival collection']",
				 // "//*[@subject = 'astronomer']",
				 Query.XPATH);

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
		// testXPath();
		// testCDLib();
		// try {
		// 	testVTD();
		// } catch (Exception e) {
		// 
		// }

		try {
			initLog4j();
			// testLogin();
			testQuery();
		} catch (Exception e) {

		}
	}
}
