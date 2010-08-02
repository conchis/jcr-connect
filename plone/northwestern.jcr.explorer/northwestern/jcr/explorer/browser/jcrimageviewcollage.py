""" Browser view for the JCRImage content type.
"""

from Acquisition import aq_inner

from zope.interface import implements, Interface

from Products.Five import BrowserView
from Products.Five.browser.pagetemplatefile import ViewPageTemplateFile

from Products.CMFCore.utils import getToolByName

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IBannerProvider


class IJCRImageViewCollage(Interface):
    """
    JCRImage view interface
    """

    def banner_tag():
        """ """

    def test():
        """ test method"""


class JCRImageViewCollage(BrowserView):
    """
    JCRImage browser view
    """
    implements(IJCRImageViewCollage)

    def __init__(self, context, request):
        self.context = context
        self.request = request

    __call__ = ViewPageTemplateFile('jcrimageviewcollage.pt')

    @property
    def portal_catalog(self):
        return getToolByName(self.context, 'portal_catalog')

    @property
    def portal(self):
        return getToolByName(self.context, 'portal_url').getPortalObject()
    
    def banner_tag(self):
        context = aq_inner(self.context)
        banner_provider = IBannerProvider(context)
        return banner_provider.tag

    # The memoize decorator means that the function will be executed only
    # once (for a given set of arguments, but in this case there are no
    # arguments). On subsequent calls, the return value is looked up from a
    # cache, meaning we can call this function several times without a 
    # performance hit.
    
    # @memoize
    # def annotations(self):
    #     context = aq_inner(self.context)
    #     catalog = getToolByName(context, 'portal_catalog')
    # 
    #     idList = self.getNoteIds()
    # 
    #     for id in idList:
    #         print id

    def test(self):
        """
        test method
        """
        dummy = _(u'a dummy string')

        return {'dummy': dummy}
