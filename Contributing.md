# TEE Contributor Guide
This instructions below will help you get your development environment setup so that you can contribute to this repository.

## Ways to Contribute
Interested in contributing to the team-explorer-everywhere project? There are plenty of ways you can contribute, all of which help make the project better.
* Submit a [bug report](https://github.com/Microsoft/team-explorer-everywhere/issues/new) through the Issue Tracker 
* Review the [source code changes](https://github.com/Microsoft/team-explorer-everywhere/pulls).
* Submit a code fix for a bug 
* Submit a [feature request](https://visualstudio.uservoice.com/forums/330519-team-services/category/145260-java-tools-intellij-eclipse) 
* Participate in [discussions](https://social.msdn.microsoft.com/Forums/vstudio/en-US/home?forum=tee&filter=alltypes&sort=lastpostdesc)

## Code Styles
A few styles we follow:
1. No tabs in source code. All tabs should be expanded to 4 spaces.
1. No imports with "*".
1. The attribute `final` should be used whereever it's possible. 
1. All Java source files must have the following two lines at the top:
```
 // Copyright (c) Microsoft. All rights reserved.
 // Licensed under the MIT license. See License.txt in the project root.
```
1. We keep LF line-ending on the server. Please set the `core.safecrlf` git config property to true.
```
git config core.safecrlf true
```

## Building, testing and debugging

Before you can build, please follow the [Getting Started Guide](GettingStarted.md) to install & configure the tools you'll need.

## Testing

1. ...

## Debugging

### Running Under the Debugger
1. To debug the plugin or the command-line client, click `Run` -> `Debug Configurations`.
1. Select the desired runtime configuration and click the Debug button.

## Contribution License Agreement
In order to contribute, you will need to sign a [Contributor License Agreement](https://cla.microsoft.com/).

## Submitting Pull Requests
We welcome pull requests!  Fork this repo and send us your contributions.  Go [here](https://help.github.com/articles/using-pull-requests/) to get familiar with GitHub pull requests.

Before submitting your request, ensure that all tests pass.
