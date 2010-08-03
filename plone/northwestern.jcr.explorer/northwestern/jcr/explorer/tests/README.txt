Introduction
============

This is a full-blown functional test. The emphasis here is on testing what
the user may input and see, and the system is largely tested as a black box.
We use PloneTestCase to set up this test as well, so we have a full Plone site
to play with. We *can* inspect the state of the portal, e.g. using 
self.portal and self.folder, but it is often frowned upon since you are not
treating the system as a black box. Also, if you, for example, log in or set
roles using calls like self.setRoles(), these are not reflected in the test
browser, which runs as a separate session.

Being a doctest, we can tell a story here.

First, we must perform some setup. We use the testbrowser that is shipped
with Five, as this provides proper Zope 2 integration. Most of the 
documentation, though, is in the underlying zope.testbrower package.

    >>> from Products.Five.testbrowser import Browser
    >>> browser = Browser()
    >>> portal_url = self.portal.absolute_url()

The following is useful when writing and debugging testbrowser tests. It lets
us see all error messages in the error_log.

    >>> self.portal.error_log._ignored_exceptions = ()

With that in place, we can go to the portal front page and log in. We will
do this using the default user from PloneTestCase:

    >>> from Products.PloneTestCase.setup import portal_owner, default_password

    >>> browser.open(portal_url)

We have the login portlet, so let's use that.

    >>> browser.getControl(name='__ac_name').value = portal_owner
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()

Here, we set the value of the fields on the login form and then simulate a
submit click.

We then test that we are still on the portal front page:

    >>> browser.url == portal_url
    True

And we ensure that we get the friendly logged-in message:

    >>> "You are now logged in" in browser.contents
    True


-*- extra stuff goes here -*-
The JCRRepository content type
===============================

In this section we are tesing the JCRRepository content type by performing
basic operations like adding, updadating and deleting JCRRepository content
items.

Adding a new JCRRepository content item
--------------------------------

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

Then we select the type of item we want to add. In this case we select
'JCRRepository' and click the 'Add' button to get to the add form.

    >>> browser.getControl('JCRRepository').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'JCRRepository' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'JCRRepository Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

And we are done! We added a new 'JCRRepository' content item to the portal.

Updating an existing JCRRepository content item
---------------------------------------

Let's click on the 'edit' tab and update the object attribute values.

    >>> browser.getLink('Edit').click()
    >>> browser.getControl(name='title').value = 'New JCRRepository Sample'
    >>> browser.getControl('Save').click()

We check that the changes were applied.

    >>> 'Changes saved' in browser.contents
    True
    >>> 'New JCRRepository Sample' in browser.contents
    True

Removing a/an JCRRepository content item
--------------------------------

If we go to the home page, we can see a tab with the 'New JCRRepository
Sample' title in the global navigation tabs.

    >>> browser.open(portal_url)
    >>> 'New JCRRepository Sample' in browser.contents
    True

Now we are going to delete the 'New JCRRepository Sample' object. First we
go to the contents tab and select the 'New JCRRepository Sample' for
deletion.

    >>> browser.getLink('Contents').click()
    >>> browser.getControl('New JCRRepository Sample').click()

We click on the 'Delete' button.

    >>> browser.getControl('Delete').click()
    >>> 'Item(s) deleted' in browser.contents
    True

So, if we go back to the home page, there is no longer a 'New JCRRepository
Sample' tab.

    >>> browser.open(portal_url)
    >>> 'New JCRRepository Sample' in browser.contents
    False

Adding a new JCRRepository content item as contributor
------------------------------------------------

Not only site managers are allowed to add JCRRepository content items, but
also site contributors.

Let's logout and then login as 'contributor', a portal member that has the
contributor role assigned.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = 'contributor'
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

We select 'JCRRepository' and click the 'Add' button to get to the add form.

    >>> browser.getControl('JCRRepository').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'JCRRepository' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'JCRRepository Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

Done! We added a new JCRRepository content item logged in as contributor.

Finally, let's login back as manager.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = portal_owner
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)


The JCRImage content type
===============================

In this section we are tesing the JCRImage content type by performing
basic operations like adding, updadating and deleting JCRImage content
items.

Adding a new JCRImage content item
--------------------------------

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

Then we select the type of item we want to add. In this case we select
'JCRImage' and click the 'Add' button to get to the add form.

    >>> browser.getControl('JCRImage').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'JCRImage' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'JCRImage Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

And we are done! We added a new 'JCRImage' content item to the portal.

Updating an existing JCRImage content item
---------------------------------------

