"""Definition of the JCRImage content type
"""
import urllib2
import base64
import simplejson as json
import transaction

from Acquisition import aq_base

from DateTime import DateTime

from zope.interface import implements, directlyProvides
from zope.component import adapts

from zope.schema.interfaces import IVocabularyFactory
from zope.schema.vocabulary import SimpleVocabulary

from Products.CMFCore.utils import getToolByName
from Products.CMFPlone.interfaces.NonStructuralFolder import INonStructuralFolder

from Products.Archetypes import atapi
from Products.Archetypes.utils import shasattr
from Products.Archetypes.interfaces import IObjectPostValidation

from Products.ATContentTypes.configuration import zconf
from Products.ATContentTypes.content import base
from Products.ATContentTypes.content import schemata
from Products.ATContentTypes.content import image
from Products.ATContentTypes.content import folder
from Products.ATContentTypes.content.schemata import ATContentTypeSchema

from Products.ATContentTypes.content.schemata import NextPreviousAwareSchema
from Products.ATContentTypes.lib.constraintypes import ConstrainTypesMixinSchema

from Products.validation import V_REQUIRED

from northwestern.jcr.explorer import explorerMessageFactory as _
from northwestern.jcr.explorer.config import PROJECTNAME
from northwestern.jcr.explorer.interfaces import IJCRImage
from northwestern.jcr.explorer.interfaces import IBannerProvider

from northwestern.jcr.explorer.content.annotatedimage import AnnotatedImage

JCRImageSchema = ATContentTypeSchema.copy() + atapi.Schema( (
# JCRImageSchema = folder.ATFolderSchema.copy() + atapi.Schema( (

    atapi.ImageField('image',
          # required=True,
          primary=True,
          languageIndependent=True,
          storage=atapi.AnnotationStorage(migrate=True),
          swallowResizeExceptions=zconf.swallowImageResizeExceptions.enable,
          pil_quality=zconf.pil_config.quality,
          pil_resize_algo=zconf.pil_config.resize_algo,
          # max_size=zconf.ATImage.max_image_dimension,
          # max_size=(400, 400),
          sizes= {'preview' : (512, 512),
                  #'mini'    : (200, 200),
                  'thumb'   : (128, 128),
                  'tile'    :  (64, 64),
                  'icon'    :  (32, 32),
                  'listing' :  (16, 16),
                  },
          validators=(('isNonEmptyFile', V_REQUIRED),),
          widget=atapi.ImageWidget(
                       description = '',
                       label= _(u'label_image', default=u'Image'),
                       show_content_type = False,)
          ),

    # atapi.StringField('filmCode',
    #       required=False,
    #       searchable=True,
    #       storage=atapi.AnnotationStorage(),
    #       widget=atapi.StringWidget(label=_(u"Film code"),
    #                    description=_(u"This should match the film code used in the "
    #                                   "booking system."))
    #       ),

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
          ),
       
    atapi.IntegerField ("count",
         default=0,
         searchable=0,
         widget=atapi.IntegerWidget (
                 visible = {'view' : 'invisible', 'edit' : 'invisible'})
         ),

    atapi.ObjectField("id_map",
         default = {},
         widget=atapi.StringWidget(
               visible = {'view' : 'invisible', 'edit' : 'invisible'})
         ),

    atapi.StringField("tileURL",   # Image URL
          searchable=0,
          widget=atapi.StringWidget()
          ),

), marshall=atapi.PrimaryFieldMarshaller()

)

# Set storage on fields copied from ATContentTypeSchema, making sure
# they work well with the python bridge properties.

JCRImageSchema['title'].storage = atapi.AnnotationStorage()
JCRImageSchema['title'].widget.label = _(u"Image name")
JCRImageSchema['title'].widget.description = _(u"")

JCRImageSchema['description'].storage = atapi.AnnotationStorage()
JCRImageSchema['description'].widget.label = _(u"Description")
JCRImageSchema['description'].widget.description = _(u"")

schemata.finalizeATCTSchema(JCRImageSchema, folderish=True, moveDiscussion=False)

