"""Definition of the JCRDocument content type
"""

from zope.interface import implements, directlyProvides

from Products.Archetypes import atapi
from Products.ATContentTypes.content import base
from Products.ATContentTypes.content import document
from Products.ATContentTypes.content import schemata

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IJCRDocument
from northwestern.jcr.explorer.config import PROJECTNAME

# JCRDocumentSchema = schemata.ATContentTypeSchema.copy() + atapi.Schema((
JCRDocumentSchema = document.ATDocumentSchema.copy() + atapi.Schema((
    # -*- Your Archetypes field definitions here ... -*-
    atapi.TextField('css',
          required=False,
          searchable=True,
          storage=atapi.AnnotationStorage(),
          widget=atapi.StringWidget(label=_(u"CSS URL"),
                                    description=_(u""),
                                    size=50),
          ),
       

))

# Set storage on fields copied from ATContentTypeSchema, making sure
# they work well with the python bridge properties.

JCRDocumentSchema['title'].storage = atapi.AnnotationStorage()
JCRDocumentSchema['description'].storage = atapi.AnnotationStorage()

schemata.finalizeATCTSchema(JCRDocumentSchema, moveDiscussion=False)

class JCRDocument(document.ATDocument):
    """JCR Document"""
    implements(IJCRDocument)

    meta_type = "JCRDocument"
    schema = JCRDocumentSchema

    title = atapi.ATFieldProperty('title')
    description = atapi.ATFieldProperty('description')
    
    # -*- Your ATSchema to Python Property Bridges Here ... -*-
    css = atapi.ATFieldProperty('css')

atapi.registerType(JCRDocument, PROJECTNAME)
