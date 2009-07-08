import java.io.*;
import java.util.*;

import org.xml.sax.InputSource;
import org.apache.log4j.PropertyConfigurator;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;

import javax.jcr.*;


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
		repository = RepositoryImpl.create(config);
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

	private static void test(Session session)
		throws Exception
	{
		Node root, test1;
		int i;
		InputStream is;

		try {
			root = session.getRootNode();

			/////////////////////////////////////////////////////////////
			// read digital objects and streams from Fedora

			for (i = 1; i <= 12; ++i) {
				test1 = root.getNode("canberra_" + i + "/DC/jcr:content");
				// Node test2 = root.getNode("test_image1");
				System.out.println(test1.getProperty("jcr:data").getString());
			
				test1 = root.getNode("canberra_" + i + "/image/jcr:content");
				is = test1.getProperty("jcr:data").getStream();
				writeStream(is, "canberra/canberra_" + i + ".jpg");
			
				test1 = root.getNode("canberra_" + i + "/thumb/jcr:content");
				is = test1.getProperty("jcr:data").getStream();
				writeStream(is, "canberra/canberra_" + i + "_thumb.jpg");
			
				if (i == 6) {
					i = 8;
				}
			}			

			//////////////////////////////////////////////////////////////

			
			// test1 = root.getNode("test_image1");
			// test1 = test1.getNode("DC");
			// test1 = test1.getNode("jcr:content");
			// System.out.println(test1.getProperty("jcr:data").getString());
			// System.out.println(test1.getProperty("jcr:encoding").getString());
			// System.out.println(test1.getProperty("jcr:mimeType").getString());


			/////////////////////////////////////////////////////////////
			// write digital objects and streams to Fedora
			// Node test = root.addNode("test_hello");
			// test.setProperty("DC", "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\"><dc:title>Updated DC from JCR</dc:title><dc:identifier>test:test</dc:identifier></oai_dc:dc>");
			// test.setProperty("image", 
			// 				 new FileInputStream(new File("test.jpg")));
			// session.save();
			/////////////////////////////////////////////////////////////

			// Store content
			// Node hello = root.addNode("test_hello");
			// Node world = hello.addNode("world");
			// world.setProperty("message", "Hello, World!");
			// session.save();

			// // Retrieve content
			// Node node = root.getNode("hello/world");
			// System.out.println(node.getPath());
			// System.out.println(node.getProperty("message").getString());
			// 
			
			// Node system = root.getNode("jcr:system");
			// Node versionStorage = system.getNode("jcr:versionStorage");
			// Node nodeTypes = system.getNode("jcr:nodeTypes");

			// // Remove content
			// root.getNode("hello").remove();
			// session.save();
        } finally {
            session.logout();
        }
	}

	public static void main (String[] args) {
		Session session;

		try {
			initLog4j();
			initRepository();
			session = login("default", "superuser", "");
			test(session);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
