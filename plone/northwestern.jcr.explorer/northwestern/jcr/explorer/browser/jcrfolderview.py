"""Browser view for the JCRFolder content type
"""
# import urllib2
# import base64
# import elementtree.ElementTree as ET
# import transaction
# 
# import simplejson as json

# from Acquisition import aq_inner

from zope.interface import implements, Interface

from plone.memoize.instance import memoize

from Products.Five import BrowserView
from Products.Five.browser.pagetemplatefile import ViewPageTemplateFile

# from Products.CMFCore.utils import getToolByName

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IJCRFolder
# from northwestern.jcr.explorer.interfaces import IJCRImage
from northwestern.jcr.explorer.content.jcrfolder import JCRFolder
# from northwestern.jcr.explorer.content.jcrimage import JCRImage


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

    # @memoize
    # def jcr_folders1(self):
    #     context = aq_inner(self.context)
    #     catalog = getToolByName(context, 'portal_catalog')
    # 
    #     context.load_folders()
    # 
    #     # path = '/'.join(context.getPhysicalPath()).replace('/Plone/jcr-fedora', '')
    #     path = '/' + '/'.join(context.getPhysicalPath()[3:])
    #     # request = urllib2.Request('http://129.105.110.205:8080/server/fedora/jcr%3aroot' + path)
    #     request = urllib2.Request(context.url + '/jcr%3aroot' + path)
    #     request.add_header('Depth', '1')
    #     request.get_method = lambda: 'PROPFIND'
    #     base64string = base64.encodestring('%s:%s' % ('admin', 'abc'))[:-1]
    #     authheader =  "Basic %s" % base64string
    #     request.add_header("Authorization", authheader)
    #     try:
    #         url = urllib2.urlopen(request)
    #     except IOError, e:
    #         pass
    #     if not hasattr(e, 'code') or e.code != 207:
    #         # we got an error - but not a 207 error
    #         # server not reachable
    #         print 'Server not available'
    # 
    #         context.description = 'Offline mode (repository not available)'
    # 
    #         list = []
    #         for tag in self.context:
    #             if isinstance(self.context[tag], JCRFolder):
    #                 list.append( dict(url = tag, title = tag, description = '',) )
    #         return list
    # 
    #     context.description = ''
    # 
    # 
    #     # temporarily read from the Apache server
    #     if context.url.find('default') > -1:
    #         # one level only
    #         return []
    # 
    # 
    #     if e.code == 207:
    #         data = e
    #     else:
    #         data = url
    #     tree = ET.parse(data)
    #     root = tree.getroot()
    #     list = []
    #     for i in range(1, len(root)):
    #         # skip the entry for itself
    #         node = root[i]
    #         # tag = node.tag
    #         if node[1][0][4].text is not None:
    #             # property
    #             continue
    #         if node[1][0][0].text is None or node[1][0][4][0][0].text == 'nt:file':
    #             # dcr:name, dcr:nodetypename
    #             continue
    #         tag = node[1][0][0].text
    #         if tag == 'jcr:system' or tag == self.context.title:
    #             # skip system node and current node
    #             continue
    # 
    # 
    #         # JSON interpretation
    #         jsonStr = self.retrieveContent(context.url + '/jcr%3aroot' + path + '/' + tag + '/contents.json')
    #         if jsonStr != '':
    #             # folder with a child named 'contents.json'
    #             continue
    #         # end of JSON interpretation
    # 
    # 
    #         tag = tag.replace(':', '_')
    #         e = JCRFolder(tag)
    #         if tag not in self.context:
    #             # add to context
    #             self.context[tag] = e
    #             self.context._p_changed = True
    #         # always add to the list
    #         list.append( dict(url = tag, title = tag, description = '',) )
    # 
    #     return list

    def have_jcr_files(self):
        return len(self.jcr_files()) > 0

    def jcr_files(self):
        self.load_contents()

        list = []
        for tag in self.context:
            if not isinstance(self.context[tag], JCRFolder):
                list.append( dict(url = tag, title = self.context[tag].title, description = '',) )
        return list
    
    # @memoize
    # def jcr_files1(self):
    #     context = aq_inner(self.context)
    #     # catalog = getToolByName(context, 'portal_catalog')
    # 
    #     # Note that we are cheating a bit here - to avoid having to "wake up"
    #     # the cinema object, we rely on our implementation that uses the 
    #     # Dublin Core Title and Description fields as title and address,
    #     # respectively. To rely only on the interface and not the 
    #     # implementation, we'd need to call getObject() and then use the
    #     # associated attributes of the interface, or we could add new catalog
    #     # metadata for these fields (with a catalog.xml GenericSetup file).
    # 
    #     # path = '/'.join(context.getPhysicalPath()).replace('/Plone/jcr-fedora', '')
    #     path = '/' + '/'.join(context.getPhysicalPath()[3:])
    #     # request = urllib2.Request('http://129.105.110.205:8080/server/fedora/jcr%3aroot' + path)
    #     request = urllib2.Request(context.url + '/jcr%3aroot' + path)
    #     request.add_header('Depth', '1')
    #     request.get_method = lambda: 'PROPFIND'
    #     base64string = base64.encodestring('%s:%s' % ('admin', 'abc'))[:-1]
    #     authheader =  "Basic %s" % base64string
    #     request.add_header("Authorization", authheader)
    #     try:
    #         url = urllib2.urlopen(request)
    #     except IOError, e:
    #         pass
    #     if not hasattr(e, 'code') or e.code != 207:
    #         # we got an error - but not a 207 error
    #         # server not reachable
    #         print 'Server not available'
    # 
    #         list = []
    #         for tag in self.context:
    #             if not isinstance(self.context[tag], JCRFolder):
    #                 list.append( dict(url = tag + '/view', title = tag, address = '',) )
    #         return list
    # 
    # 
    #     # temporarily read from the Apache server
    #     if context.url.find('default') > -1:
    #         jsonStr = self.retrieveContent('http://sauer.at.northwestern.edu/chicago' + path + '/contents.json')
    # 
    #         list = []
    # 
    #         if jsonStr == '':
    #             return list
    # 
    #         jsonData = json.loads(jsonStr)
    #         
    #         if jsonData["children"] is None:
    #             return list
    # 
    #         i = 0
    #         for child in jsonData["children"]:
    #             if i >= 100:
    #                 break
    # 
    #             tag = child["name"]
    #             title = child["title"]
    #  
    #             if tag not in self.context:
    #                 e = JCRImage(tag)
    #                 e.title = title
    # 
    #                 # use teaser for image URL
    #                 e.teaser = 'http://sauer.at.northwestern.edu/chicago' + path + '/' + tag
    # 
    #                 # add to context
    #                 self.context[tag] = e
    #             # end of if not in context
    #             # always add to the list
    #             list.append( dict(url = tag + '/view', title = title, address = '',) )                
    # 
    #             i += 1
    #             # end of loop over children
    #         # persist
    #         self.context._p_changed = True
    # 
    #         return list
    #     #  temporarily read from the Apache server
    # 
    # 
    #     if e.code == 207:
    #         data = e
    #     else:
    #         data = url
    #     tree = ET.parse(data)
    #     root = tree.getroot()
    #     list = []
    #     
    #     links = False
    # 
    #     for i in range(1, len(root)):
    #         # skip the entry for itself
    #         node = root[i]
    #         # tag = node.tag
    #         if node[1][0][4].text is not None:
    #             # property
    #             tag = node[1][0][4].text
    #             if tag == 'String':
    #                 tag = node[1][0][3].text
    #             if node[1][0][0].text == 'fileLocation':
    #                 links = True
    #             # list.append( dict(url = tag + '/view', title = tag, address = '',) )
    #             continue
    # 
    # 
    # 
    #         # JSON interpretation
    #         elif node[1][0][0].text is not None and node[1][0][4][0][0].text != 'nt:file':
    #             # dcr:name, dcr:nodetypename
    #             tag = node[1][0][0].text
    #             if tag == 'jcr:system' or tag == self.context.title:
    #                 # skip system node and current node
    #                 continue
    #             tag = tag.replace(':', '_')
    # 
    #             jsonStr = self.retrieveContent(context.url + '/jcr%3aroot' + path + '/' + tag + '/contents.json/jcr:content/jcr:data')
    #             if jsonStr != '' and not jsonStr.startswith('<?xml'):
    #                 abc = json.loads(jsonStr)
    #                 title = abc["metadata"]["title"]
    #                 description = abc["metadata"]["description"]
    #                 data = self.retrieveContent(context.url + '/jcr%3aroot' + path + '/' + tag + '/small.jpg/jcr:content/jcr:data')
    #                 if data != '':
    #                     tag = tag + '_content'
    # 
    #                     e = JCRImage(tag)
    #                     e.title = title
    #                     e.description = description
    #                     field = e.getPrimaryField()
    # 
    #                     thumbnail_data = self.retrieveContent(context.url + '/jcr%3aroot' + path + '/' + tag + '/thumbnail.jpg/jcr:content/jcr:data')
    #                     large_data = self.retrieveContent(context.url + '/jcr%3aroot' + path + '/' + tag + '/large.jpg/jcr:content/jcr:data')
    # 
    #                     try:
    #                         field.set(e, data)
    #                         e.image_large = large_data
    #                         e.image_preview = data
    #                         e.image_thumb = thumbnail_data
    # 
    #                     except IOError, exception:
    #                         continue
    #                     if tag not in self.context:
    #                         print tag + ": " + title
    # 
    #                         # add to context
    #                         self.context[tag] = e
    #                         self.context._p_changed = True
    #                     # always add to the list
    #                     list.append( dict(url = tag + '/view', title = title, address = '',) )
    #             continue
    #         # end of JSON interpretation
    # 
    # 
    # 
    #         if node[1][0][0].text is None or node[1][0][4][0][0].text != 'nt:file':
    #             # dcr:name, dcr:nodetypename
    #             continue
    #         tag = node[1][0][0].text
    #         if tag == 'jcr:system' or tag == self.context.title:
    #             # skip system node and current node
    #             continue
    #         tag = tag.replace(':', '_')
    # 
    #         if tag not in self.context:
    # 
    #         	mimeType = self.retrieveContent(context.url + '/jcr%3aroot' + path + '/' + tag + '/jcr:content/jcr:mimeType')
    #         	
    #         	data = self.retrieveContent(context.url + '/jcr%3aroot' + path + '/' + tag + '/jcr:content/jcr:data')
    #         	
    #         	types_tool = getToolByName(self, 'portal_types')
    #         	
    #         	if mimeType == 'image/jpeg':
    #         	    # image
    #         	    # field = e.getPrimaryField()
    #         	    # field.set(e, data)
    #         	
    #         	    new_id = types_tool.constructContent('JCRImage', self.context, tag, None, image=data)
    #         	
    #         	elif mimeType == 'text/html':
    #         	    # HTML page
    #         	
    #         	    new_id = types_tool.constructContent('Document', self.context, tag, None, text=data)
    #         	else:
    #         	    # all other types
    #         	    new_id = types_tool.constructContent('File', self.context, tag, None, file=data)
    # 
    #             # add to context
    #             # self.context[tag] = e
    #             # self.context._p_changed = True
    #             transaction.savepoint(optimistic=True)
    # 
    #             try:
    #                 getattr(self.context, new_id)
    #             except:
    #                 print "object not created: ", new_id
    # 
    #         # always add to the list
    #         list.append( dict(url = tag + '/view', title = tag, address = '',) )
    # 
    #     if links == True:
    #         request = urllib2.Request(context.url + '/jcr%3aroot' + path + '/fileLocation')
    #         base64string = base64.encodestring('%s:%s' % ('admin', 'abc'))[:-1]
    #         authheader =  "Basic %s" % base64string
    #         request.add_header("Authorization", authheader)
    #         try:
    #             url = urllib2.urlopen(request)
    #         except IOError, e:
    #             pass
    #         if not hasattr(e, 'code') or e.code != 207:
    #             # we got an error - but not a 207 error
    #             print 'Failed for another reason.'
    #             sys.exit(1)
    #         if e.code == 207:
    #             data = e
    #         else:
    #             data = url
    #         data = url
    #         tree = ET.parse(data)
    #         root = tree.getroot()
    #     
    #         for i in range(0, len(root)):
    #             tag = root[i].text
    #             list.append( dict(url = tag, title = tag, address = '',) )
    # 
    #     return list
    # 
    # def retrieveContent(self, url):
    #     request = urllib2.Request(url)
    #     base64string = base64.encodestring('%s:%s' % ('admin', 'abc'))[:-1]
    #     authheader =  "Basic %s" % base64string
    #     request.add_header("Authorization", authheader)
    #     try:
    #         url = urllib2.urlopen(request)
    #     except IOError, e:
    #         # pass
    #         print "error retrieving content!"
    #         return ''
    # 
    #     data = url.read()
    # 
    #     return data

    def test(self):
        """
        test method
        """
        dummy = _(u'a dummy string')

        return {'dummy': dummy}
