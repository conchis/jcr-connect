# 
#   Copyright 2010 Northwestern University.
# 
# Licensed under the Educational Community License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the
# License. You may obtain a copy of the License at
# 
#    http://www.osedu.org/licenses/ECL-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
# 
# Author: Xin Xiang, Rick Moore
# 

"""Definition of the JCRRepository content type
"""
import urllib2
import base64
import elementtree.ElementTree as ET
import transaction

from zope.interface import implements
from zope.component import adapts

from Products.CMFCore.utils import getToolByName

from Products.Archetypes import atapi
from Products.Archetypes.interfaces import IObjectPostValidation

from Products.ATContentTypes.content import folder
from Products.ATContentTypes.content import schemata

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IJCRRepository
from northwestern.jcr.explorer.config import PROJECTNAME

from northwestern.jcr.explorer.content.jcrfolder import JCRFolder

# from Products.Archetypes.utils import DisplayList

# REPOSITORY_TYPE_GROUPS = DisplayList(( 
#     ('1', 'Fedora'), 
#     ('2', 'XTF'), 
#     ('3', 'Jackrabbit'), 
#     )) 

JCRRepositorySchema = folder.ATFolderSchema.copy() + atapi.Schema((

    atapi.StringField('repositoryID',
        required=False,
        searchable=True,
        storage=atapi.AnnotationStorage(),
        widget=atapi.StringWidget(label=_(u"Repository ID"),
                                  description=_(u"This should match the ID used in the "
                                                 "browser system."))
        ),

    atapi.StringField('url',
        required=False,
        searchable=True,
        storage=atapi.AnnotationStorage(),
        widget=atapi.StringWidget(label=_(u"Server URL"),
                                  description=_(u""),
                                  size=50)
        ),

    atapi.StringField('syncURL',
        required=False,
        searchable=True,
        storage=atapi.AnnotationStorage(),
        widget=atapi.StringWidget(label=_(u"Synchornization Service URL"),
                                  description=_(u""),
                                  size=50)
        )

    # -*- Your Archetypes field definitions here ... -*-

))

# Set storage on fields copied from ATFolderSchema, making sure
# they work well with the python bridge properties.

JCRRepositorySchema['title'].storage = atapi.AnnotationStorage()
JCRRepositorySchema['title'].widget.label = _(u"Repository name")
JCRRepositorySchema['title'].widget.description = _(u"")

JCRRepositorySchema['description'].storage = atapi.AnnotationStorage()
JCRRepositorySchema['description'].widget.label = _(u"Description")
JCRRepositorySchema['description'].widget.description = _("")

schemata.finalizeATCTSchema(
    JCRRepositorySchema,
    folderish=True,
    moveDiscussion=False
)

class JCRRepository(folder.ATFolder):
    """A collection of items in a JCR Repository."""
    implements(IJCRRepository)

    meta_type = "JCRRepository"
    _at_rename_after_creation = True
    schema = JCRRepositorySchema

    repository_id = atapi.ATFieldProperty('repositoryID')
    name = atapi.ATFieldProperty('title')
    url = atapi.ATFieldProperty('url')
    syncURL = atapi.ATFieldProperty('syncURL')
    description = atapi.ATFieldProperty('description')
    # -*- Your ATSchema to Python Property Bridges Here ... -*-

    def load_folders(self):
        # if self.url.find('sauer') > -1:
        self.load_folders_http()
        # else:
        #     self.load_folders_webdav()

    def load_folders_http(self):
        tool = getToolByName(self, 'portal_membership')
        folder = tool.getHomeFolder()

        types_tool = getToolByName(self, 'portal_types')            	

        if self.url.find("access") > -1:
            tag = "shelf"
        else:
            tag = "content"

        if tag not in folder:
            new_id = types_tool.constructContent('JCRFolder', folder, tag, None, title=tag)
            transaction.savepoint(optimistic=True)

            try:
                e = getattr(folder, new_id)
                e.title = tag
                e.url = self.url # + '/content'
                e.syncURL = self.syncURL
            except Exception, inst:
                print type(inst)
                print inst.args
                print inst
                print "object not created: ", new_id

    def load_folders_webdav(self):
        path = self.url + '/jcr%3aroot'
        request = urllib2.Request(path)
        request.add_header('Depth', '1')
        request.get_method = lambda: 'PROPFIND'
        base64string = base64.encodestring('%s:%s' % ('admin', 'abc'))[:-1]
        authheader =  "Basic %s" % base64string
        request.add_header("Authorization", authheader)
        try:
            url = urllib2.urlopen(request)
        except IOError, e:
            pass
        if not hasattr(e, 'code') or e.code != 207:
            # we got an error - but not a 207 error
            # server not reachable
            print 'Server not available'

            self.description = 'Offline mode (repository not available)'

            list = []
            for tag in self:
                if isinstance(self[tag], JCRFolder):
                    list.append( dict(url = tag, title = tag, description = '',) )
            return list

        self.description = ''

        if e.code == 207:
            data = e
        else:
            data = url
        tree = ET.parse(data)
        root = tree.getroot()
        list = []
        types_tool = getToolByName(self, 'portal_types')            	

        for i in range(1, len(root)):
            # skip the entry for itself
            node = root[i]
            # tag = node.tag
            if node[1][0][0].text is None or node[1][0][4][0][0].text == 'nt:file':
                # dcr:name, dcr:nodetypename
                continue
            tag = node[1][0][0].text
            if tag == 'jcr:system' or tag == self.context.title:
                # skip system node and current node
                continue
            tag = tag.replace(':', '_')
            if tag not in self:
                # add to context
                new_id = types_tool.constructContent('JCRFolder', self, tag, None, title=tag)
                transaction.savepoint(optimistic=True)
                try:
                    getattr(self, new_id).title = tag
                except:
                    print "object not created: ", new_id

            # always add to the list
            list.append( dict(url = tag, title = tag, description = '',) )

        return list


atapi.registerType(JCRRepository, PROJECTNAME)


# This is a subscription adapter which is used to validate the cinema object.
# It will be called after the normal schema validation.

class ValidateRepositoryCodeUniqueness(object):
    """Validate site-wide uniquness of cinema codes.
    """
    implements(IObjectPostValidation)
    adapts(IJCRRepository)
    
    field_name = 'repositoryID'
    
    def __init__(self, context):
        self.context = context
    
    def __call__(self, request):
        value = request.form.get(self.field_name, request.get(self.field_name, None))
        if value is not None:
            catalog = getToolByName(self.context, 'portal_catalog')
            results = catalog(repository_id=value,
                              object_provides=IJCRRepository.__identifier__)
            if len(results) == 0:
                return None
            elif len(results) == 1 and results[0].UID == self.context.UID():
                return None
            else:
                return {self.field_name : _(u"The ID is already in use")}
        
        # Returning None means no error
        return None