Let's click on the 'edit' tab and update the object attribute values.

    >>> browser.getLink('Edit').click()
    >>> browser.getControl(name='title').value = 'New JCRImage Sample'
    >>> browser.getControl('Save').click()

We check that the changes were applied.

    >>> 'Changes saved' in browser.contents
    True
    >>> 'New JCRImage Sample' in browser.contents
    True

Removing a/an JCRImage content item
--------------------------------

If we go to the home page, we can see a tab with the 'New JCRImage
Sample' title in the global navigation tabs.

    >>> browser.open(portal_url)
    >>> 'New JCRImage Sample' in browser.contents
    True

Now we are going to delete the 'New JCRImage Sample' object. First we
go to the contents tab and select the 'New JCRImage Sample' for
deletion.

    >>> browser.getLink('Contents').click()
    >>> browser.getControl('New JCRImage Sample').click()

We click on the 'Delete' button.

    >>> browser.getControl('Delete').click()
    >>> 'Item(s) deleted' in browser.contents
    True

So, if we go back to the home page, there is no longer a 'New JCRImage
Sample' tab.

    >>> browser.open(portal_url)
    >>> 'New JCRImage Sample' in browser.contents
    False

Adding a new JCRImage content item as contributor
------------------------------------------------

Not only site managers are allowed to add JCRImage content items, but
also site contributors.

Let's logout and then login as 'contributor', a portal member that has the
contributor role assigned.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = 'contributor'
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

We select 'JCRImage' and click the 'Add' button to get to the add form.

    >>> browser.getControl('JCRImage').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'JCRImage' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'JCRImage Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

Done! We added a new JCRImage content item logged in as contributor.

Finally, let's login back as manager.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = portal_owner
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)


The JCRFolder content type
===============================

In this section we are tesing the JCRFolder content type by performing
basic operations like adding, updadating and deleting JCRFolder content
items.

Adding a new JCRFolder content item
--------------------------------

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

Then we select the type of item we want to add. In this case we select
'JCRFolder' and click the 'Add' button to get to the add form.

    >>> browser.getControl('JCRFolder').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'JCRFolder' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'JCRFolder Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

And we are done! We added a new 'JCRFolder' content item to the portal.

Updating an existing JCRFolder content item
---------------------------------------

Let's click on the 'edit' tab and update the object attribute values.

    >>> browser.getLink('Edit').click()
    >>> browser.getControl(name='title').value = 'New JCRFolder Sample'
    >>> browser.getControl('Save').click()

We check that the changes were applied.

    >>> 'Changes saved' in browser.contents
    True
    >>> 'New JCRFolder Sample' in browser.contents
    True

Removing a/an JCRFolder content item
--------------------------------

If we go to the home page, we can see a tab with the 'New JCRFolder
Sample' title in the global navigation tabs.

    >>> browser.open(portal_url)
    >>> 'New JCRFolder Sample' in browser.contents
    True

Now we are going to delete the 'New JCRFolder Sample' object. First we
go to the contents tab and select the 'New JCRFolder Sample' for
deletion.

    >>> browser.getLink('Contents').click()
    >>> browser.getControl('New JCRFolder Sample').click()

We click on the 'Delete' button.

    >>> browser.getControl('Delete').click()
    >>> 'Item(s) deleted' in browser.contents
    True

So, if we go back to the home page, there is no longer a 'New JCRFolder
Sample' tab.

    >>> browser.open(portal_url)
    >>> 'New JCRFolder Sample' in browser.contents
    False

Adding a new JCRFolder content item as contributor
------------------------------------------------

Not only site managers are allowed to add JCRFolder content items, but
also site contributors.

Let's logout and then login as 'contributor', a portal member that has the
contributor role assigned.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = 'contributor'
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

We select 'JCRFolder' and click the 'Add' button to get to the add form.

    >>> browser.getControl('JCRFolder').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'JCRFolder' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'JCRFolder Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

Done! We added a new JCRFolder content item logged in as contributor.

Finally, let's login back as manager.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = portal_owner
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)


The CinemaFolder content type
===============================

In this section we are tesing the CinemaFolder content type by performing
basic operations like adding, updadating and deleting CinemaFolder content
items.

Adding a new CinemaFolder content item
--------------------------------

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

Then we select the type of item we want to add. In this case we select
'CinemaFolder' and click the 'Add' button to get to the add form.

    >>> browser.getControl('CinemaFolder').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'CinemaFolder' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'CinemaFolder Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

And we are done! We added a new 'CinemaFolder' content item to the portal.

Updating an existing CinemaFolder content item
---------------------------------------

Let's click on the 'edit' tab and update the object attribute values.

    >>> browser.getLink('Edit').click()
    >>> browser.getControl(name='title').value = 'New CinemaFolder Sample'
    >>> browser.getControl('Save').click()

