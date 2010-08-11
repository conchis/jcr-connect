# 
#   Copyright 2010 Northwestern University.
# 
# Licensed under the Educational Community License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the
# License. You may obtain a copy of the License at
# 
#    http://www.osedu.org/licenses/ECL-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
# 
# Author: Xin Xiang, Rick Moore
# 

"""Browser view for the JCRFolder content type
"""

from zope.interface import implements, Interface

from plone.memoize.instance import memoize

from Products.Five import BrowserView
from Products.Five.browser.pagetemplatefile import ViewPageTemplateFile


from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IJCRFolder
from northwestern.jcr.explorer.content.jcrfolder import JCRFolder


class IJCRFolderView(Interface):
    """
    JCRFolder view interface
    """
    def have_jcr_files():
        """ """

    def have_jcr_folders():
        """ """

    def jcr_files():
        """ """

    def jcr_folders():
        """ """

    def test():
        """ test method"""


class JCRFolderView(BrowserView):
    """
    JCRFolder browser view
    """
    implements(IJCRFolderView)

    def __init__(self, context, request):
        self.context = context
        self.request = request

    __call__ = ViewPageTemplateFile('jcrfolderview.pt')

    @property
    def portal_catalog(self):
        return getToolByName(self.context, 'portal_catalog')

    @property
    def portal(self):
        return getToolByName(self.context, 'portal_url').getPortalObject()

    # Methods called from the associated template
    
    def have_jcr_folders(self):
        return len(self.jcr_folders()) > 0
    
    # The memoize decorator means that the function will be executed only
    # once (for a given set of arguments, but in this case there are no
    # arguments). On subsequent calls, the return value is looked up from a
    # cache, meaning we can call this function several times without a 
    # performance hit.

    @memoize
    def load_contents(self):
        self.context.load_folders()
        
    def jcr_folders(self):
        self.load_contents()

        list = []
        for tag in self.context:
            if isinstance(self.context[tag], JCRFolder):
                list.append( dict(url = tag, title = tag, description = '',) )
        return list

    def have_jcr_files(self):
        return len(self.jcr_files()) > 0

    def jcr_files(self):
        self.load_contents()

        list = []
        for tag in self.context:
            if not isinstance(self.context[tag], JCRFolder):
                list.append( dict(url = tag, title = self.context[tag].title, description = '',) )
        return list
    
    def test(self):
        """
        test method
        """
        dummy = _(u'a dummy string')

        return {'dummy': dummy}
