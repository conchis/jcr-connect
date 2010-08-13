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


"""Definition of the JCRFolder content type
"""
from time import time
import datetime
import urllib
import urllib2
import httplib
import base64
import elementtree.ElementTree as ET
import transaction
import re

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
          widget=atapi.StringWidget(label=_(u"URL"),
                                    description=_(u"")),
          ),
    atapi.TextField('syncURL',
          required=False,
          searchable=True,
          storage=atapi.AnnotationStorage(),
          widget=atapi.StringWidget(label=_(u"Synchronization URL"),
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


message = ""
progress = 1


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
    syncURL = atapi.ATFieldProperty('syncURL')

    # -*- Your ATSchema to Python Property Bridges Here ... -*-

    def load_folders(self):
        relativePath = '/' + '/'.join(self.getPhysicalPath()[3:])

        # if self.url.find('sauer') > -1:
        relativePath = '/' + '/'.join(self.getPhysicalPath()[4:])
        self.load_folders_http(relativePath)
        # else:
        #     self.load_folders_webdav(relativePath)

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
                    # record local created time
                    if "modified" in child:
                        e.lastModified = child["modified"]
                    else:
                        e.lastModified = str(e.modified())
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
            	    new_id = types_tool.constructContent('JCRImage', self, tag, None, image=data, title=tag)
            	
            	elif mimeType == 'text/html' or mimeType == 'text/xml':
            	    # HTML page
            	
            	    new_id = types_tool.constructContent('JCRDocument', self, tag, None, text=data, title=tag)
            	else:
            	    # all other types
            	    new_id = types_tool.constructContent('File', self, tag, None, file=data, title=tag)

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

    def getInterval(self):
        # get currentTimeMillis
        currentServerTime = self.retrieveContent(self.syncURL)
        
        if currentServerTime != '':
            currentServerTime = int(currentServerTime) / 1000
        else:
            # server time is not available
            return datetime.timedelta(seconds=0)
        
        currentClientTime = time()

        interval = datetime.datetime.fromtimestamp(currentServerTime) - datetime.datetime.fromtimestamp(currentClientTime)
        print str(interval)
        return interval
        

    security.declarePublic('synchToRepository')
    def synchToRepository(self):
        """synchronize to repository"""
       
        jsonRequest = {}
        jsonRequest["header"] = {}
        jsonRequest["header"]["destinationPath"] = re.split("Members/\w+", self.absolute_url())[1]
        jsonRequest["header"]["password"] = "3041f65dbefebc61cd2623e14cdd1dfc"
        jsonRequest["header"]["basePath"] = self.absolute_url()
        jsonRequest["header"]["repositoryURL"] = self.url
        jsonRequest["body"] = {}

        # get the time difference
        interval = self.getInterval()
        
        for child in self:
            if isinstance(self[child], JCRFolder):
                # ignore folder
                continue

            jsonRequest["body"][self[child].id] = {}
            jsonRequest["body"][self[child].id]["path"] = "/" + self[child].id
            jsonRequest["body"][self[child].id]["uuid"] = ""

            timestamp = datetime.datetime.fromtimestamp(self[child].modified())

            if self[child].lastModified == '':
                # new object
                self[child].lastModified = str(self[child].modified())
                timestamp += interval
            elif self[child].lastModified == str(self[child].modified()):
                # hasn't changed since creation from repository
                timestamp = datetime.datetime(1970, 01, 01, 0, 0, 0)
            else:
                # changed
                self[child].lastModified = str(self[child].modified())
                timestamp += interval

            # print str(timestamp)

            jsonRequest["body"][self[child].id]["modified"] = str(timestamp)

            if isinstance(self[child], JCRImage):
                if self[child].tileURL == '':
                    jsonRequest["body"][self[child].id]["type"] = "BinaryImage"
                else:
                    jsonRequest["body"][self[child].id]["type"] = "TiledImage"
            elif isinstance(self[child], AnnotatedImage):
                jsonRequest["body"][self[child].id]["type"] = "AnnotatedImage"
            elif isinstance(self[child], JCRDocument):
                jsonRequest["body"][self[child].id]["type"] = "Document"
                
            jsonRequest["body"][self[child].id]["properties"] = {}
            jsonRequest["body"][self[child].id]["properties"]["title"] = self[child].title
            jsonRequest["body"][self[child].id]["properties"]["url"] = self[child].url
            jsonRequest["body"][self[child].id]["properties"]["description"] = self[child].description
            
        # print json.dumps(jsonRequest)

        global message, progress
        message = "synchronizing ..."
        progress = 1

        params = urllib.urlencode({'manifest': json.dumps(jsonRequest), 'path': "/def"})
        req = urllib2.Request(self.syncURL, params)
        try:
            response = urllib2.urlopen(req)
        except httplib.HTTPException:
            # pass
            print "HTTPException"
            return ""
        except Exception, inst:
            print type(inst)     # the exception instance
            print inst.args      # arguments stored in .args
            print inst           # __str__ allows args to printed directly
            return ""
        return "true"

    security.declarePublic('getMessage')
    def getMessage(self):
        """ returns the message """
        global message, progress

        if progress == 100:
            m = message
            message = ""
            progress = 1
            return m
        return message
    
    security.declarePublic('getProgress')
    def getProgress(self):
        """ returns the progress """
        global message, progress

        if progress == 100:
            progress = 1
            message = ""
            return 100
        return progress

    security.declarePublic('updateSyncProgress')
    def updateSyncProgress(self, n, c, t):
        """update the progress of synchronization"""

        name = n
        completed = c
        total = t
        
        global message, progress

        if completed == total:
            message = "finished synchronizing " + total + " items"
            progress = 100
        else:
            message = "synchronizing " + name + " ... " + completed + "/" + total + " completed"
            progress = 100 * int(completed) / int(total)

    def show(self):
        """test"""
 
        return str(self)

atapi.registerType(JCRFolder, PROJECTNAME)
