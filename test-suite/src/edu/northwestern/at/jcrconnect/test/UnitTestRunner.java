package edu.northwestern.at.jcrconnect.test;

import java.io.*;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import javax.jcr.Repository;
import javax.naming.InitialContext;
import org.apache.log4j.PropertyConfigurator;
import org.xml.sax.InputSource;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;

/**	Runs the JCR 1.0 SDK unit tests.
 *
 *	<p>Usage:
 *
 *	<p><code>java edu.northwestern.at.jcrconnect.test.UnitTestRunner</code>
 *
 *	<p>Files used by all tests:
 *
 *	<ul>
 *	<li>log4j.properties: Log4j configuration properties.
 *	<li>repositoryStubImpl.properties: TCK configuration properties.
 *	</ul>
 *
 *	<p>Files used by the Jackrabbit test repository:
 *
 *	<ul>
 *	<li>repository.xml: Jackrabbit configuration properties.
 *	<li>jackrabbit: Jackrabbit test repository home directory.
 *	</ul>
 */

public class UnitTestRunner {

	/**	Array of test classes.
	 *
	 *	<p>To run a subset of the tests, comment out the classes you do not want to run.
	 */

	private static Class[] testClasses =
	
		new Class[] {
			org.apache.jackrabbit.test.api.NodeReadMethodsTest.class,
/*
			// Basic level 1 tests.

			org.apache.jackrabbit.test.api.RootNodeTest.class,
			org.apache.jackrabbit.test.api.NodeReadMethodsTest.class,
			org.apache.jackrabbit.test.api.PropertyTypeTest.class,
			org.apache.jackrabbit.test.api.NodeDiscoveringNodeTypesTest.class,
	
			org.apache.jackrabbit.test.api.BinaryPropertyTest.class,
			org.apache.jackrabbit.test.api.BooleanPropertyTest.class,
			org.apache.jackrabbit.test.api.DatePropertyTest.class,
			org.apache.jackrabbit.test.api.DoublePropertyTest.class,
			org.apache.jackrabbit.test.api.LongPropertyTest.class,
			org.apache.jackrabbit.test.api.NamePropertyTest.class,
			org.apache.jackrabbit.test.api.PathPropertyTest.class,
			org.apache.jackrabbit.test.api.ReferencePropertyTest.class,
			org.apache.jackrabbit.test.api.StringPropertyTest.class,
			org.apache.jackrabbit.test.api.UndefinedPropertyTest.class,
	
			org.apache.jackrabbit.test.api.NamespaceRegistryReadMethodsTest.class,
			org.apache.jackrabbit.test.api.NamespaceRemappingTest.class,
			org.apache.jackrabbit.test.api.NodeIteratorTest.class,
			org.apache.jackrabbit.test.api.PropertyReadMethodsTest.class,
			org.apache.jackrabbit.test.api.RepositoryDescriptorTest.class,
			org.apache.jackrabbit.test.api.SessionReadMethodsTest.class,
			org.apache.jackrabbit.test.api.WorkspaceReadMethodsTest.class,
			org.apache.jackrabbit.test.api.ReferenceableRootNodesTest.class,
	
			org.apache.jackrabbit.test.api.ExportSysViewTest.class,
			org.apache.jackrabbit.test.api.ExportDocViewTest.class,
	
			// Basic level 2 tests.
			
			org.apache.jackrabbit.test.api.AddNodeTest.class,
			org.apache.jackrabbit.test.api.NamespaceRegistryTest.class,
			org.apache.jackrabbit.test.api.ReferencesTest.class,
			org.apache.jackrabbit.test.api.SessionTest.class,
			org.apache.jackrabbit.test.api.SessionUUIDTest.class,
			org.apache.jackrabbit.test.api.NodeTest.class,
			org.apache.jackrabbit.test.api.NodeUUIDTest.class,
			org.apache.jackrabbit.test.api.NodeOrderableChildNodesTest.class,
			org.apache.jackrabbit.test.api.PropertyTest.class,
	
			org.apache.jackrabbit.test.api.SetValueBinaryTest.class,
			org.apache.jackrabbit.test.api.SetValueBooleanTest.class,
			org.apache.jackrabbit.test.api.SetValueDateTest.class,
			org.apache.jackrabbit.test.api.SetValueDoubleTest.class,
			org.apache.jackrabbit.test.api.SetValueLongTest.class,
			org.apache.jackrabbit.test.api.SetValueReferenceTest.class,
			org.apache.jackrabbit.test.api.SetValueStringTest.class,
			org.apache.jackrabbit.test.api.SetValueConstraintViolationExceptionTest.class,
			org.apache.jackrabbit.test.api.SetValueValueFormatExceptionTest.class,
			org.apache.jackrabbit.test.api.SetValueVersionExceptionTest.class,
	
			org.apache.jackrabbit.test.api.SetPropertyBooleanTest.class,
			org.apache.jackrabbit.test.api.SetPropertyCalendarTest.class,
			org.apache.jackrabbit.test.api.SetPropertyDoubleTest.class,
			org.apache.jackrabbit.test.api.SetPropertyInputStreamTest.class,
			org.apache.jackrabbit.test.api.SetPropertyLongTest.class,
			org.apache.jackrabbit.test.api.SetPropertyNodeTest.class,
			org.apache.jackrabbit.test.api.SetPropertyStringTest.class,
			org.apache.jackrabbit.test.api.SetPropertyValueTest.class,
			org.apache.jackrabbit.test.api.SetPropertyConstraintViolationExceptionTest.class,
			org.apache.jackrabbit.test.api.SetPropertyAssumeTypeTest.class,
	
			org.apache.jackrabbit.test.api.NodeItemIsModifiedTest.class,
			org.apache.jackrabbit.test.api.NodeItemIsNewTest.class,
			org.apache.jackrabbit.test.api.PropertyItemIsModifiedTest.class,
			org.apache.jackrabbit.test.api.PropertyItemIsNewTest.class,
	
			org.apache.jackrabbit.test.api.NodeAddMixinTest.class,
			org.apache.jackrabbit.test.api.NodeCanAddMixinTest.class,
			org.apache.jackrabbit.test.api.NodeRemoveMixinTest.class,
	
			org.apache.jackrabbit.test.api.WorkspaceCloneReferenceableTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCloneSameNameSibsTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCloneTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCloneVersionableTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesReferenceableTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesSameNameSibsTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesVersionableTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCopyReferenceableTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCopySameNameSibsTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCopyTest.class,
			org.apache.jackrabbit.test.api.WorkspaceCopyVersionableTest.class,
			org.apache.jackrabbit.test.api.WorkspaceMoveReferenceableTest.class,
			org.apache.jackrabbit.test.api.WorkspaceMoveSameNameSibsTest.class,
			org.apache.jackrabbit.test.api.WorkspaceMoveTest.class,
			org.apache.jackrabbit.test.api.WorkspaceMoveVersionableTest.class,
	
			org.apache.jackrabbit.test.api.RepositoryLoginTest.class,
			org.apache.jackrabbit.test.api.ImpersonateTest.class,
			org.apache.jackrabbit.test.api.CheckPermissionTest.class,
	
			org.apache.jackrabbit.test.api.DocumentViewImportTest.class,
			org.apache.jackrabbit.test.api.SerializationTest.class,
	
			org.apache.jackrabbit.test.api.ValueFactoryTest.class,
			
			// Lock tests.
			
			org.apache.jackrabbit.test.api.lock.LockTest.class,
			org.apache.jackrabbit.test.api.lock.SetValueLockExceptionTest.class,
			
			// Node type tests.
			
			org.apache.jackrabbit.test.api.nodetype.NodeDefTest.class,
			org.apache.jackrabbit.test.api.nodetype.NodeTypeManagerTest.class,
			org.apache.jackrabbit.test.api.nodetype.NodeTypeTest.class,
			org.apache.jackrabbit.test.api.nodetype.PropertyDefTest.class,
			org.apache.jackrabbit.test.api.nodetype.PredefinedNodeTypeTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyBinaryTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyBooleanTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyDateTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyDoubleTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyLongTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyMultipleTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyNameTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyPathTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyStringTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanSetPropertyTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanAddChildNodeCallWithNodeTypeTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanAddChildNodeCallWithoutNodeTypeTest.class,
			org.apache.jackrabbit.test.api.nodetype.CanRemoveItemTest.class,

			// Observation tests.
			
        	org.apache.jackrabbit.test.api.observation.EventIteratorTest.class,
        	org.apache.jackrabbit.test.api.observation.EventTest.class,
        	org.apache.jackrabbit.test.api.observation.GetRegisteredEventListenersTest.class,
        	org.apache.jackrabbit.test.api.observation.LockingTest.class,
        	org.apache.jackrabbit.test.api.observation.NodeAddedTest.class,
        	org.apache.jackrabbit.test.api.observation.NodeRemovedTest.class,
        	org.apache.jackrabbit.test.api.observation.NodeMovedTest.class,
        	org.apache.jackrabbit.test.api.observation.NodeReorderTest.class,
        	org.apache.jackrabbit.test.api.observation.PropertyAddedTest.class,
        	org.apache.jackrabbit.test.api.observation.PropertyChangedTest.class,
        	org.apache.jackrabbit.test.api.observation.PropertyRemovedTest.class,
        	org.apache.jackrabbit.test.api.observation.AddEventListenerTest.class,
        	org.apache.jackrabbit.test.api.observation.WorkspaceOperationTest.class,

			// Query tests.
			
        	org.apache.jackrabbit.test.api.query.SaveTest.class,
        	org.apache.jackrabbit.test.api.query.SQLOrderByTest.class,
        	org.apache.jackrabbit.test.api.query.SQLQueryLevel2Test.class,
        	org.apache.jackrabbit.test.api.query.SQLJoinTest.class,
        	org.apache.jackrabbit.test.api.query.SQLJcrPathTest.class,
        	org.apache.jackrabbit.test.api.query.SQLPathTest.class,
        	org.apache.jackrabbit.test.api.query.XPathPosIndexTest.class,
        	org.apache.jackrabbit.test.api.query.XPathDocOrderTest.class,
        	org.apache.jackrabbit.test.api.query.XPathOrderByTest.class,
        	org.apache.jackrabbit.test.api.query.XPathQueryLevel2Test.class,
        	org.apache.jackrabbit.test.api.query.XPathJcrPathTest.class,
        	org.apache.jackrabbit.test.api.query.DerefQueryLevel1Test.class,
        	org.apache.jackrabbit.test.api.query.ElementTest.class,
        	org.apache.jackrabbit.test.api.query.TextNodeTest.class,
        	org.apache.jackrabbit.test.api.query.GetLanguageTest.class,
        	org.apache.jackrabbit.test.api.query.GetPersistentQueryPathLevel1Test.class,
        	org.apache.jackrabbit.test.api.query.GetPersistentQueryPathTest.class,
        	org.apache.jackrabbit.test.api.query.GetStatementTest.class,
        	org.apache.jackrabbit.test.api.query.GetSupportedQueryLanguagesTest.class,
        	org.apache.jackrabbit.test.api.query.GetPropertyNamesTest.class,
        	org.apache.jackrabbit.test.api.query.PredicatesTest.class,
        	org.apache.jackrabbit.test.api.query.SimpleSelectionTest.class,
        	org.apache.jackrabbit.test.api.query.OrderByDateTest.class,
        	org.apache.jackrabbit.test.api.query.OrderByDoubleTest.class,
        	org.apache.jackrabbit.test.api.query.OrderByLongTest.class,
        	org.apache.jackrabbit.test.api.query.OrderByMultiTypeTest.class,
        	org.apache.jackrabbit.test.api.query.OrderByStringTest.class,
        	
        	// Versioning tests.
        	
        	org.apache.jackrabbit.test.api.version.VersionTest.class,
        	org.apache.jackrabbit.test.api.version.VersionHistoryTest.class,
        	org.apache.jackrabbit.test.api.version.VersionStorageTest.class,
        	org.apache.jackrabbit.test.api.version.VersionLabelTest.class,
        	org.apache.jackrabbit.test.api.version.CheckoutTest.class,
        	org.apache.jackrabbit.test.api.version.CheckinTest.class,
        	org.apache.jackrabbit.test.api.version.VersionGraphTest.class,
        	org.apache.jackrabbit.test.api.version.RemoveVersionTest.class,
        	org.apache.jackrabbit.test.api.version.RestoreTest.class,
        	org.apache.jackrabbit.test.api.version.WorkspaceRestoreTest.class,
        	org.apache.jackrabbit.test.api.version.OnParentVersionAbortTest.class,
        	org.apache.jackrabbit.test.api.version.OnParentVersionComputeTest.class,
        	org.apache.jackrabbit.test.api.version.OnParentVersionCopyTest.class,
        	org.apache.jackrabbit.test.api.version.OnParentVersionIgnoreTest.class,
        	org.apache.jackrabbit.test.api.version.OnParentVersionInitializeTest.class,
        	org.apache.jackrabbit.test.api.version.GetReferencesNodeTest.class,
        	org.apache.jackrabbit.test.api.version.GetPredecessorsTest.class,
        	org.apache.jackrabbit.test.api.version.GetCreatedTest.class,
        	org.apache.jackrabbit.test.api.version.GetContainingHistoryTest.class,
        	org.apache.jackrabbit.test.api.version.GetVersionableUUIDTest.class,
        	org.apache.jackrabbit.test.api.version.SessionMoveVersionExceptionTest.class,
        	org.apache.jackrabbit.test.api.version.WorkspaceMoveVersionExceptionTest.class,
        	org.apache.jackrabbit.test.api.version.MergeCancelMergeTest.class,
        	org.apache.jackrabbit.test.api.version.MergeCheckedoutSubNodeTest.class,
        	org.apache.jackrabbit.test.api.version.MergeDoneMergeTest.class,
        	org.apache.jackrabbit.test.api.version.MergeNodeIteratorTest.class,
        	org.apache.jackrabbit.test.api.version.MergeNodeTest.class,
        	org.apache.jackrabbit.test.api.version.MergeNonVersionableSubNodeTest.class,
        	org.apache.jackrabbit.test.api.version.MergeSubNodeTest.class,
*/        	
		};
		
