-------------------------------------------------------------------
             XTF JCR Connector Release 0.5 - December 2009
-------------------------------------------------------------------
This is a full source code release of the JCR connector for XTF,
an architecture that supports searching across collections of 
heterogeneous textual data (XML, PDF, text), and the presentation of 
results and documents in a highly configurable manner.  JCR (Content 
Repository API for Java) is the emerging Java platform API for 
accessing content repositories in a uniform manner.

In current implementation the JCR adapter for Fedora is built on top of
Apache Jackrabbit 1.5, or more specifically, the core of the adapter is a 
Jackrabbit index manager that implements the 
org.apache.jackrabbit.core.query.QueryHandler interface. It translates 
all JCR queries to XTF queries and stores all the items returned in a 
Jackrabbit repository. All future node/property storing and loading 
requests are then directed to this Jackrabbit repository.

A Jackrabbit 1.5 installation is included with this release. In the default 
workspace configuration file 

jackrabbit/workspaces/default/workspace.xml 

the XTF index manager is set as the index manager used by the 
Jackrabbit repository (in place of the default Lucene-based index
manager).

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


Prerequisites
================================
The JCR connector for Fedora requires the following software:

- XTF 2.1+
- Java SE 5.0+
- Ant 1.7+

Note that a Jackrabbit 1.5 installation is included with the adapter package.


Building the Connector
================================
To build the connector from source, make sure you have Java SE 5 and
ant 1.7 installed and enter the following:

  ant


Setting up an XTF Repository
================================
This connector was developed for XTF 2.1. Earlier versions of XTF
have not been tested. To use this connector, make sure an XTF
instance is running. 


Configure the XTF Repository
================================
To make the JCR connector aware of your XTF instance, change the 
values for "host" and "port" in the property file xtf.properties.


Test Program
================================
Test.java in the src directory is intended for various quick and dirty tests.
If you are familiar with various JSR 170 features, feel free to use
this program as a template for code that reads from and writes to your
Fedora repository through JCR APIs.
