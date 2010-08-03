"""Browser view for the JCRRepository content type.
"""

from Acquisition import aq_inner
from zope.interface import implements, Interface

from plone.memoize.instance import memoize

from Products.Five import BrowserView
from Products.Five.browser.pagetemplatefile import ViewPageTemplateFile

from Products.CMFCore.utils import getToolByName

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IBannerProvider
from northwestern.jcr.explorer.content.jcrfolder import JCRFolder

class IJCRRepositorySearch(Interface):
    """
    JCRRepository view interface
    """

    def test():
        """ test method"""


class JCRRepositorySearch(BrowserView):
    """
    JCRRepository browser view
    """
    implements(IJCRRepositorySearch)

    def __init__(self, context, request):
        self.context = context
        self.request = request

    __call__ = ViewPageTemplateFile('jcrrepositorysearch.pt')

    @property
    def portal_catalog(self):
        return getToolByName(self.context, 'portal_catalog')

    @property
    def portal(self):
        return getToolByName(self.context, 'portal_url').getPortalObject()


    # Methods called from the associated template
    
    def test(self):
        """
        test method
        """
        dummy = _(u'a dummy string')
        
        return {'dummy': dummy}