We check that the changes were applied.

    >>> 'Changes saved' in browser.contents
    True
    >>> 'New CinemaFolder Sample' in browser.contents
    True

Removing a/an CinemaFolder content item
--------------------------------

If we go to the home page, we can see a tab with the 'New CinemaFolder
Sample' title in the global navigation tabs.

    >>> browser.open(portal_url)
    >>> 'New CinemaFolder Sample' in browser.contents
    True

Now we are going to delete the 'New CinemaFolder Sample' object. First we
go to the contents tab and select the 'New CinemaFolder Sample' for
deletion.

    >>> browser.getLink('Contents').click()
    >>> browser.getControl('New CinemaFolder Sample').click()

We click on the 'Delete' button.

    >>> browser.getControl('Delete').click()
    >>> 'Item(s) deleted' in browser.contents
    True

So, if we go back to the home page, there is no longer a 'New CinemaFolder
Sample' tab.

    >>> browser.open(portal_url)
    >>> 'New CinemaFolder Sample' in browser.contents
    False

Adding a new CinemaFolder content item as contributor
------------------------------------------------

Not only site managers are allowed to add CinemaFolder content items, but
also site contributors.

Let's logout and then login as 'contributor', a portal member that has the
contributor role assigned.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = 'contributor'
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

We select 'CinemaFolder' and click the 'Add' button to get to the add form.

    >>> browser.getControl('CinemaFolder').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'CinemaFolder' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'CinemaFolder Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

Done! We added a new CinemaFolder content item logged in as contributor.

Finally, let's login back as manager.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = portal_owner
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)


The cinemafolder content type
===============================

In this section we are tesing the cinemafolder content type by performing
basic operations like adding, updadating and deleting cinemafolder content
items.

Adding a new cinemafolder content item
--------------------------------

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

Then we select the type of item we want to add. In this case we select
'cinemafolder' and click the 'Add' button to get to the add form.

    >>> browser.getControl('cinemafolder').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'cinemafolder' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'cinemafolder Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

And we are done! We added a new 'cinemafolder' content item to the portal.

Updating an existing cinemafolder content item
---------------------------------------

Let's click on the 'edit' tab and update the object attribute values.

    >>> browser.getLink('Edit').click()
    >>> browser.getControl(name='title').value = 'New cinemafolder Sample'
    >>> browser.getControl('Save').click()

We check that the changes were applied.

    >>> 'Changes saved' in browser.contents
    True
    >>> 'New cinemafolder Sample' in browser.contents
    True

Removing a/an cinemafolder content item
--------------------------------

If we go to the home page, we can see a tab with the 'New cinemafolder
Sample' title in the global navigation tabs.

    >>> browser.open(portal_url)
    >>> 'New cinemafolder Sample' in browser.contents
    True

Now we are going to delete the 'New cinemafolder Sample' object. First we
go to the contents tab and select the 'New cinemafolder Sample' for
deletion.

    >>> browser.getLink('Contents').click()
    >>> browser.getControl('New cinemafolder Sample').click()

We click on the 'Delete' button.

    >>> browser.getControl('Delete').click()
    >>> 'Item(s) deleted' in browser.contents
    True

So, if we go back to the home page, there is no longer a 'New cinemafolder
Sample' tab.

    >>> browser.open(portal_url)
    >>> 'New cinemafolder Sample' in browser.contents
    False

Adding a new cinemafolder content item as contributor
------------------------------------------------

Not only site managers are allowed to add cinemafolder content items, but
also site contributors.

Let's logout and then login as 'contributor', a portal member that has the
contributor role assigned.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = 'contributor'
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)

We use the 'Add new' menu to add a new content item.

    >>> browser.getLink('Add new').click()

We select 'cinemafolder' and click the 'Add' button to get to the add form.

    >>> browser.getControl('cinemafolder').click()
    >>> browser.getControl(name='form.button.Add').click()
    >>> 'cinemafolder' in browser.contents
    True

Now we fill the form and submit it.

    >>> browser.getControl(name='title').value = 'cinemafolder Sample'
    >>> browser.getControl('Save').click()
    >>> 'Changes saved' in browser.contents
    True

Done! We added a new cinemafolder content item logged in as contributor.

Finally, let's login back as manager.

    >>> browser.getLink('Log out').click()
    >>> browser.open(portal_url)
    >>> browser.getControl(name='__ac_name').value = portal_owner
    >>> browser.getControl(name='__ac_password').value = default_password
    >>> browser.getControl(name='submit').click()
    >>> browser.open(portal_url)



