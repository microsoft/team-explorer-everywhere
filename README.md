# Team Explorer Everywhere Plug-in for Eclipse and Cross-platform Command-line Client for Team Foundation Server
This project contains the Team Explorer Everywhere Plug-in for Eclipse, 
the cross-platform Command-line Client for Team Foundation Server, and
the Team Foundation Server SDK for Java. 

## What is Team Explorer Everywhere?
Team Explorer Everywhere is the official TFS plug-in for Eclipse from Microsoft. 
It works on the operating system of your choice with your favorite Eclipse-based IDE 
and helps you collaborate across development teams using Team Foundation Server 
or Visual Studio Team Services. 
 
Supported on Linux, Mac OS X, and Windows.
Compatible with IDEs that are based on Eclipse 3.5 to 4.5.

## Where Can I Get This Eclipse Plug-in?
The plug-in is freely available from the [Eclipse Marketplace](https://marketplace.eclipse.org/content/team-explorer-everywhere).
Hover over the `Install` button for more information on how to install it into your version of Eclipse.

## What is the Command-line Client for TFS?
The CLC for TFS allows you run version control commands from a console/terminal window against a TFS server on any operating system. 
This tool is for use with Team Foundation Version Control (TFVC), a centralized version control system.
If you prefer to use Git, you can use any Git client with TFS or Team Services as well.

## Where Can I Get The Command-line Client?
The CLC is a separate download choice when you choose to download TEE [here](https://www.visualstudio.com/downloads/download-visual-studio-vs#d-team-explorer-everywhere). 

## How do I build the source code?

Before you can build, please follow the [Getting Started Guide](GettingStarted.md) to install & configure the tools you'll need.

Note: The Eclipse target installation location is needed as a parameter for the Ant build variable `dir.machine.build-runtime`. For the samples below, we will assume that the target Eclipse version was installed into the `dev/eclipseTargets/352` sub-folder under your `HOME` folder.
1. From a terminal/console window, change to the `build` sub-folder of the root folder of the **team-explorer-everywhere** repository.
1. Run ant -Ddir.machine.build-runtime=`<pathToEclipseTarget>`, for example, 
```
(Windows) ant -Ddir.machine.build-runtime=C:\Users\<userId>\dev\eclipseTargets\352\
(Linux) ant -Ddir.machine.build-runtime=/home/<userId>/dev/eclipseTargets/352/
(Mac) ant -Ddir.machine.build-runtime=/Users/<userId>/dev/eclipseTargets/352/
``` 
1. Build results can be found in the `output` sub-folder.

## How can I contribute?
We welcome pull requests. Please fork this repo and send us your contributions.
See [Contributing.md](Contributing.md) for details.
