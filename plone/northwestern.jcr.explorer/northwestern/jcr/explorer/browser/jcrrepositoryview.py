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

class IJCRRepositoryView(Interface):
    """
    JCRRepository view interface
    """
    def have_jcr_folders():
        """ """

    def jcr_folders():
        """ """

    def test():
        """ test method"""


class JCRRepositoryView(BrowserView):
    """
    JCRRepository browser view
    """
    implements(IJCRRepositoryView)

    def __init__(self, context, request):
        self.context = context
        self.request = request

    __call__ = ViewPageTemplateFile('jcrrepositoryview.pt')

    @property
    def portal_catalog(self):
        return getToolByName(self.context, 'portal_catalog')

    @property
    def portal(self):
        return getToolByName(self.context, 'portal_url').getPortalObject()


    # Methods called from the associated template
    
    def have_jcr_folders(self):
        return len(self.jcr_folders()) > 0

    @memoize
    def jcr_folders(self):
        self.context.load_folders()

        tool = getToolByName(self.context, 'portal_membership')
        folder = tool.getHomeFolder()

        # redirect to the content folder at home
        if self.context.url.find("access") > -1:
            tag = "shelf"
        else:
            tag = "content"
        self.context.REQUEST.RESPONSE.redirect(folder.absolute_url_path() + '/' + tag)

        list = []
        # for tag in folder:
        #     if isinstance(folder[tag], JCRFolder) and tag == "content":
        #         self.context.REQUEST.RESPONSE.redirect(folder.absolute_url_path() + '/' + tag)
        #         list.append( dict(url = (folder.absolute_url_path() + '/' + tag), title = tag, description = '',) )
        # 
        # # is this the right place ?
        # self.context.REQUEST.RESPONSE.redirect(folder.absolute_url_path() + '/' + tag)

        return list
    
    def test(self):
        """
        test method
        """
        dummy = _(u'a dummy string')
        
        return {'dummy': dummy}
