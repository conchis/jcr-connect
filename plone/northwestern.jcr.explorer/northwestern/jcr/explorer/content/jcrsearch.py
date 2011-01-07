"""Definition of the JCRSearch content type
"""
import transaction

from zope.interface import implements, directlyProvides

from Products.CMFCore.utils import getToolByName

from Products.Archetypes import atapi
from Products.ATContentTypes.content import base
from Products.ATContentTypes.content import schemata

from AccessControl import ClassSecurityInfo

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IJCRSearch
from northwestern.jcr.explorer.config import PROJECTNAME

JCRSearchSchema = schemata.ATContentTypeSchema.copy() + atapi.Schema((

    # -*- Your Archetypes field definitions here ... -*-
    atapi.StringField('accessservice',
        required=False,
        searchable=True,
        storage=atapi.AnnotationStorage(),
        widget=atapi.StringWidget(label=_(u"Access Service"),
                                  description=_(u""),
                                  size=80)
        ),

    atapi.StringField('shelfservice',
        required=False,
        searchable=True,
        storage=atapi.AnnotationStorage(),
        widget=atapi.StringWidget(label=_(u"Shelf Service"),
                                  description=_(u""),
                                  size=80)
        ),

    atapi.StringField('webdavservice',
        required=False,
        searchable=True,
        storage=atapi.AnnotationStorage(),
        widget=atapi.StringWidget(label=_(u"WebDAV Service"),
                                  description=_(u""),
                                  size=80)
        ),

    atapi.StringField('username',
        required=False,
        searchable=True,
        storage=atapi.AnnotationStorage(),
        widget=atapi.StringWidget(label=_(u"User Name"),
                                  description=_(u""),
                                  size=50)
        ),

    atapi.StringField('password',
        required=False,
        searchable=True,
        storage=atapi.AnnotationStorage(),
        widget=atapi.StringWidget(label=_(u"Password"),
                                  description=_(u""),
                                  size=50)
         )


))

# Set storage on fields copied from ATContentTypeSchema, making sure
# they work well with the python bridge properties.

JCRSearchSchema['title'].storage = atapi.AnnotationStorage()
JCRSearchSchema['description'].storage = atapi.AnnotationStorage()

schemata.finalizeATCTSchema(JCRSearchSchema, moveDiscussion=False)

class JCRSearch(base.ATCTContent):
    """Content type for conducting JCR-based search"""
    security = ClassSecurityInfo()
    implements(IJCRSearch)

    meta_type = "JCRSearch"
    schema = JCRSearchSchema

    title = atapi.ATFieldProperty('title')
    description = atapi.ATFieldProperty('description')
    accessservice = atapi.ATFieldProperty('accessservice')
    shelfservice = atapi.ATFieldProperty('shelfservice')
    webdavservice = atapi.ATFieldProperty('webdavservice')
    username = atapi.ATFieldProperty('username')
    password = atapi.ATFieldProperty('password')
    
    # -*- Your ATSchema to Python Property Bridges Here ... -*-

    security.declarePublic('seach')
    def search(self, term, repository):
        """search"""
        tool = getToolByName(self, 'portal_membership')
        folder = tool.getHomeFolder()

        types_tool = getToolByName(self, 'portal_types')            	

        tag = term

        searchserviceDICT = {}
        searchserviceDICT["CDL"] = "http://localhost:8080/category_marker/service/search?ws=xtf&q="
        searchserviceDICT["EOC"] = "http://localhost:8080/category_marker/service/search?ws=fedora&q="

        if term not in folder:
            new_id = types_tool.constructContent('JCRFolder', folder, tag, None, title=tag)

            transaction.savepoint(optimistic=True)

        try:
            e = getattr(folder, tag)
            # e.title = tag
            e.url = "http://localhost:8080/category_marker/service/access"
            e.term = term
            e.searchservice = []
            e.repositoryid = []
            e.refresh = True
            if isinstance(repository, list):
                for r in repository:
                    e.searchservice.append(searchserviceDICT[r])
                    e.repositoryid.append(r)
                    
            else:
                e.searchservice.append(searchserviceDICT[repository])
                e.repositoryid.append(repository)
        except Exception, inst:
            print type(inst)
            print inst.args
            print inst
            print "object not created: ", new_id

        self.REQUEST.RESPONSE.redirect(folder.absolute_url_path() + '/' + tag)


atapi.registerType(JCRSearch, PROJECTNAME)
