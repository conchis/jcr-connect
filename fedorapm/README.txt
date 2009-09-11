-------------------------------------------------------------------
             Fedora JCR Connector Release 0.5 - September 2009
-------------------------------------------------------------------
This is a full source code release of the JCR connector for Fedora,
a popular digital asset management system that underlies a large number
of institutional repositories and digital libraries. JCR (Content Repository 
API for Java) is the emerging Java platform API for accessing content 
repositories in a uniform manner.

In current implementation the JCR adapter for Fedora is built on top of
Apache Jackrabbit 1.5, or more specifically, the core of the adapter is a 
Jackrabbit persistence manager that implements the 
org.apache.jackrabbit.core.persistence.PersistenceManager interface. It 
translates all node/property storing and loading requests to API calls 
(REST or API-A/API-M) made to the underlying Fedora repository.

A Jackrabbit 1.5 installation is included with this release. In the default 
workspace configuration file 

jackrabbit/workspaces/default/workspace.xml 

the Fedora persistence manager is set as the persistence manager used by the 
Jackrabbit repository (in place of the default object persistence manager), 
while the Fedora search index is set as the search index used by the 
Jackrabbit repository (in place of the default Lucene search index).

Before using this software, you must read and agree to the the following
license, also found in LICENSE.txt.  Documentation can be found  at 
http://jcr-connect.at.northwestern.edu/en/Technical_Discussion.
Javadoc for the source code is available in the docs directory.


License (see also LICENSE.txt)
================================

Copyright 2009 Northwestern University.

Licensed under the Educational Community License, Version 2.0 
(the "License"); you may not use this file except in compliance with 
the License. You may obtain a copy of the License at

http://www.osedu.org/licenses/ECL-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an "AS IS"
BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied. See the License for the specific language governing
permissions and limitations under the License.


Building the Connector
================================
To build the connector from source, make sure you have Java SE 5 and
ant 1.7 installed and enter the following:

  ant


Setting up a Fedora Repository
================================
This connector was developed for Fedora 3.2. Earlier versions of Fedora
have not been tested. To use this connector, make sure a Fedora 3.2
instance is running. If the resource index is not yet enabled, follow
these steps to enable and rebuild resource index.

1. Enable the resource index and use Mulgara datastore by changing the
parameters in the "ResourceIndex" section of the Fedora configuration
file (server/config/fedora.fcfg):

<module role="fedora.server.resourceIndex.ResourceIndex"
        class="fedora.server.resourceIndex.ResourceIndexModule">
  <param name="level" value="1"/>
  <param name="datastore" value="localMulgaraTriplestore"/>
  <param name="syncUpdates" value="false"/>
</module> 

2. Stop the Fedora server (if using Tomcat, this can be done with the 
shutdown.bat or shutdown.sh command);

3. Run fedora-rebuild.sh or fedora-rebuild.bat (in server/bin) to 
re-construct the Resource Index files and the Resource Index tables 
in the SQL database (select menu option '1');

4. Restart the Fedora server (if using Tomcat, this can be done with the 
startup.bat or startup.sh command).

More information about the fedora-rebuild utility can be found at:
http://www.fedora-commons.org/documentation/3.2/Command-Line%20Utilities.html


Configure the Fedora Installation
================================
To make the JCR connector aware of your Fedora installation, change the 
values for "host", "port", "user", "password", "protocol" and "context" in 
the property file fedora.properties. The "phrase" property value restrains 
the objects that are visible from the JCR perspective and is default to 
wildcard character. The "rest" property specifies whether REST API will
be used whenever possible and is default to "yes" since it is the 
recommended way to access the latest version of Fedora repository.


Query Utility
================================
To get a taste of what this connector does, play with the query utility by
entering

  ant query

on the command line. This will execute the query utility which prompts
you for an XPath query expression. For example, type

  /*

will generate a list of all digital objects in the Fedora repository if the
structure is flat, meaning if no object is related to any other object
by an "isMemeberOfCollection" predicate. The "isMemberOfCollection"
predicate is used to express in Fedora the relation between a JCR node
and its child nodes.


Test Program
================================
Test.java in the src directory is intended for various quick and dirty tests.
If you are familiar with various JSR 170 features, feel free to use
this program as a template for code that reads from and writes to your
Fedora repository through JCR APIs.
