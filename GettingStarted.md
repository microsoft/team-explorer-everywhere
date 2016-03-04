# Getting started with TEE development
The instructions below will help you get your development environment configured so that you can build, test and debug Team Explorer Everywhere.

## Build and Run with Eclipse
### Tools
Install Eclipse Mars.2 Release (4.5.2) for RCP and RAP Developers or later.

### Java Requirements
We use JavaSE-1.6 as the minimal supported Java execution environment.
Depending on the Eclipse version you use you might have to install a later JDK version.
For Eclipse Mars install JDK 8 or later. You can find the JDK downloads on Oracle's web site at
[Java SE Downloads](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
Find older versions in [Previous Releases](http://www.oracle.com/technetwork/java/javase/archive-139210.html).

### Eclipse IDE Configuration
In a new empty Eclipse workspace open the Preferences dialog using Window -> Preferences menu option.

1. Set JRE preferences
  * Go to the `Java` -> `Installed JREs` page.
    - Click the `Add` button.
    - Select `Standard VM` and click the `Next` button.
    - Enter a full JDK path into the JRE home field. Note that you should use the JDK folder, not a JRE one.
    - Click the `Finish` button.
    - Select the check-box at the added JDK and click the `Apply` button.
  * Go to the `Java` -> `Installed JREs` -> `Execution Environment` page.
    - Select `JavaSE-1.6` in the `Execution Environments` column.
    - Select the check-box at the desired compatible JDK in the Compatible JREs column.

1. Set Compiler preferences
  * Go to the `Java` -> `Compiler` page.
    - Select `1.6` in the `Compiler complience level` list-box.
    - Select the `Use default complience setting` check-box.
    - Click the `Apply` button.
  * Go to the `Java` -> `Compiler` -> `Error/Warnings` page.
    - Select `Error` in the `Non-externalized strings (missing/unused $NON-NLS$ tag)` list-box.
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
    - Click the `Import` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the team-explorer-everywhere repository.
    - Select the `Java ALM Java Editor Templates.xml` file and click `Open`.
    - Click the `Apply` button.

1. Set Code Style preferences
  * Go to the `Java` -> `Code Style` -> `Clean Up` page.
    - Click the `Import` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the team-explorer-everywhere repository.
    - Select the `Java ALM Java Code Cleanup.xml` file and click `Open`.
    - Make sure that `Java ALM` is selected in the `Active profile` list-box.
    - Click the `Apply` button.
  * Go to the `Java` -> `Code Style` -> `Code Templates` page.
    - Click the `Import` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the team-explorer-everywhere repository.
    - Select the `Java ALM Java Code Templates.xml` file and click `Open`.
    - Click the `Apply` button.
  * Go to the `Java` -> `Code Style` -> `Formatter` page.
    - Click the `Import` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the team-explorer-everywhere repository.
    - Select the `Java ALM Java Code Formatting.xml` file and click `Open`.
    - Make sure that `Java ALM` is selected in the `Active profile` list-box.
    - Click the `Apply` button.
  * Go to the `Java` -> `Code Style` -> `Organize Import` page.
    - Click the `Import` button.
    - Navigate to the `dev-config\codeStyle` subfolder of the team-explorer-everywhere repository.
    - Select the `Java ALM Java Code.importorder` file and click `Open`.
    - Click the `Apply` button.

### Importing Projects

1. Open Git Import Wizard using `File` -> `Import` -> `Git` ->`Projects from Git`.
1. Select `Existing local repository`, click the `Add` button.
1. Using the `Browse` button, navigate to  and your local copy of the team-explorer-everywhere repository, select it, and click the `Finish` button.
1. Click the `Next` button to switch to the Wizard Selection page.
1. Select the `Import existing Eclipse projects' option and the `source` folder under the root of the repository.
1. Click the `Next` button.
1. Make sure that all `com.microsoft.tfs.*` project are selected and click the `Finish` button.

#### Dependencies

To install dependencies
* Open the `com.mictosoft.tfs.client.eclipse.target' project.
* Select the `<version>.target` file that matches to your Eclipse platform target version.
* Open the file with the default editor (Target Editor).
Whait until Eclipse downloads indexes of the referenced p2 repositories.
* Click the `Set as Target Platform` link at the top right of the Target Definition view.
* Open `Window` -> `Preferences` -> Plug-In Development` -> `Target Platform` and
make sure the desired target is selected.
* Clean rebuild projects. (`Project` -> `Clean` -> `Clean all projects`)


1. We keep LF line-ending on the server. Please set the `core.safecrlf` git config property to true.
```
git config core.safecrlf true
```

## Debugging
### Creating Runtime Configurations
1. Create a "Plugin" configuration to run/debug the plugin code.
  * Open the Debug Configurations window using `Run` -> `Debug Configurations`
  * Create a new Eclipse Application configuration:
    * Right click the `Eclipse Application` node in the configurations list.
    * Select the `New` pop menu option.
  * Enter a unique name for the new configuration 
  * On the **Main** tab:
    * Select the `Run a product` option and `org.eclipse.platform.ide`.
    * Select the `Execution environment` option and the target Java version.
1. Create a "CLC" configuration to run/debug the command-line client code.
  * Open the Debug Configurations window using `Run` -> `Debug Configurations`
  * Create a new Java Application configuration:
    * Right click the `Java Application` node in the configurations list.
    * Select the `New` pop menu option.
  * Enter a unique name for the new configuration 
  * On the **Main** tab:
    * Set the `Project` to `com.microsoft.tfs.client.clc`.
    * Set the `Main class` to `com.microsoft.tfs.client.clc.vc.Main`.
  * On the **Arguments** tab:
    * In the `VM arguments` section add information for the TEE to locate its native libraries 
      `-Dcom.microsoft.tfs.jni.native.base-directory=${workspace_loc:com.microsoft.tfs.jni}/os`
    * In the `Program arguments` section enter the `tf` command arguments for the run, e.g. `workspaces /help`.
