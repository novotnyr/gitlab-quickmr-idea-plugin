About this plugin
=================

This is an IntelliJ IDEA plug-in that will help you to quickly
create Merge Requests for GitLab projects.

* Quickly create merge requests with default settings and submit them with a single click
* Specify your favourite merge request assignees
* Supports GitLab API v4

Using the plug-in
-----------------

First, configure the project settings in
**Preferences | Version Control | GitLab Quick Merge Request**.

Then, specify GitLab URL and REST Access Token and enter
default target branch for Merge Request and a title.

Then, feel free to add as many assignees as necessary. The first
assignee is a default one that can be used to super-quick
Merge Request.

Submitting Merge Requests
-------------------------
Right-Click on the project, then use the *Git* submenu.

* *Quick Merge Request* will create a new Merge Request
from the current module with the default settings. It will be assigned
to the favourite assignee
* *Quick Merge Assigned To* will submit a new Merge Request,
but you may choose a default assignee.


Compatibility
-------------

This plugin is compatible with all JetBrains product that enable
Git VC integration.