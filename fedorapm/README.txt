-------------------------------------------------------------------
             Fedora JCR Connector Release 0.7 - May 2010
-------------------------------------------------------------------
This is a full source code release of the JCR connector for Fedora,
a popular digital asset management system that underlies a large number
of institutional repositories and digital libraries. JCR (Content Repository 
API for Java) is the emerging Java platform API for accessing content 
repositories in a uniform manner.

In current implementation the JCR connector for Fedora is built on top of
Apache Jackrabbit 2.0.0, or more specifically, the core of the connector is a 
Jackrabbit persistence manager that implements the 
org.apache.jackrabbit.core.persistence.PersistenceManager interface. It 
translates all node/property storing and loading requests to API calls 
(REST or API-A/API-M) made to the underlying Fedora repository.

A Jackrabbit 2.0.0 repository is included with this release under the directory
"jackrabbit". In the default workspace configuration file 

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


Changes from 0.6 release
================================
- Jackrabbit 2.0.0 and JCR 2.0 APIs are supported.
- Simple full-text search against Fedora data streams is implemented.
- All Dublin Core fields are exposed as JCR properties.

Prerequisites
================================
The JCR connector for Fedora requires the following software:

- Fedora 3.2+
- Java SE 5.0+
- Ant 1.7+

Note that a Jackrabbit 2.0.0 repository is included with the connector package.


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

To use the full-text search feature of the connector, set up GSearch service
on the same host as the Fedora repository, as directed in:
http://www.fedora-commons.org/confluence/display/FCSVCS/Generic+Search+Service+2.2

Follow these steps to create index for GSearch:

1. Go to the RESTful interface for the GSearch installation, for example, at
http://localhost:9090/fedoragsearch/rest?operation=updateIndex

2. Click the button "updateIndex createEmpty". This will create an empty
index.

3. Click the button "updateIndex fromFoxmlFiles". This will create index using
FOXML files at the default location.

To enable automatic update to the index used by Fedora GSearch, follow
these steps to enable messaging in Fedora:

1. Enable messaging by changing the parameter in the "Messaging" section
of the Fedora configuration file (server/config/fedora.fcfg):

<module role="fedora.server.messaging.Messaging" class="fedora.server.messaging.MessagingModule">
  <comment>Fedora's Java Messaging Service (JMS) Module</comment>
  <param name="enabled" value="true"/>
  <param name="java.naming.factory.initial" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
  <param name="java.naming.provider.url" value="vm:(broker:(tcp://localhost:61616))"/>
  <param name="datastore1" value="apimUpdateMessages">
    <comment>A datastore representing a JMS Destination for APIM events which update the repository</comment>
  </param>
  <param name="datastore2" value="apimAccessMessages">
    <comment>A datastore representing a JMS Destination for APIM events which do not update the repository</comment>
  </param>
</module>

Make sure the parameters match those specified in the updater.properties
in the GSearch installation located at:
fedoragsearch/WEB-INF/classes/config/updater/BasicUpdaters/updater.properties

2. Stop the Fedora server (if using Tomcat, this can be done with the 
shutdown.bat or shutdown.sh command);

3. Restart the Fedora server (if using Tomcat, this can be done with the 
startup.bat or startup.sh command).

More information about messaging in Fedora can be found at:
http://www.fedora-commons.org/confluence/display/FCR30/Messaging


Configure the Fedora Installation
================================
To make the JCR connector aware of your Fedora installation, modify
the configuration file fedora.properties as appropriate for the Fedora
repository and GSearch installation. The parameters are:

host: the host on which the Fedora repository and GSearch service reside;
port: the port at which the Fedora server is listening;
user: the user account that has the proper permission to manipulate the Fedora objects;
password: password of the user;
protocol: http/https
context: context of the Fedora repository web application;
gsearchcontext: context of the Fedora GSearch service;
gsearchfields: list of the data streams against which full-text search will be run;
phrase: restrains the objects that are visible from the JCR perspective (default to wildcard character);
rest: (y/n) whether REST API will be used whenever possible and is default
to "yes" since it is the recommended way to access the latest version of 
Fedora repository.


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
and its child nodes. While type

  //*[jcr:contains(@jcr:data, 'asia')]

will generate a list of digital objects whose data stream (one of those specified
in "gsearchfields" of the fedora.properties file) contains the word "asia".


Test Program
================================
Test.java in the src directory is intended for various quick and dirty tests.
If you are familiar with various JSR 170 features, feel free to use
this program as a template for code that reads from and writes to your
Fedora repository through JCR APIs.
