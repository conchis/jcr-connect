from zope import schema
from zope.interface import Interface

from zope.app.container.constraints import contains
from zope.app.container.constraints import containers

from northwestern.jcr.explorer import explorerMessageFactory as _

class IJCRSearch(Interface):
    """Content type for conducting JCR-based search"""
    
    # -*- schema definition goes here -*-
