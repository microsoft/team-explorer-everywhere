# Getting started with TEE development
The instructions below will help you get your development environment configured so that you can build, test and debug Team Explorer Everywhere.

## Clone and configure the repository
Use the Git tool of your choice to clone the repository into a local path.
For example, you could use git.exe from a Windows console window:
```
mkdir c:\repos
pushd c:\repos
git clone https://github.com/Microsoft/team-explorer-everywhere.git
```

We keep LF line-ending on the server. Please set the `core.safecrlf` git config property to `true`.
```
git config --local core.safecrlf true
```

## Install development tools

### Install Java

Two versions of the Java Development Kit are needed: we use Java SE 6 as the minimal supported Java execution environment and to build the release kit, as well as Java SE 8 for running Eclipse.

1. Download and install Java SE 6:
  * [Windows and Linux: Java Development Kit 6u45](http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase6-419409.html#jdk-6u45-oth-JPR)
  * [Mac: Apple Java 1.6.0_65, a.k.a. Java for OS X 2015-001](https://support.apple.com/kb/DL1572?locale=en_US)
1. Set the JAVA_HOME environment variable to point to the install, e.g
 * (Windows) `SET JAVA_HOME=C:\Program Files\Java\jdk1.6.0_45`
 * (Linux) `JAVA_HOME=~/dev/java/jdk1.6.0_45`
 * (Mac) `JAVA_HOME=/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home`
1. Add JAVA_HOME bin directory to the path
 * (Windows) `SET PATH=%JAVA_HOME%\bin;%PATH%`
 * (Linux) `PATH=$JAVA_HOME/bin:$PATH`
 * (Mac) `PATH=$JAVA_HOME/bin:$PATH`
1. Download and install Java SE 8 JDK from [Java SE Downloads](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

### Install Ant

1. If you do not already have it, download and install Apache Ant(TM) version 1.9.6 from [Ant Binary Distributions](http://ant.apache.org/bindownload.cgi).
1. Add the full path of the Ant `bin` directory to the `PATH` system environment variable. You can find more Ant installation details [here](http://ant.apache.org/manual/install.html#installing).
 * (Windows) `SET PATH=C:\dev\apache-ant-1.9.6\bin;%PATH%`
 * (Linux) `PATH=~/dev/apache-ant-1.9.6/bin:$PATH`
 * (Mac) `PATH=~/dev/apache-ant-1.9.6/bin:$PATH`

### Install Eclipse

Two versions of Eclipse are needed: we use Eclipse 3.5.2 (Galileo) as the Target Environment, which is the minimum supported Eclipse version (used for building the release and testing backward-compatibility) and Eclipse 4.5.2 (Mars.2) as a dedicated team-explorer-everywhere Development Environment.  Using a dedicated Eclipse installation and workspace for development makes it less disruptive to configure Eclipse-wide preferences.

1. Download and extract [Eclipse Classic 3.5.2](http://www.eclipse.org/downloads/packages/eclipse-classic-352/galileosr2) to the `dev/eclipseTargets/352` sub-folder under your `HOME` folder.  The absolute path to this folder will be needed later when building with Ant.
  * (Windows) Use [7-Zip](http://www.7-zip.org/) to [make sure all files are extracted](https://bugs.eclipse.org/bugs/show_bug.cgi?id=166597).
1. Launch Eclipse 3.5.2
  1. `Help` -> `Install New Software...`
  1. In the *Work with* textbox, type `http://archive.eclipse.org/egit/updates-2.1` and click `Add...`
  1. The *Add Site* dialog appears.  Type `EGit 2.1.0` in the _Name_ textbox and click `OK`.
  1. Expand both features and select the following plug-ins:
    * *Eclipse EGit*
    * *Eclipse EGit - Source*
    * *Eclipse JGit*
    * *Eclipse JGit - Source*
  1. Click `Next >` and finish the wizard to install the plugins
  1. TODO: install more modern SWT than 4.1 for browser-based authentication
1. Download and extract [Eclipse for RCP and RAP Developers 4.5.2](http://www.eclipse.org/downloads/packages/eclipse-rcp-and-rap-developers/mars2)
  * (Windows) Use [7-Zip](http://www.7-zip.org/) to [make sure all files are extracted](https://bugs.eclipse.org/bugs/show_bug.cgi?id=166597).
1. Launch Eclipse 4.5.2 and follow the instructions below to configure it

### Configure Eclipse
In a new empty Eclipse workspace open the Preferences dialog using `Window` -> `Preferences` menu option.

1. Set JRE preferences
  * Go to the `Java` -> `Installed JREs` page.
    - Click the `Add` button.
    - Select `Standard VM` and click the `Next` button.
    - Enter the absolute path to the JDK 6 into the JRE home field. Note that you should use the JDK folder, not the JRE one.
    - Click the `Finish` button.
    - Select the checkbox next to the newly-added JDK and click the `Apply` button.
  * Go to the `Java` -> `Installed JREs` -> `Execution Environment` page.
    - Select `JavaSE-1.6` in the `Execution Environments` column.
    - Select the checkbox next to the best match in the Compatible JREs column.

1. Set Compiler preferences
  * Go to the `Java` -> `Compiler` page.
    - Select `1.6` in the `Compiler compliance level` list box.
    - Select the `Use default compliance setting` checkbox.
    - Click the `Apply` button.
  * Go to the `Java` -> `Compiler` -> `Error/Warnings` page.
    - Select `Error` in the `Non-externalized strings (missing/unused $NON-NLS$ tag)` list box.
    - Click the `Apply` button.

1. Set Editor preferences
  * Go to the `Java` -> `Editor` -> `Save Actions` page.
    - Make sure that all the following is selected:
      - `Perform the selected actions on save`
        - `Format source code`
          - `Format all lines`
        - `Organize imports`
        - `Additional actions`
          - `Add missing '@Override' annotations`
          - `Add missing '@Override' annotations to implementations of interface methods`
          - `Add missing '@Deprecated' annotations`
          - `Remove unnecessary casts`
    - Click the `Apply` button.
  * Go to the `Java` -> `Editor` -> `Templates` page.
    - Click the `Import...` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the **team-explorer-everywhere** repository.
    - Select the `Java ALM Java Editor Templates.xml` file and click `Open`.
    - Click the `Apply` button.

1. Set Code Style preferences
  * Go to the `Java` -> `Code Style` -> `Clean Up` page.
    - Click the `Import...` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the **team-explorer-everywhere** repository.
    - Select the `Java ALM Java Code Cleanup.xml` file and click `Open`.
    - Make sure that `Java ALM` is selected in the `Active profile` list box.
    - Click the `Apply` button.
  * Go to the `Java` -> `Code Style` -> `Code Templates` page.
    - Click the `Import...` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the **team-explorer-everywhere** repository.
    - Select the `Java ALM Java Code Templates.xml` file and click `Open`.
    - Click the `Apply` button.
  * Go to the `Java` -> `Code Style` -> `Formatter` page.
    - Click the `Import...` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the **team-explorer-everywhere** repository.
    - Select the `Java ALM Java Code Formatting.xml` file and click `Open`.
    - Make sure that `Java ALM` is selected in the `Active profile` list box.
    - Click the `Apply` button.
  * Go to the `Java` -> `Code Style` -> `Organize Imports` page.
    - Click the `Import...` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the **team-explorer-everywhere** repository.
    - Select the `Java ALM Java Code.importorder` file and click `Open`.
    - Click the `Apply` button.

### Import the projects into Eclipse

1. Open Git Import Wizard using `File` -> `Import` -> `Git` ->`Projects from Git`.
1. Select `Existing local repository`, click the `Add` button.
  1. Using the `Browse...` button, navigate to your local copy of the **team-explorer-everywhere** repository, select it, and click the `Finish` button.
1. Click the `Next` button to switch to the Wizard Selection page.
1. Select the `Import existing Eclipse projects` option and the `source` folder under the root of the repository.
1. Click the `Next` button.
1. Make sure that all the `com.microsoft.tfs.*` projects are selected and click the `Finish` button.

### Install Eclipse dependencies

There are additional Eclipse-related dependencies that must be installed _for each Eclipse platform target version_.  For now, let's install the dependencies of earliest-supported Eclipse version (3.5.2).  If you ever need to target another version of Eclipse, repeat this process with the matching version.

1. Open the `com.microsoft.tfs.client.eclipse.target` project.
1. Select the `3.5.target` file
1. Open the file with the default editor (Target Editor).
1. Wait until Eclipse downloads indexes of the referenced p2 repositories. (this might take a few minutes)
1. Click the `Set as Target Platform` link at the top right of the Target Definition view.
1. Open `Window` -> `Preferences` -> `Plug-In Development` -> `Target Platform` and
make sure the desired target is selected.
1. Perform a clean rebuild of all projects. (`Project` -> `Clean...` -> `Clean all projects` -> `OK`)

### Create Runtime Configurations in Eclipse
1. Create a "Plugin" configuration to run/debug the plugin code.
  * Open the Debug Configurations window using `Run` -> `Debug Configurations...`
  * Create a new Eclipse Application configuration:
    * Right-click the `Eclipse Application` node in the configurations list.
    * Select the `New` pop menu option.
  * Enter `TEE Eclipse plugin` as the new configuration `Name`
  * On the **Main** tab:
    * Select the `Run a product` option and enter `org.eclipse.platform.ide` in the textbox.
    * Select the `Execution environment` option and make sure `JavaSE-1.6` is selected.
  * Click `Debug` to test the configuration
1. Create a "CLC" configuration to run/debug the command-line client code.
  * Open the Debug Configurations window using `Run` -> `Debug Configurations...`
  * Create a new Java Application configuration:
    * Right-click the `Java Application` node in the configurations list.
    * Select the `New` pop menu option.
  * Enter `TEE CLC` as the new configuration `Name`
  * On the **Main** tab:
    * Set the `Project` to `com.microsoft.tfs.client.clc`
    * Set the `Main class` to `com.microsoft.tfs.client.clc.vc.Main`
  * On the **Arguments** tab:
    * In the `VM arguments` section add the following so the CLC can locate its native libraries:
      ```
      -Dcom.microsoft.tfs.jni.native.base-directory=${workspace_loc:com.microsoft.tfs.jni}/os
      ```
    * In the `Program arguments` section enter the `tf` command arguments for the run, e.g. `workspaces /help`.
  * Click `Debug` to test the configuration
