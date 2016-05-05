# Team Explorer Everywhere Plug-in for Eclipse and Cross-platform Command-line Client for Team Foundation Server
This project contains:
- Team Explorer Everywhere Plug-in for Eclipse
- Cross-platform Command-line Client for Team Foundation Server and Team Services
- Team Foundation Server/Team Services SDK for Java

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

## Building with Ant
### Clone the Repository
Use the Git tool of your choice to clone the repository into a local path.
For example, you could use git.exe from a Windows console window:
```
mkdir c:\repos
pushd c:\repos
git clone https://github.com/Microsoft/team-explorer-everywhere
```

### Install Java 6
1. We use JavaSE-1.6 as the minimal supported Java execution environment.
1. Download and install the JDK for [JavaSE-1.6](http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase6-419409.html).
1. Set the **JAVA_HOME** environment variable to point to the install, e.g.
 * (Windows) `SET JAVA_HOME=C:\dev\java\jdk1.6.0_45`
 * (Linux) `JAVA_HOME=~/dev/java/jdk1.6.0_45`
 * (Mac) `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home`
1. Add **JAVA_HOME bin** directory to the **Path**
 * (Windows) `SET PATH=%JAVA_HOME%\bin;%PATH%`
 * (Linux) `PATH=$JAVA_HOME/bin:$PATH`
 * (Mac) `PATH=$JAVA_HOME/bin:$PATH`

### Install Ant 
1. If you do not already have it, download and install Apache Ant(TM) version 1.9.6 from [Ant Binary Distributions](http://ant.apache.org/bindownload.cgi).
1. Add the full path of the Ant `bin` directory to the `PATH` system environment variable. You can find more Ant installation details [here](http://ant.apache.org/manual/install.html#installing).
 * (Windows) `SET PATH=C:\dev\apache-ant-1.9.6\bin;%PATH%`
 * (Linux) `PATH=~/dev/apache-ant-1.9.6/bin:$PATH`
 * (Mac) `PATH=~/dev/apache-ant-1.9.6/bin:$PATH`

### Install the Eclipse Target Environment
We use Eclipse 3.5.2 as the minimum supported Eclipse version.
 1. Download and install Eclipse Classic from [Eclipse 3.5.2](http://www.eclipse.org/downloads/packages/release/galileo/sr2). On Windows, you may want to use a third party ZIP tool to unzip the Eclipse archive.
 1. Install the [EGit 2.1.0](http://archive.eclipse.org/egit/updates-2.1) plug-in into that Eclipse instance.
   * In Eclipse, choose to "Install New Software" and enter `http://archive.eclipse.org/egit/updates-2.1/` in the "Work with:" box.  Click `Add`.
   * Select all packages, click `Next` (several times) and `Finish` to complete the installation.

### Build
**Note**: The Eclipse target installation location is needed as a parameter for the Ant build variable `dir.machine.build-runtime`. For the samples below, we will assume that the target Eclipse version was installed into '\dev\eclipseTargets\352'.
 1. From a terminal/console window, change to the `build` subfolder of the root folder of the team-explorer-everywhere repository
 1. Run `ant -Ddir.machine.build-runtime=<pathToEclipseTarget>`
   * For example,
```
(Windows) ant -Ddir.machine.build-runtime=c:\Users\<userId>\dev\eclipseTargets\352\
(Linux) ant -Ddir.machine.build-runtime=/home/<userId>/dev/eclipseTargets/352/
(Mac) ant -Ddir.machine.build-runtime=/Applications/eclipse-classic/
``` 
1. Build results can be found in `build\output`

## Contributing
We welcome pull requests. Please fork this repo and send us your contributions.

See [Contributing](https://github.com/Microsoft/team-explorer-everywhere/blob/master/Contributing.md) for details.
