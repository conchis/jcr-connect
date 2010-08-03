from zope.interface import implements, Interface

from Products.Five import BrowserView
from Products.Five.browser.pagetemplatefile import ViewPageTemplateFile

from Products.CMFCore.utils import getToolByName

from northwestern.jcr.explorer import explorerMessageFactory as _


class IAnnotatedImageViewCollage(Interface):
    """
    AnnotatedImage view interface
    """

    def test():
        """ test method"""


class AnnotatedImageViewCollage(BrowserView):
    """
    AnnotatedImage browser view
    """
    implements(IAnnotatedImageViewCollage)

    def __init__(self, context, request):
        self.context = context
        self.request = request

    __call__ = ViewPageTemplateFile('annotatedimageviewcollage.pt')

    @property
    def portal_catalog(self):
        return getToolByName(self.context, 'portal_catalog')

    @property
    def portal(self):
        return getToolByName(self.context, 'portal_url').getPortalObject()

    def test(self):
        """
        test method
        """
        dummy = _(u'a dummy string')

        return {'dummy': dummy}
