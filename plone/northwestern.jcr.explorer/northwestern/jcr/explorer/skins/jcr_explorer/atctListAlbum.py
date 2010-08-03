## Script (Python) "atctListAlbum"
##title=Helper method for photo album view
##bind container=container
##bind context=context
##bind namespace=
##bind script=script
##bind subpath=traverse_subpath
##parameters=images=0, folders=0, subimages=0, others=0

from Products.CMFPlone.utils import base_hasattr

result = {}

if context.portal_type == 'Topic':
    queryMethod = context.queryCatalog
else:
    queryMethod = context.getFolderContents

if images:
    result['images'] = queryMethod({'portal_type':('Image','JCRImage')},full_objects=True)
if folders:
    # We don't need the full objects for the folders
    result['folders'] = queryMethod({'portal_type':('Folder','JCRFolder')})
if subimages:
    #Handle brains or objects
    if base_hasattr(context, 'getPath'):
        path = context.getPath()
    else:
        path = '/'.join(context.getPhysicalPath())
    # Explicitly set path to remove default depth
    result['subimages'] = queryMethod({'Type':('Image',), 'path':path})
if others:
    searchContentTypes = context.plone_utils.getUserFriendlyTypes()
    filtered = [p_type for p_type in searchContentTypes
                if p_type not in ('Image', 'Folder', 'JCRImage') ]
    if filtered:
        # We don't need the full objects for the folder_listing
        result['others'] = queryMethod({'portal_type':filtered})
    else:
        result['others'] = ()

return result
