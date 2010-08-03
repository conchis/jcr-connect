from zope import schema
from zope.interface import Interface

from zope.app.container.constraints import contains
from zope.app.container.constraints import containers

from northwestern.jcr.explorer import explorerMessageFactory as _

class IJCRDocument(Interface):
    """JCR Document"""
    
    # -*- schema definition goes here -*-