	/**	Gets the repository instance to be tested.
	 *
	 *	<p>The code here gets a Jackrabbit 1.0 test repository. To test some other
	 *	repository, replace this method.
	 *
	 *	@return		Repository to be tested.
	 *
	 *	@throws		Exception
	 */
		
	private static Repository getRepository () 
		throws Exception
	{
		File configFile = new File("repository.xml");
		InputStream configFileInputStream = new FileInputStream(configFile);
		InputSource configFileInputSource = new InputSource(configFileInputStream);
		File repositoryHome = new File("jackrabbit");
		RepositoryConfig config = 
			RepositoryConfig.create(configFileInputSource, repositoryHome.getAbsolutePath());
		configFileInputStream.close();
		return RepositoryImpl.create(config);
	}
	
	/**	Registers the test repository with JNDI.
	 *
	 *	@param	repository		Test repository.
	 *
	 *	@throws	Exception
	 */
	
	private static void registerRepository (Repository repository)
		throws Exception 
	{
		Properties env = new Properties();
		env.put("java.naming.provider.url", 
			"http://www.apache.org/jackrabbit");
		env.put("java.naming.factory.initial", 
			"com.day.crx.jndi.provider.MemoryInitialContextFactory");
		InitialContext jndiContext = new InitialContext(env);
		jndiContext.bind("test-repository", repository);
	}
	
	/**	Initializes log4j.
	 *
	 *	@throws	Exception
	 */
	
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
	
	/**	The main program.
	 *
	 *	@param	args	Command line arguments (ignored).
	 */
	
	public static void main (String[] args) {
		try {
			initLog4j();
			System.setProperty("javax.jcr.tck.properties", "repositoryStubImpl.properties");
			TestSuite suite = new TestSuite();
			for (Class testClass : testClasses) suite.addTestSuite(testClass);
			Repository repository = getRepository();
			registerRepository(repository);
			junit.textui.TestRunner.run(suite);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}