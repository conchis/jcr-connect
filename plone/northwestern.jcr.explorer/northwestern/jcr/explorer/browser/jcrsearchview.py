import transaction

from zope.interface import implements, Interface

from plone.memoize.instance import memoize

from Products.Five import BrowserView
from Products.Five.browser.pagetemplatefile import ViewPageTemplateFile
from Products.CMFCore.utils import getToolByName

from Products.CMFCore.utils import getToolByName

from AccessControl import getSecurityManager

from northwestern.jcr.explorer import explorerMessageFactory as _


class IJCRSearchView(Interface):
    """
    JCRSearch view interface
    """

    def test():
        """ test method"""


class JCRSearchView(BrowserView):
    """
    JCRSearch browser view
    """
    implements(IJCRSearchView)

    def __init__(self, context, request):
        self.context = context
        self.request = request

    __call__ = ViewPageTemplateFile('jcrsearchview.pt')


    @property
    def portal_catalog(self):
        return getToolByName(self.context, 'portal_catalog')

    @property
    def portal(self):
        return getToolByName(self.context, 'portal_url').getPortalObject()

    @memoize
    def createFolder(self):
        tool = getToolByName(self.context, 'portal_membership')
        folder = tool.getHomeFolder()
        types_tool = getToolByName(self.context, 'portal_types')            	

        tag = "shelf"

        if tag not in folder:
            new_id = types_tool.constructContent('JCRFolder', folder, tag, None, title=tag)
            transaction.savepoint(optimistic=True)

        try:
            e = getattr(folder, tag)
            e.url = self.context.accessservice
            e.searchservice = []
            user = getSecurityManager().getUser()
            name = user.getUserName()
            # use the shelf service as the search service for the folder as it returns JSON in the same format
            e.searchservice.append(self.context.shelfservice.rstrip('/') + '/' + name)
            e.webdavservice = self.context.webdavservice
            e.username = self.context.username
            e.password = self.context.password
            e.term = ""
            self.context.userName = name
        except Exception, inst:
            print "object not created: ", new_id

        return False

    @memoize
    def getUserName(self):
        user = getSecurityManager().getUser()
        name = user.getUserName()
        
        return name

    def test(self):
        """
        test method
        """
        dummy = _(u'a dummy string')

        return {'dummy': dummy}