class JCRImage(atapi.OrderedBaseFolder, image.ATImage):
# class JCRImage(folder.ATFolder, image.ATImage):
    """Information about films."""

    implements(IJCRImage)
    # __implements__ = folder.ATFolder.__implements__ + \
    __implements__ = atapi.OrderedBaseFolder.__implements__ + \
                     image.ATImage.__implements__ # + \
                     # (INonStructuralFolder,)

    meta_type = "JCRImage"
    archetype_name = "JCR Image"
    _at_rename_after_creation = True
    schema = JCRImageSchema

    title = atapi.ATFieldProperty('title')
    summary = atapi.ATFieldProperty('description')
    # film_code = atapi.ATFieldProperty('filmCode')
    # filmCode = ''
    url = atapi.ATFieldProperty('url')
    tileURL = atapi.ATFieldProperty('tileURL')
    tileURL = ''
    # shown_from = atapi.ATDateTimeFieldProperty('startDate')
    # shown_until = atapi.ATDateTimeFieldProperty('endDate')


    # -*- Your ATSchema to Python Property Bridges Here ... -*-

    def myMethod(self):
        """My test method.
        """
        return "Hello, world!"

# replaced the custom object management code in createNote with Plone folder's
# builtin OFS.ObjectManager functionality. Replaced note_data with **kwargs to
# take advantage of Python's builtin options dictionary functionality which
# Plone and Zope use to fill out content fields.
    def createNote(self, note_data):
        """Create a new note."""
        id = self._makeClientId()
        note_data['id'] = id
        note = AnnotatedImage(id)
        note._setImage(self.getField("tileURL").get(self))
        note._setDetails(note_data)
        self._setObject(id, note)
        self._registerId(id, id)
        self._p_changed = True
        print "note: %s" % (note, )
        return id

    #security.declareProtected(AddAnnotations, 'createAnnotation')
    # def createNote(self, note_data):
    #     """Create a new instance of AnnotatedImage"""
    #     # the anno_count attribute will exist for this instance only
    #     # after an annotation has been added.
    #     if hasattr(aq_base(self), 'anno_count'):
    #         self.anno_count+=1
    #     else:  # first annotation, add the counter to the instance
    #         self.anno_count = 1
    #     anno_id = "anno-%i" % self.anno_count
    #     types_tool = getToolByName(self, 'portal_types')
    #     new_id = types_tool.constructContent('AnnotatedImage', self,
    #                                          anno_id, None)
    #     transaction.savepoint(optimistic=True)
    #     try:
    #         new_annotation = getattr(self, new_id)
    #     except:
    #         return None
    #     new_annotation._setImage(self.getField("tileURL").get(self))
    #     new_annotation._setDetails(note_data)
    # 
    #     return new_annotation
    #     #return new_annotation.getId()

# replaced the custom object management code in getNote with Plone folder's
# builtin OFS.ObjectManager functionality
    def getNote(self, client_id):
        """Returns a note for a given client id"""

       # Get ImageNote object for id
        note = self._getNote(client_id)
        if note == None: return False

       # Return note details
        return note._getDetails()

    #security.declareProtected(View, 'getAnnotation')
    # def getNote(self, anno_id):
    #     """ Returns AnnotastionImage object that matches input ID.
    #     """
    #     if hasattr(aq_base(self), anno_id):
    #         note = getattr(self, anno_id)
    #     else:
    #         return False
    #     
    #     return note._getDetails()

