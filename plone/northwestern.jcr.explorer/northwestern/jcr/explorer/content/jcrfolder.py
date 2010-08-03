"""Definition of the JCRFolder content type
"""
import urllib2
import base64
import elementtree.ElementTree as ET
import transaction

import simplejson as json

from zope.interface import implements, directlyProvides

from Products.Archetypes import atapi

from Products.ATContentTypes.content import folder
from Products.ATContentTypes.content import schemata

from Products.CMFCore.utils import getToolByName

from AccessControl import ClassSecurityInfo
from AccessControl import getSecurityManager

from Products.CMFCore import permissions

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IJCRFolder
from northwestern.jcr.explorer.config import PROJECTNAME

from northwestern.jcr.explorer.content.jcrimage import JCRImage
from northwestern.jcr.explorer.content.jcrdocument import JCRDocument


JCRFolderSchema = folder.ATFolderSchema.copy() + atapi.Schema((

    # -*- Your Archetypes field definitions here ... -*-
    atapi.TextField('url',
          required=False,
          searchable=True,
          storage=atapi.AnnotationStorage(),
          # validators=('isTidyHtmlWithCleanup',),
          # default_output_type='text/x-html-safe',
          # widget=atapi.RichWidget(label=_(u"URL"),
          #                         description=_(u""),
          #                         rows=25,
          #                         allow_file_upload=False),
          widget=atapi.StringWidget(label=_(u"URL"),
                                    description=_(u"")),
          )
))

# Set storage on fields copied from ATFolderSchema, making sure
# they work well with the python bridge properties.

JCRFolderSchema['title'].storage = atapi.AnnotationStorage()
JCRFolderSchema['description'].storage = atapi.AnnotationStorage()

schemata.finalizeATCTSchema(
    JCRFolderSchema,
    folderish=True,
    moveDiscussion=False
)

