# Team Explorer Everywhere Cross-platform Command-line Client for Team Foundation Server
This project contains:
- Cross-platform Command-line Client for Team Foundation Server and Team Services
- Team Foundation Server/Team Services SDK for Java

The purpose of this client is mainly to support [Azure DevOps Plugin for IntelliJ][azure-devops-intellij].

## What is the Command-line Client for TFS?
The CLC for TFS allows you run version control commands from a console/terminal window against a TFS server on any operating system. 
This tool is for use with Team Foundation Version Control (TFVC), a centralized version control system.
If you prefer to use Git, you can use any Git client with TFS or Team Services as well.

## Where Can I Get The Command-line Client?
Download the TEE-CLC-*.zip file in the [Releases](https://github.com/JetBrains/team-explorer-everywhere/releases) area of this repo.

## Building with Ant
### Install Java 8
1. We use Java 8 as the minimal supported Java execution environment.
2. Download and install the JDK for [Java 8][adoptopenjdk]
3. Set the JAVA_HOME environment variable to point to the install, e.g.
   * (Windows) `SET JAVA_HOME=C:\dev\java\jdk8u192-b12`
   * (Linux) `JAVA_HOME=~/dev/java/jdk8u192-b12`
   * (Mac) `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home`
4. Add JAVA_HOME bin directory to the path
   * (Windows) `SET PATH=%JAVA_HOME%\bin;%PATH%`
   * (Linux) `PATH=$JAVA_HOME/bin:$PATH`
   * (Mac) `PATH=$JAVA_HOME/bin:$PATH`
    
### Install Ant 
1. If you do not already have it, download and install Apache Ant(TM) version 1.9.6 from [Ant Binary Distributions](http://ant.apache.org/bindownload.cgi).
1. Add the full path of the Ant `bin` directory to the `PATH` system environment variable. You can find more Ant installation details [here](http://ant.apache.org/manual/install.html#installing).
 * (Windows) `SET PATH=C:\dev\apache-ant-1.9.6\bin;%PATH%`
 * (Linux) `PATH=~/dev/apache-ant-1.9.6/bin:$PATH`
 * (Mac) `PATH=~/dev/apache-ant-1.9.6/bin:$PATH`

### Automated Build

There's a script to download the Eclipse automatically and set up its environment. To do that, execute the following PowerShell scripts:

```console
$ pwsh ./scripts/prepare-eclipse.ps1
$ pwsh ./scripts/build.ps1
```
    
### Install the Eclipse Target Environment
Historically, we use Eclipse 3.5.2 as the base target Eclipse version.
1. Download and install Eclipse Classic from [Eclipse 3.5.2](http://www.eclipse.org/downloads/packages/release/galileo/sr2). On Windows, you may want to use a third party ZIP tool to unzip the Eclipse archive.
1. Install the [EGit 2.1.0](http://archive.eclipse.org/egit/updates-2.1) plug-in into that Eclipse instance.

### Clone the Repository
Use the Git tool of your choice to clone the repository into a local path.
For example, you could use git.exe from a Windows console window:
```
mkdir c:\repos
pushd c:\repos
git clone https://github.com/Microsoft/team-explorer-everywhere
```

### Build
Note: The Eclipse target installation location is needed as a parameter for the Ant build variable `dir.machine.build-runtime`. For the samples below, we will assume that the target Eclipse version was installed into '\dev\eclipseTargets\352'.
1. From a terminal/console window, change to the `build` subfolder of the root folder of the team-explorer-everywhere repository
1. Run ant -Ddir.machine.build-runtime=`<pathToEclipseTarget>`, for example, 
```
(Windows) ant -Ddir.machine.build-runtime=c:\Users\<userId>\dev\eclipseTargets\352\
(Linux) ant -Ddir.machine.build-runtime=/home/<userId>/dev/eclipseTargets/352/
(Mac) ant -Ddir.machine.build-runtime=/Applications/eclipse-classic/
``` 
1. Build results can be found in `build\output`

## Contributing
We welcome pull requests. Please fork this repo and send us your contributions.
See [Contributing](./Contributing.md) for details.

## Localization / Translation
Your language, your words, your plug-in for you!

Along with open-sourced Team Explorer Everywhere (TEE) source code, we are making it possible for anyone to contribute translations in your native language. With these changes, you can now improve existing translated resources, translate updated resources, or even provide new language support TEE did not have before. Your contribution will be part of the TEE Plug-in in your language for everyone to use. We highly appreciate your efforts, and we welcome your feedback and suggestions on the TEE community localization process. Your contribution could be in next release!

Please click [Localization](./Localization.md) for details on how to contribute in TEE community translation effort. Feel free to contact [us](mailto:kevinli@microsoft.com) if you have any questions.

**Happy contributing!**

[adoptopenjdk]: https://adoptopenjdk.net/
[azure-devops-intellij]: https://github.com/microsoft/azure-devops-intellij