# replaced the custom object management code in getNoteIds with Plone folder's
# builtin OFS.ObjectManager functionality
    def getNoteIds(self):
        """Returns a list of all note client ids"""
        self._updateIdMap()
        ids = self.getField("id_map").get(self).keys()
        print "getNoteIds: %s" % (ids, )
        return ids

    #security.declareProtected(View, 'getAnnotationIds')
    # def getNoteIds(self):
    #     """ Returns a list of the IDs of all AnnotatedImages
    #     """
    #     brains = self.searchMe()
    #     return [brain['id'] for brain in brains]

    #security.declareProtected(View, 'getAnnotations')
    # def getAnnotations(self, return_objects=False):
    #     """ Returns a brain or full object for each AnnotatedImage.
    #     """
    #     return self.searchMe(return_objects)

    #security.declareProtected(View, 'getMetadata')
    def getMetadata(self):
        """Return metadata associated with this image"""
        return { 
            "work_type"                 : self.getField("Work Type"                ).get(self),
            "work_type_english"         : self.getField("Work Type - English"      ).get(self),
            "dynasty"                   : self.getField("Dynasty"                  ).get(self),
            "dynasty_english"           : self.getField("Dynasty - English"        ).get(self),
            "item_date"                 : self.getField("Item Date"                ).get(self),
            "item_date_english"         : self.getField("Item Date - English"      ).get(self),
            "site"                      : self.getField("Site"                     ).get(self),
            "site_english"              : self.getField("Site - English"           ).get(self),
            "discovery_site"            : self.getField("Discovery Site"           ).get(self),
            "discovery_site_english"    : self.getField("Discovery Site - English" ).get(self),
            "material"                  : self.getField("Material"                 ).get(self),
            "material_english"          : self.getField("Material - English"       ).get(self),
            "technique"                 : self.getField("Technique"                ).get(self),
            "technique_english"         : self.getField("Technique - English"      ).get(self),
            "measurements"              : self.getField("Measurements"             ).get(self),
            "measurements_english"      : self.getField("Measurements - English"   ).get(self),
            "description"               : self.getDescription()(), #self.getField("Description"              ).get(self),
            "description_english"       : self.getField("Description - English"    ).get(self),
            "repository"                : self.getField("Repository"               ).get(self),
            "repository_english"        : self.getField("Repository - English"     ).get(self),
            "subject"                   : self.getField("Item Subject"             ).get(self),
            "subject_english"           : self.getField("Item Subject - English"   ).get(self),
            "collection"                : self.getField("Collection"               ).get(self),
            "collection_english"        : self.getField("Collection - English"     ).get(self),
            "source"                    : self.getField("Source"                   ).get(self),
            "source_english"            : self.getField("Source - English"         ).get(self),
            "image_url"                 : self.getField("tileURL"                    ).get(self)
        }

    #security.declareProtected(View, 'getUser')
    def getUser(self):
        """Return user information"""
        user = self.REQUEST.AUTHENTICATED_USER
        owner = self.getOwner()
        return { "_class": "ppad.xmlrpc.User",
                 "user":   user.getUserName(),
                 "owner":  owner.getUserName() }

# replaced the custom object management code in putNote with Plone folder's
# builtin OFS.ObjectManager functionality
    def putNote(self, note_data):
        """Replace a note"""
        
       # Get the client id
        client_id = note_data["id"]
       
       # get existing note, if not found create a new note
        note = self._getNote(client_id)
        if note == None:
            note = AnnotatedImage(client_id)
            self._setObject(client_id, note)
       
       # Set note details, update the id map
        note._setDetails(note_data)
        self._registerId(client_id, note.getId())
        
        return True

    #security.declareProtected(View, 'putAnnotation')
    # def putNote(self, note_data):
    #     """Replace an annotation"""
    #     if 'id' in note_data:
    #         obj = self.getNote(note_data['id'])
    #         if obj != None:
    #             obj._setDetails(note_data)
    #             return True
    # 
    #     obj = self.createNote(note_data)
    #     return True

