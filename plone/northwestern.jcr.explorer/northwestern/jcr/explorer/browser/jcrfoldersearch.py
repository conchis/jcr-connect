"""Browser view for the JCRFolder content type.
"""

from Acquisition import aq_inner
from zope.interface import implements, Interface

from plone.memoize.instance import memoize

from Products.Five import BrowserView
from Products.Five.browser.pagetemplatefile import ViewPageTemplateFile

from Products.CMFCore.utils import getToolByName

from AccessControl import getSecurityManager

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IBannerProvider
from northwestern.jcr.explorer.content.jcrfolder import JCRFolder

class IJCRFolderSearch(Interface):
    """
    JCRFolder view interface
    """

    def test():
        """ test method"""


class JCRFolderSearch(BrowserView):
    """
    JCRFolder browser view
    """
    implements(IJCRFolderSearch)

    def __init__(self, context, request):
        self.context = context
        self.request = request

    __call__ = ViewPageTemplateFile('jcrfoldersearch.pt')

    @property
    def portal_catalog(self):
        return getToolByName(self.context, 'portal_catalog')

    @property
    def portal(self):
        return getToolByName(self.context, 'portal_url').getPortalObject()


    # Methods called from the associated template

    def getUserName(self):
        """
        Returns user name
        """
        user = getSecurityManager().getUser()
        name = user.getUserName()
        
        searchURL = "http://sauer.at.northwestern.edu:4004/category_marker/search/" + name

        return searchURL


    def test(self):
        """
        test method
        """
        dummy = _(u'a dummy string')
        
        return {'dummy': dummy}
