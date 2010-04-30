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
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.xml.sax.InputSource;
import org.apache.log4j.PropertyConfigurator;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.spi.commons.query.xpath.ParseException;

/**
 *	A command line query processer.
 */
public class QueryCMD
{
	private static Repository repository;
	
	private static Session session;

	private static void initLog4j() throws Exception
	{
		File configFile = new File("log4j.properties");
		InputStream configFileInputStream = new FileInputStream(configFile);
		Properties log4jProperties = new Properties();
		log4jProperties.load(configFileInputStream);
		configFileInputStream.close();
		PropertyConfigurator.configure(log4jProperties);
	}
	
	private static void initRepository() throws Exception
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
	
	private static Session login(String workspace, String username, 
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

		return session;
	}

	private static void testQuery() throws Exception
	{
		initRepository();

		try {
			session = login("default", "superuser", "");
			Workspace ws = session.getWorkspace();
			QueryManager qm = ws.getQueryManager();

			while (true) {
				//  prompt the user to enter query
				System.out.println("Enter query: ");
				//  open up standard input
				BufferedReader br = 
					new BufferedReader(new InputStreamReader(System.in));

				String query = null;
				query = br.readLine();

				// execute the query
				Query q = null;
				QueryResult result = null;
				try {
					q = qm.createQuery(query, Query.XPATH);
					result = q.execute();
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Illegal query expression!");
					continue;
				}

				if (result == null) {
					System.out.println("null result!");
					continue;
				}

				// iterate over the result set
				NodeIterator it = result.getNodes();
				int i = 0;
				System.out.println("number of items: " + it.getSize());

				while (it.hasNext()) {
					Node n = it.nextNode();

					i++;

					System.out.println();
					System.out.println("item " + i);
					System.out.println("path: " + n.getPath());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		} finally {
			session.logout();
		}
	}

	public static void main (String[] args) {
		Session session;

		try {
			initLog4j();

			testQuery();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