# replaced the custom object management code in removeNote with Plone folder's
# builtin OFS.ObjectManager functionality
    def removeNote(self, client_id):
        """Removes a note with a specified id"""
        
        # Get ImageNote object for id
        note = self._getNote(client_id)
        if note == None: return False
       
        # Delete note
       #del self[note.getId()]
        self._delObject(note.getId())
        return True


    def _makeClientId(self):
        """Generate a uneque client id"""
        count = self.getField("count").get(self)
        self.getField("count").set(self, count + 1)
        return "note-%i" % count
        
    def _getNote(self, client_id):
        """Get an image note object by id. Refresh id map if needed."""
        
        # Fetch note for client id
        note = self._fetchNote(client_id)
        
        # If note not found, rebuild id map and fetch again
        if note == None:
            self._updateIdMap()
            note = self._fetchNote(client_id)
            
        # Return note or None
        return note
        
    def _fetchNote(self, client_id):
        """Fetch a note by client id"""
        
        # Get local id from id map, get note object
        id = self._getNoteId(client_id)
        if id == None: return None
        
        try:
            note = self[id]
        except KeyError:
            return None 
            
        # If found note with wrong client_id, return None
        if note._getClientId() != client_id:
            return None
            
        # Otherwise return the note
        return note
        
    def _registerId(self, client_id, note_id):
        """Set the mapping from client id to note id"""
        
        id_map = self.getField("id_map").get(self)
        id_map[client_id] = note_id
        self.getField("id_map").set(self, id_map)
        
    def _getNoteId(self, client_id):
        """Return a note id for a specified client id"""
        
        return self.getField("id_map").get(self).get(client_id, None)
        
    def _updateIdMap(self):
        """Update the map from client id to plone id"""
        
        id_map = {}
        for id in self.contentIds():
            note = self[id]
            if isinstance(note, AnnotatedImage):
                id_map[note._getClientId()] = note.getId()
                
        print "updated id map: %s" % id_map
        self.getField("id_map").set(self, id_map)



    #security.declareProtected(View, 'removeAnnotation')
    # def removeNote(self, anno_id):
    #     """Removes an annotation with a specified id"""
    # 
    #     if hasattr(aq_base(self), anno_id):
    #         self._delObject(anno_id)
    #         return True
    #     return False

    #security.declareProtected(View, 'retrieveContent')
    def retrieveContent(self, url):
        request = urllib2.Request(url)
        base64string = base64.encodestring('%s:%s' % ('admin', 'abc'))[:-1]
        authheader =  "Basic %s" % base64string
        request.add_header("Authorization", authheader)
        try:
            connection = urllib2.urlopen(request)
        except IOError, e:
            # pass
            return ''

        print "loading: " + url

        data = connection.read()

        return data

    #security.declareProtected(View, 'searchMe')
    def searchMe(self, return_objects=False, sort_on='getId', sort_order='',
                       portal_types=('AnnotatedImage',), **kwargs):
        """Uses portal catalog to search for content contained in this
        JCRImage. Additonal search criteria can be added using **kwargs.
        Default case returns a list of all of instances of AnnotatedImage,
        as catalog brains.
        """
        catalog = getToolByName(self, 'portal_catalog')
        path = '/'.join(self.getPhysicalPath())
        search_kwargs = { 'path' : path
                        , 'portal_type' : portal_types
                        }
        if sort_on:
            search_kwargs['sort_on'] = sort_on
            if sort_order:
                search_kwargs['sort_order'] = sort_order
        if kwargs:
            search_kwargs.extend(kwargs)
        lazy_map = catalog(**search_kwargs)
        if return_objects:
            return [brain.getObject() for brain in lazy_map]
        return [brain for brain in lazy_map]

    def tag(self, **kwargs):
        """Generate image tag using the api of the ImageField
        """
        # this is a REAL BAD IDEA !!!
        #
        # First Issue - it requires that all users of the system have the
        # ability to ModifyPortalContent, which means they can change anything,
        # not just the current image.
        #
        # Second Issue - it overwrites a scaled local copy of the binary image
        # with a string pointing outside Plone to a binary image. This will
        # eventually cause issues in functions that get a string when they are
        # expecting binary image data. e.g. __bobo_traverse__
        # 
        # Third Issue - if this is indeed the behavior you want (i.e. not
        # storing any binary images and only storing pointers to something
        # else), then it should be done in the constructor and JCRImage should
        # NOT be a subclass of ATIMage at all.
        if self.url is not '':
            if kwargs != {} and kwargs['scale'] == 'thumb' and not hasattr(self, 'image_thumb'):
                self.image_thumb = self.retrieveContent(self.url + '/thumbnail.jpg')
                self._p_changed = True
            elif kwargs != {} and kwargs['scale'] == 'preview' and not hasattr(self, 'image_preview'):
                self.image_preview = self.retrieveContent(self.url + '/small.jpg')
                self._p_changed = True
            # load the images
    
        return self.getField('image').tag(self, **kwargs)

