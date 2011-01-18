-------------------------------------------------------------------
             JCR-Connect Plone Product  Release 0.3 - January 2011
-------------------------------------------------------------------
This is a release of the JCR-Connect Plone product. JCR (Content 
Repository API for Java) is the emerging Java platform API for accessing
content repositories in a uniform manner.

The JCR-Connect Plone product is a Plone add-on that connects
to the JCR-Connect web services and serves as the presentation
system that exposes JCR repositories to end users.

Before using this software, you must read and agree to the the following
license, also found in LICENSE.txt.


License (see also LICENSE.txt)
================================

Copyright 2010 Northwestern University.

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
The JCR-Connector connector webapp requires the following software:

- Plone 3.x


Installing JCR-Connect Plone Product
================================

Installing Plone 3
---------------

The latest version of Plone 3 is available at

http://plone.org/products/plone/releases/3.3.5

Please follow documents in the package to install Plone
to the target directory. An example command is:

./install.sh standalone --target=/path/to/Plone --password=password

After that, try starting Plone by running

./plonectl start

in zinstance/bin of the Plone installation. The default port
is 8080.

If Plone can be accessed in the browser, bring it down by running

./plonectl stop


Installing the Product
--------------------

Edit the configuration file

zinstance/buildout.cfg

in the Plone installation and make the following changes:

- Ports section

http-address = 7070

Set the port so it's not conflicting with the Tomcat instance
that the JCR-Connect webapps are running on.

- Eggs section

Add
    northwestern.jcr.explorer
    Products.Collage

after

eggs =
    Plone

The first is the JCR-Connect Plone product and the second is a dependency.

- ZCML Slugs section

Add northwestern.jcr.explorer

- find-links section

Add

http://jcr-connect.at.northwestern.edu/documents/installer/

If a previous version of the product has been installed, remove
precious packages by running

rm -rf "northwestern.jcr.explorer*"

in the "buildout-cache" directory of the Plone installation.

This allows Plone to retrieve the latest version of the product
without having to make any changes to the configuration.

After all the steps are done, run

bin/buildout

for the changes to take effect and restart Plone. 

After Plone is successfully restarted, navigate to the "Site Setup"
section of the web interface, click "Add-on Products" under
"Plone configuration". In the section called "Products available 
for install", check the box next to "JCR Repository Explorer"
and click "Install" button.


Enabling Member Folders
----------------------

Please follow instructions on the page

http://plone.org/documentation/faq/how-do-i-enable-member-folders-in-plone-3

to enable member folders in the Plone instance.


Test
================================
