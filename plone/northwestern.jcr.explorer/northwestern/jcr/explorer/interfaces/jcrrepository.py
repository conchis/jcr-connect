from zope import schema
from zope.interface import Interface

from zope.app.container.constraints import contains
from zope.app.container.constraints import containers

from northwestern.jcr.explorer import explorerMessageFactory as _

class IJCRRepository(Interface):
    """A collection of items in a JCR Repository."""

    contains('northwestern.jcr.explorer.interfaces.IJCRFolder')
    
    repository_id = schema.ASCIILine(title=_(u"Repository ID"),
                                   description=_(u"This should match the ID used by "
                                                  "the browser system"),
                                   required=True)
    
    name = schema.TextLine(title=_(u"Cinema name"),
                           required=True)
                            
    url = schema.TextLine(title=_(u"Server URL"),
                            description=_(u"URL of the JCR Repository"),
                            required=True)
                            
    syncURL = schema.TextLine(title=_(u"Synchronization Service URL"),
                            description=_(u"URL of the JCR Synchronization Service"),
                            required=True)
                            
    description = schema.Text(title=_(u"Description"),
                          description=_(u"Description of this cinema"),
                          required=True)
                            
    # text = schema.SourceText(title=_(u"Descriptive text"),
    #                          description=_(u"Descriptive text about this cinema"),
    #                          required=True)
                             
    # highlighted_films = schema.List(title=_(u"Highlighted films"),
    #                                  description=_(u"Selected films to highlight"),
    #                                  value_type=schema.Object(title=_(u"JCRImage"),
    #                                                           schema=IJCRImage),
    #                                  unique=True)
                                
    # rtype = schema.TextLine(title=_(u"Type of Repository"),
    #                         description=_(u"Type of the JCR Repository"),
    #                         required=True)

    # -*- schema definition goes here -*-

