from zope import schema
from zope.interface import Interface

from zope.app.container.constraints import contains
from zope.app.container.constraints import containers

from northwestern.jcr.explorer import explorerMessageFactory as _

class IJCRImage(Interface):
    """A JCR image
    """

    contains('northwestern.jcr.explorer.interfaces.IAnnotatedImage')
    
    # film_code = schema.ASCIILine(title=_(u"Film Code"),
    #                              description=_(u"This should match the film code used by the booking system"),
    #                              required=True)
    
    title = schema.TextLine(title=_(u"Film title"),
                            required=True)
    
    summary = schema.TextLine(title=_(u"Short summary"),
                              description=_(u"Plain-text blurb about the film"))
    
    url = schema.SourceText(title=_(u"URL"),
                               description=_(u"A teaser/description of the film"),
                               required=True)
    
    # shown_from = schema.Date(title=_(u"Visible from"),
    #                          description=_(u"Date when film first appears on the website"))
    # 
    # shown_until = schema.Date(title=_(u"Visible until"),
    #                          description=_(u"Date when film last appears on the website"))

    # -*- schema definition goes here -*-
