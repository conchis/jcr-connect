from zope import schema
from zope.interface import Interface

from zope.app.container.constraints import contains
from zope.app.container.constraints import containers

from northwestern.jcr.explorer import explorerMessageFactory as _

class IJCRFolder(Interface):
    """A folder which can contain films."""
    
    contains('northwestern.jcr.explorer.interfaces.IJCRImage')
    
    title = schema.TextLine(title=_(u"Title"),
                            required=True)
                            
    description = schema.TextLine(title=_(u"Description"),
                                  description=_(u"A short summary of this folder"))

    url = schema.SourceText(title=_(u"URL"),
                               description=_(u"A teaser/description of the film"),
                               required=True)

    # -*- schema definition goes here -*-
