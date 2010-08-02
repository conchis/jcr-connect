
from Products.CMFCore.utils import getToolByName

# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#
def setupExplorer(context):
    
    portal = context.getSite()

    # register JCRImage for for special treatment in navigation
    props = getToolByName(portal, 'portal_properties').site_properties
    
    typesUseViewActionInListings = props.getProperty('typesUseViewActionInListings') 
    if typesUseViewActionInListings:
        special_types = list(typesUseViewActionInListings)
    else:
        special_types = []
    if 'JCRImage' not in special_types:
        special_types.append('JCRImage')
        props._updateProperty('typesUseViewActionInListings',
                              tuple(special_types))