class JCRFolder(folder.ATFolder):
    """A folder which can contain films."""
    security = ClassSecurityInfo()
    implements(IJCRFolder)

    meta_type = "JCRFolder"
    _at_rename_after_creation = True
    schema = JCRFolderSchema

    title = atapi.ATFieldProperty('title')
    description = atapi.ATFieldProperty('description')
    
    url = atapi.ATFieldProperty('url')

    # -*- Your ATSchema to Python Property Bridges Here ... -*-

    def load_folders(self):
        relativePath = '/' + '/'.join(self.getPhysicalPath()[3:])

        print "url in folder: ", self.url

        if self.url.find('sauer') > -1:
            relativePath = '/' + '/'.join(self.getPhysicalPath()[4:])
            print relativePath
            self.load_folders_http(relativePath)
        else:
            self.load_folders_webdav(relativePath)

    def load_folders_http(self, relativePath):
        # read from the Apache server

        if self.url.find("access") > -1:
            user = getSecurityManager().getUser()
            name = user.getUserName()
            # shelf in home folder but content on the server
            path = self.url + "/" + name + relativePath.replace("shelf", "content")
        else:
            path = self.url + relativePath

        jsonStr = self.retrieveContent(path + '/contents.json')

        list = []

        if jsonStr == '':
            return

        jsonData = json.loads(jsonStr)
        
        if jsonData["children"] is None:
            return

        types_tool = getToolByName(self, 'portal_types')

        i = 0
        for child in jsonData["children"]:
            # use 100 for the demo
            if i >= 100:
                break

            tag = child["name"]
            title = child["title"]
     
            if tag not in self:
                itemPath = path + '/' + tag
                new_id = types_tool.constructContent('JCRImage', self, tag, None, title=title)
                transaction.savepoint(optimistic=True)
            
                try:
                    e = getattr(self, new_id)
                    e.url = itemPath
                except:
                    print "object not created: ", new_id
            # end of if not in context

            i += 1
        # end of loop over children

    def load_folders_webdav(self, relativePath):
        path = self.url + '/jcr%3aroot' + relativePath

        request = urllib2.Request(path)
        request.add_header('Depth', '1')
        request.get_method = lambda: 'PROPFIND'
        base64string = base64.encodestring('%s:%s' % ('admin', 'abc'))[:-1]
        authheader =  "Basic %s" % base64string
        request.add_header("Authorization", authheader)
        try:
            url = urllib2.urlopen(request)
        except IOError, e:
            pass
        if not hasattr(e, 'code') or e.code != 207:
            # we got an error - but not a 207 error
            # server not reachable
            print 'Server not available'

            self.description = 'Offline mode (repository not available)'

            return

        self.description = ''

        if e.code == 207:
            data = e
        else:
            data = url
        tree = ET.parse(data)
        root = tree.getroot()
        list = []
        
        links = False

        types_tool = getToolByName(self, 'portal_types')            	

        for i in range(1, len(root)):
            # skip the entry for itself
            node = root[i]
            # tag = node.tag
            if node[1][0][4].text is not None:
                # property
                tag = node[1][0][4].text
                if tag == 'String':
                    tag = node[1][0][3].text
                if node[1][0][0].text == 'fileLocation':
                    links = True
                # list.append( dict(url = tag + '/view', title = tag, address = '',) )
                continue


            # JSON interpretation
            elif node[1][0][0].text is not None and node[1][0][4][0][0].text != 'nt:file':
                # dcr:name, dcr:nodetypename
                tag = node[1][0][0].text
                if tag == 'jcr:system' or tag == self.title:
                    # skip system node and current node
                    continue
                tag = tag.replace(':', '_')

            
                # # JSON interpretation
                # jsonStr = self.retrieveContent(path + '/' + tag + '/contents.json')
                # if jsonStr != '':
                #     # folder with a child named 'contents.json'
                #     continue
                # # end of JSON interpretation
            
                if tag not in self:
                    # add to context
                    # e = JCRFolder(tag)
                    # self[tag] = e
                    # self._p_changed = True
            	    new_id = types_tool.constructContent('JCRFolder', self, tag, None, title=tag)
                    transaction.savepoint(optimistic=True)
                    try:
                        getattr(self, new_id).title = tag
                    except:
                        print "object not created: ", new_id
                    continue
            
                # jsonStr = self.retrieveContent(path + '/' + tag + '/contents.json/jcr:content/jcr:data')
                # if jsonStr != '' and not jsonStr.startswith('<?xml'):
                #     abc = json.loads(jsonStr)
                #     title = abc["metadata"]["title"]
                #     description = abc["metadata"]["description"]
                #     data = self.retrieveContent(path + '/' + tag + '/small.jpg/jcr:content/jcr:data')
                #     if data != '':
                #         tag = tag + '_content'
                # 
                #         e = JCRImage(tag)
                #         e.title = title
                #         e.description = description
                #         field = e.getPrimaryField()
                # 
                #         thumbnail_data = self.retrieveContent(path + '/' + tag + '/thumbnail.jpg/jcr:content/jcr:data')
                #         large_data = self.retrieveContent(path + '/' + tag + '/large.jpg/jcr:content/jcr:data')
                # 
                #         try:
                #             field.set(e, data)
                #             e.image_large = large_data
                #             e.image_preview = data
                #             e.image_thumb = thumbnail_data
                # 
                #         except IOError, exception:
                #             continue
                #         if tag not in self:
                #             print tag + ": " + title
                # 
                #             # add to context
                #             self[tag] = e
                #             self._p_changed = True
                #     continue
                # end of JSON interpretation

            if node[1][0][0].text is None or node[1][0][4][0][0].text != 'nt:file':
                # dcr:name, dcr:nodetypename
                continue
            tag = node[1][0][0].text
            if tag == 'jcr:system' or tag == self.title:
                # skip system node and current node
                continue
            tag = tag.replace(':', '_')

            if tag not in self:

            	mimeType = self.retrieveContent(path + '/' + tag + '/jcr:content/jcr:mimeType')
            	
            	data = self.retrieveContent(path + '/' + tag + '/jcr:content/jcr:data')
            	
            	if mimeType == 'image/jpeg':
            	    # image
            	    # field = e.getPrimaryField()
            	    # field.set(e, data)
            	
            	    new_id = types_tool.constructContent('JCRImage', self, tag, None, image=data, title=tag)
            	
            	elif mimeType == 'text/html' or mimeType == 'text/xml':
            	    # HTML page
            	
            	    new_id = types_tool.constructContent('JCRDocument', self, tag, None, text=data, title=tag)
            	else:
            	    # all other types
            	    new_id = types_tool.constructContent('File', self, tag, None, file=data, title=tag)

                # add to context
                # self.context[tag] = e
                # self.context._p_changed = True
                transaction.savepoint(optimistic=True)

                try:
                    item = getattr(self, new_id)
                    # if mimeType == 'text/html' or mimeType == 'text/xml':
                    #     item.text = data
                except:
                    print "object not created: ", new_id


        if links == True:
            request = urllib2.Request(path + '/fileLocation')
            base64string = base64.encodestring('%s:%s' % ('admin', 'abc'))[:-1]
            authheader =  "Basic %s" % base64string
            request.add_header("Authorization", authheader)
            try:
                url = urllib2.urlopen(request)
            except IOError, e:
                pass
            if not hasattr(e, 'code') or e.code != 207:
                # we got an error - but not a 207 error
                print 'Failed for another reason.'
                sys.exit(1)
            if e.code == 207:
                data = e
            else:
                data = url
            data = url
            tree = ET.parse(data)
            root = tree.getroot()
        
            for i in range(0, len(root)):
                tag = root[i].text
                fileName = tag.rsplit('/', 1)[1]

                if fileName not in self:
                    data = self.retrieveContent(tag)

                    types_tool = getToolByName(self, 'portal_types')
                    new_id = types_tool.constructContent('JCRImage', self, fileName, None, image=data)
            	
                    transaction.savepoint(optimistic=True)
                    try:
                        getattr(self, new_id).title = tag
                    except:
                        print "object not created: ", new_id


    def retrieveContent(self, url):
        request = urllib2.Request(url)
        base64string = base64.encodestring('%s:%s' % ('admin', 'abc'))[:-1]
        authheader =  "Basic %s" % base64string
        request.add_header("Authorization", authheader)
        try:
            url = urllib2.urlopen(request)
        except IOError, e:
            # pass
            print "error retrieving content!"
            return ''

        data = url.read()

        return data

    security.declarePublic('synchToRepository')
    def synchToRepository(self):
        """synchronize to repository"""
       
        jsonRequest = {}
        jsonRequest["header"] = {}
        jsonRequest["header"]["destinationPath"] = "/content"
        jsonRequest["header"]["password"] = "3041f65dbefebc61cd2623e14cdd1dfc"
        jsonRequest["header"]["basePath"] = self.absolute_url() # '/'.join(self.getPhysicalPath())
        jsonRequest["body"] = {}
        
        for child in self:
            jsonRequest["body"][self[child].id] = {}
            jsonRequest["body"][self[child].id]["path"] = "/" + self[child].id
            jsonRequest["body"][self[child].id]["uuid"] = ""
            jsonRequest["body"][self[child].id]["modified"] = str(self[child].modified())
            jsonRequest["body"][self[child].id]["properties"] = {}
            jsonRequest["body"][self[child].id]["properties"]["title"] = self[child].title
            jsonRequest["body"][self[child].id]["properties"]["url"] = self[child].url
            jsonRequest["body"][self[child].id]["properties"]["description"] = self[child].description
            

        print json.dumps(jsonRequest)

    def show(self):
        """test"""
 
        return str(self)

atapi.registerType(JCRFolder, PROJECTNAME)
