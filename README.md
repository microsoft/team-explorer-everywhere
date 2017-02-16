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

a) The plug-in is freely available from the [Eclipse Marketplace](https://marketplace.eclipse.org/content/team-explorer-everywhere).
Hover over the `Install` button for more information on how to install it into your version of Eclipse.

b) Follow instructions at https://java.visualstudio.com/Docs/tools/eclipse and use update site `http://dl.microsoft.com/eclipse/`.

c) You can also manually download and install the plug-in from the [Releases](https://github.com/Microsoft/team-explorer-everywhere/releases) section of this GitHub repository.

## What is the Command-line Client for TFS?
The CLC for TFS allows you run version control commands from a console/terminal window against a TFS server on any operating system. 
This tool is for use with Team Foundation Version Control (TFVC), a centralized version control system.
If you prefer to use Git, you can use any Git client with TFS or Team Services as well.

## Where Can I Get The Command-line Client?
Download the TEE-CLC-*.zip file in the [Releases](https://github.com/Microsoft/team-explorer-everywhere/releases) area of this repo.

## Building with Ant
### Install Java 6
1. We use JavaSE-1.6 as the minimal supported Java execution environment.
1. Download and install the JDK for [JavaSE-1.6](http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase6-419409.html).
1. Set the JAVA_HOME environment variable to point to the install, e.g.
 * (Windows) `SET JAVA_HOME=C:\dev\java\jdk1.6.0_45`
 * (Linux) `JAVA_HOME=~/dev/java/jdk1.6.0_45`
 * (Mac) `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home`
1. Add JAVA_HOME bin directory to the path
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


## Frequently Asked Questions (FAQ)
**Q: Is there a beginner's guide for TEE?**

**A:** Absolutely.  You can find it on MSDN at <a href="https://msdn.microsoft.com/en-us/library/hh913026(v=vs.120).aspx" target="_blank">Team Foundation Server Plug-in for Eclipse - Beginner's Guide</a>.

**Q: Is there a way to view local repos in TEE 2015 in Eclipse (Mars) or is it assumed one would use the other Git tooling for Eclipse?**

**A:** It is expected that one would use the standard EGit tooling in Eclipse to view local repos, but TEE does have a "Repositories" view in which you can see which repos are available on the server.

**Q: Also, is there an easy way (using TEE) to “import” a local Git repo and push it up to Team Services? Or is the Git command-line the way to do it?**

**A:** There’s documentation on how to do it in TEE at <a href="https://msdn.microsoft.com/en-us/library/hh568708(v=vs.120).aspx" target="_blank">Sharing Eclipse Projects in Team Foundation Server</a>.
That article specifically shows TFVC but when you go to Share the project, you’ll be prompted to choose a repository type (Git or TFVC).

**Q: Where can I get more help?**

**A:** Log an issue or check the <a href="https://social.msdn.microsoft.com/Forums/vstudio/en-US/home?forum=tee" target="_blank">Team Explorer Everywhere forum</a>

**Q: Where can I learn more about the Azure Toolkit for Eclipse?**

**A:** Check the <a href="https://docs.microsoft.com/azure/azure-toolkit-for-eclipse" target="_blank">Azure Toolkit for Eclipse web page</a>

**Q: The TEE Command Line Client has removed the "tf profile" command. How can I connect to TFS without having to repeatedly type my credentials?**

**A:** You can use Kerberos for authentication to a TFS server. More information can be found <a href="https://msdn.microsoft.com/en-us/library/gg475929%28v=vs.120%29.aspx" target="_blank">here.</a> This article mentions the "tf profile" command because it still existed at that time this article was written but that step can be skipped now all together.

**Q: How can I fix the "Authentication not supported" error when using Eclipse to perform Git operations with TFS?**

**A:** Eclipse’s EGit is built on JGit, and unfortunately, recent versions of JGit actively reject NTLM authentication, resulting in “Authentication not supported” when connecting to on-premises installations of TFS that require NTLM.  We’re working to improve this situation in the next version of TEE, but until then, you can do one of the following:
* Use <a href="http://cntlm.sourceforge.net/" target="_blank">Cntlm</a>, a locally-installed proxy that adds NTLM authentication on-the-fly
* Use an older version of Eclipse/EGit/JGit
* <a href="https://github.com/Microsoft/tfs-cli/blob/master/docs/configureBasicAuth.md" target="_blank">Enable</a> basic authentication with SSL in IIS on your TFS server
* Enable Kerberos authentication in IIS on your TFS server (the default for the next version of TFS after TFS 2015):
  1. In IIS manager, click on the TFS site on the left under Connections and open up the "Authentication" section under IIS.  Set “ASP.NET Impersonation” to Enabled and “Windows Authentication” to Enabled.
  2. Under "Windows Authentication" right click and select "Providers."  Add/enable the "Negotiate" and "NTLM" providers.
  3. Under "Windows Authentication" right click and select "Advanced Settings."  Uncheck "Enable Kernel-mode" because it is incompatible with Kerberos.
