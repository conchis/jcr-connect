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
# Author: Jonathan Smith, Xin Xiang, Rick Moore
# 

"""Definition of the AnnotatedImage content type
"""

from zope.interface import implements, directlyProvides

from Products.Archetypes import atapi
from Products.ATContentTypes.content import base
from Products.ATContentTypes.content import schemata

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.interfaces import IAnnotatedImage
from northwestern.jcr.explorer.config import PROJECTNAME

import string

AnnotatedImageSchema = schemata.ATContentTypeSchema.copy() + atapi.Schema((

    # -*- Your Archetypes field definitions here ... -*-
        atapi.StringField ("image",          # Image URL
                     searchable=0,
                     widget=atapi.StringWidget()
                     ),
        atapi.StringField("description",     # Description text
                    searchable=1,
                    widget=atapi.StringWidget(),
                    ),        
        atapi.IntegerField("left",           # Left position in image coordinates
                     searchable=0,
                     widget=atapi.IntegerWidget(
                visible = {'view' : 'invisible', 'edit' : 'invisible'})
                     ),
        atapi.IntegerField("top",            # Top position in image coordinates
                     searchable=0,
                     widget=atapi.IntegerWidget(
                visible = {'view' : 'invisible', 'edit' : 'invisible'})
                     ),
        atapi.IntegerField("width",          # Width in image coordinates
                     searchable=0,
                     widget=atapi.IntegerWidget(
                visible = {'view' : 'invisible', 'edit' : 'invisible'})
                     ),
        atapi.IntegerField("height",         # Height in image coordinates
                     searchable=0,
                     widget=atapi.IntegerWidget(
                visible = {'view' : 'invisible', 'edit' : 'invisible'})
                     ),
        atapi.IntegerField("color_index",    # Index of marker color
                     searchable=0,
                     widget=atapi.IntegerWidget(
                visible = {'view' : 'invisible', 'edit' : 'invisible'})
                     ),
        atapi.ObjectField("details",
                    widget=atapi.StringWidget(
                visible = {'view' : 'invisible', 'edit' : 'invisible'})
                    )
))

# Set storage on fields copied from ATFolderSchema, making sure
# they work well with the python bridge properties.

AnnotatedImageSchema['title'].storage = atapi.AnnotationStorage()
AnnotatedImageSchema['description'].storage = atapi.AnnotationStorage()

schemata.finalizeATCTSchema(AnnotatedImageSchema, moveDiscussion=False)

class AnnotatedImage(base.ATCTContent):
    """Description of the Example Type"""
    implements(IAnnotatedImage)

    meta_type = "AnnotatedImage"
    schema = AnnotatedImageSchema

    title = atapi.ATFieldProperty('title')
    description = atapi.ATFieldProperty('description')
    
    # -*- Your ATSchema to Python Property Bridges Here ... -*-
    def _setImage(self, image_url):
        """Set the image url"""
        self.getField("image").set(self, image_url)
        
    def _setDetails(self, details):
        """Set the details field and extract note information"""
        self.setTitle(self.decodeString(details["title"]))
        self._renameAfterCreation()
        self.getField("details").set(self, details)
        self.getField("description").set(self, details["description"])
        self.getField("color_index").set(self, details["color_index"])
        bounds = details["bounds"]
        self.getField("left").set(self, bounds["left"])
        self.getField("top").set(self, bounds["top"])
        self.getField("width").set(self, bounds["width"])
        self.getField("height").set(self, bounds["height"])
        
    def _getDetails(self):
        """Returns the note's details"""
        return self.getField("details").get(self)
        
    def _getClientId(self):
        """Returns the note's id from note details"""
        return self.getField("details").get(self)["id"]
        
    def bounds(self):
        """Return a string with left, top, width, height"""
        left   = self.getField("left"  ).get(self)
        top    = self.getField("top"   ).get(self)
        width  = self.getField("width" ).get(self)
        height = self.getField("height").get(self)
        return "%i, %i, %i, %i" % (left, top, width, height)

    def characterForEntity(self, entity_string):
        """Convert an XML character entity to a character"""
        digits = entity_string[3:entity_string.find(";", 3)]
        return unichr(int(digits, 16))

    def decodeString(self, text):
        result = []
        start = 0
        while True:
            entity_start = text.find("&#x", start)
            if entity_start < 0: 
                result.append(text[start:])
                break
            result.append(text[start:entity_start])
            entity_end = text.find(";", entity_start)
            result.append(self.characterForEntity(text[entity_start:entity_end + 1]))
            start = entity_end + 1
        return string.join(result, "")

atapi.registerType(AnnotatedImage, PROJECTNAME)
