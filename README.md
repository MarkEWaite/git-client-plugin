Avoid shell expansion in credentials file names
===============================================

Work attempting to reduce shell wildcard expansion risks in credential values.

Utility plugin for Git-related support
======================================

Extracted from [git-plugin](https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin)
to make it easier for other plugins to use and contribute new features.
Includes JGit as a library so that other Jenkins components can rely on
JGit being available whenever the git client plugin is available.

* see [Jenkins wiki](https://wiki.jenkins-ci.org/display/JENKINS/Git+Client+Plugin) for detailed feature descriptions
* use [JIRA](https://issues.jenkins-ci.org) to report issues / feature requests

Contributing to the Plugin
==========================

Refer to [contributing to the plugin](https://github.com/jenkinsci/git-client-plugin/blob/master/CONTRIBUTING.md)
for suggestions to speed the acceptance of your contributions.

To Do
=====

* Evaluate and comment on [pull requests](https://github.com/jenkinsci/git-client-plugin/pulls)
* Fix [bugs](https://issues.jenkins-ci.org/secure/IssueNavigator.jspa?mode=hide&reset=true&jqlQuery=project+%3D+JENKINS+AND+status+in+%28Open%2C+"In+Progress"%2C+Reopened%29+AND+component+%3D+git-client-plugin)
* Create infrastructure to detect [files opened during a unit test](https://issues.jenkins-ci.org/browse/JENKINS-19994) and left open at exit from test
* Complete more of the JGit implementation
* Add more authentication tests