# this is a lot of runtime overhead just to increment a counter. Having it
# in a field causes the JCRImage object to be updated twice each time an
# annotation is added.
#   def _makeClientId(self):
#       """Generate a uneque client id"""
#       count = self.getField("count").get(self)
#       self.getField("count").set(self, count + 1)
#       return "note-%i" % count


    def __bobo_traverse__(self, REQUEST, name):
        """Give transparent access to image scales. This hooks into the
        low-level traversal machinery, checking to see if we are trying to
        traverse to /path/to/object/image_<scalename>, and if so, returns
        the appropriate image content.
        """

        # this is not good - it means that the content could be updated on
        # a simple URL traversal ... this breaks the basic Plone content
        # model which does not support changing content values on the fly
        # !!! all of this should be done at create time.

        # also, you are completely trashing the image field and replacing
        # it with a string

        #if not hasattr(self, 'image') and self.teaser != '':
        #if not hasattr(aq_base(self), 'image') and self.teaser != '':
        #    self.image = self.retrieveContent(self.teaser + '/large.jpg')
        #    jsonStr = self.retrieveContent(self.teaser + '/contents.json')
        #    jsonData = json.loads(jsonStr)
        #    self.filmCode = jsonData["sources"]["tiled"]["href"]
        #    self.description = jsonData["metadata"]["description"]
        #    self._p_changed = True

        if self.url and not self.tileURL:
            jsonStr = self.retrieveContent(self.url + '/contents.json')
            jsonData = json.loads(jsonStr)
            self.tileURL = jsonData["sources"]["tiled"]["href"]
            # self.tileURL = self.filmCode
            self.description = jsonData["metadata"]["description"]
            self._p_changed = True

        if name == 'image':
            if self.url:
                # tiled image - use thumbnail
                return self.retrieveContent(self.url + '/thumbnail.jpg')
            else:
                field = self.getField('image')
                return field.getScale(self)

        # if name == 'image_thumb' and self.teaser:
        #     image = self.retrieveContent(self.teaser + '/thumbnail.jpg')
        #     self.image = image
        #     self._p_changed = True
        #     return image

        if name.startswith('image'):
            field = self.getField('image')
            image = None
            # add this to handle views that want image_preview scale
            # if name == 'image_preview':
            #     image = field.getScale(self)
            # else:
            scalename = name[len('image_'):]
            if scalename in field.getAvailableSizes(self):
                image = field.getScale(self, scale=scalename)
            if image is not None and not isinstance(image, basestring):
                # image might be None or '' for empty images
                return image

        return super(JCRImage, self).__bobo_traverse__(REQUEST, name)

        

atapi.registerType(JCRImage, PROJECTNAME)

# This simple adapter uses Archetypes' ImageField to extract an HTML tag
# for the banner image. This is used in the XXXXXXXX pxxxxxxx to avoid
# having a hard dependency on the AT ImageField implementation.

# Note that we adapt a class, not an interface. This means that we will only
# match adapter lookups for this class (or a subclass), which is correct in
# this case, because we are relying on internal implementation details.

class BannerProvider(object):
    implements(IBannerProvider)
    adapts(JCRImage)
    
    def __init__(self, context):
        self.context = context
    
    @property
    def tag(self):
        return self.context.getField('image').tag(self.context, scale='thumb')
        
# This is a subscription adapter which is used to validate the film object.
# It will be called after the normal schema validation.

# class ValidateFilmCodeUniqueness(object):
#     """Validate site-wide uniquness of film codes.
#     """
#     implements(IObjectPostValidation)
#     adapts(IJCRImage)
#     
#     field_name = 'filmCode'
#     
#     def __init__(self, context):
#         self.context = context
#     
#     def __call__(self, request):
#         value = request.form.get(self.field_name, request.get(self.field_name, None))
#         if value is not None:
#             catalog = getToolByName(self.context, 'portal_catalog')
#             results = catalog(film_code=value,
#                               object_provides=IJCRImage.__identifier__)
#             if len(results) == 0:
#                 return None
#             elif len(results) == 1 and results[0].UID == self.context.UID():
#                 return None
#             else:
#                 return {self.field_name : _(u"The film code is already in use")}
#         
#         # Returning None means no error
#         return None
        
# A vocabulary factory to return currently valid films. The factory itself 
# is registered as a named utility in configure.zcml. This is referenced
# from cinema.py, in the highlightedFilms reference field.

def CurrentFilmsVocabularyFactory(context):
    """Vocabulary factory for currently published films
    """
    catalog = getToolByName(context, 'portal_catalog')
    items = [(r.Title, r.UID) for r in 
                catalog(object_provides=IJCRImage.__identifier__,
                        review_state="published",
                        sort_on='sortable_title')]
                        
    # This turns a list of title->id pairs into a Zope 3 style vocabulary
    return SimpleVocabulary.fromItems(items)
directlyProvides(CurrentFilmsVocabularyFactory, IVocabularyFactory)

