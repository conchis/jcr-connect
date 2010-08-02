from zope import schema
from zope.interface import Interface

from zope.app.container.constraints import contains
from zope.app.container.constraints import containers

from northwestern.jcr.explorer import explorerMessageFactory as _

class IBannerProvider(Interface):
    """A component which can provide an HTML tag for a banner image
    """
    
    tag = schema.TextLine(title=_(u"A HTML tag to render to show the banner image"))

