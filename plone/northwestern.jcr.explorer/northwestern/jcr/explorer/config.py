"""Common configuration constants
"""

PROJECTNAME = 'northwestern.jcr.explorer'
PROMOTIONS_PORTLET_COLUMN = u"plone.rightcolumn"

ADD_PERMISSIONS = {
    # -*- extra stuff goes here -*-
    'JCRDocument': 'northwestern.jcr.explorer: Add JCRDocument',
    'AnnotatedImage': 'northwestern.jcr.explorer: Add AnnotatedImage',
    'JCRRepository': 'northwestern.jcr.explorer: Add JCRRepository',
    'JCRImage': 'northwestern.jcr.explorer: Add JCRImage',
    'JCRFolder': 'northwestern.jcr.explorer: Add JCRFolder',
    'CinemaFolder': 'northwestern.jcr.explorer: Add CinemaFolder',
}
