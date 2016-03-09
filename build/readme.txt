Version 2.0
Date: 2012-07-05
Note that this file is now out of date after the P2 refactoring.  Now need
to refector the readme :-)


Version: 1.2
Date: 2009-01-15

This file is somewhere between a readme and a manual for the TEE build. 
Some parts of the build are documented more completely than others. I'll try 
to keep it up to date with the latest revisions to the build, but keep in mind 
that when there's a conflict between the build and the documentation, the 
build is always right :).

Also, many of the build files are well commented, especially the bootstrap
build script (bootstrap/build.xml). Since the bootstrap is where the build starts,
reading through that script wouldn't be a bad idea.

1000 foot view
--------------
The TEE build system is what we use to perform automated builds of 
anything and everything related to TEE. The build system is modular, 
multi-phase, and designed to work in a variety of scenarios. More details are below.

Terminology
-----------
1) build phases 
The build is split into multiple phases. Each phase has a set of responsibilities and 
a contract with the other phases. The main motivation for multiple phases is to allow 
a single build setup to build from multiple branches at the same time. Right now there 
are two phases to the build - a bootstrap phase and the main build phase. All build 
elements are stored in source control, but from the point of view of a build machine, 
the bootstrap phase lives outside source control and the main phase lives inside 
source control.

2) build types
There are different kinds of build types. Right now we define two types: "dev" and 
"release", although more types could be defined in the future if we need them. Individual 
parts of the build can use the build type to perform conditional behavior. For example, 
some of the longer-running tasks are only done as part of a release build. The goal is 
for the dev build to be very fast - that way the dev build could be done as part of 
a CI setup.	

3) build modules
The TEE build system is modular. Instead of one monolithic build script, there 
are a number of small modules that are merged together at build time. Each module 
contributes targets and properties to the build system. See the section below on 
modules for more information.

4) build configurations
The build system is driven by build configurations. Each build configuration defines 
a number of parameters. For example, the location in source control from which to 
pull down the source files is defined in the build configuration. The build configuration 
is also responsible for selecting which modules to run as part of the build.

5) build runtime engine
The build system requires an Eclipse 3.2 instance - this is called the build runtime 
engine. The build makes use of a number of features that are only available in Eclipse 
3.2 PDE build.

6) build eclipse base
Each build configuration can specify an Eclipse base to build against. This could be the 
same as the build runtime engine, but doesn't have to be. For instance, a build configuration 
could be created to build against Eclipse 3.0 (even though the build runtime engine is 
Eclipse 3.2). You probably want to make sure that build eclipse base has the delta pack 
installed into it. The build eclipse base should NOT have any TEE plugins installed 
into it, as this can interfere with the PDE build process.

7) build artifacts 
The outputs of the build are known as artifacts. Generally, each module defines targets that 
produce one or more artifacts. The most common example of an artifact is a packaged product 
(like the TEE CLC or TEE Eclipse plug-in). However, there are some artifacts that 
are never shipped - things like JUnit reports, code obfuscation maps, etc.

machine.properties:
-------------------
A build setup has an (optional) properties file for build-system-wide properties. 
This file is called machine.properties, and it should define the following properties:

dir.machine.build-runtime
dir.machine.working (optional, defaults to <bootstrap_dir>/working)
dir.machine.archive (optional, defaults to <bootstrap_dir>/archive)
name.archive.current (optional, defaults to "current")
dir.machine.build-configs (optional, defaults to <bootstrap_dir>)
name.product (optional)
name.machine.identifier - required, used to create unique workspace names

build-config.properties:
------------------------
Each build configuration is defined by a properties file (here called build-config.properties), 
and it should define the following properties:

dir.machine.build-runtime (optional, defaults to dir.machine.build-runtime in machine.properties)
config.build.type - dev, release (optional, defaults to "dev")
relpath.buildfile (optional, see "contract" section below) - DO NOT DOCUMENT IN TEMPLATE FILE
config.build.targets (optional, defaults to "*")
config.archive-current.enable (optional, defaults to not set)

build-config.properties should *not* include the number.build property. The build
numbering system has been redone and the build number is now a derived value.

Bootstrap build phase (phase 1):
--------------------------------
1) load the machine.properties file
	a) the default location is machine.properties in the same dir as the bootstrap build file
	b) the default location can be overriden by file.machine.properties
	c) certain properties (specified on the cmd line) are aliases for properties in machine.properties and are considered *first*:
		-- buildRuntime presets dir.machine.build-runtime
	d) any machine.properties property can be overridden by specifying it on the command line
	e) build only fails if required properties are not set (ie machine.properties is optional if required properties are set some other way)

2) load the build-config.properties file
	a) the default locations in order of precedence are:
		-- <dir.machine.build-configs>/<config.build.configuration>.properties
		-- <dir.machine.build-configs>/<build.tag>.properties (backcompat)
		-- <dir.machine.build-configs>/default-config.properties
	b) the default location can be overriden by file.build-config.properties
	c) certain properties (specified on the cmd line) are aliases for properties in build-config.properties and are considered *first*:
		-- buildNumber presets number.build
		-- eclipsebase presets dir.machine.build-runtime
		-- buildType presets config.build.type
		-- targets presets config.build.targets
	d) any build-config.properties property can be overriden by specifying it on the command line or in machine.properties
	e) build only fails if required properties are not set (ie build-config.properties is optional if required properties are set some other way)
	f) if the build-config.properties file exists, and number.build is not specified some other way then:
		-- the number.build property is incremented before loading build-config.properties, or set to 1000 if it does not exist in the file already
		
3) the main build (phase 2) is launched
	-- config.main-phase.verbose is set, the main phase will be launched in verbose mode

4) after the main build phase finishes successfully, the bootstrap phase copies artifacts:
	if config.archive-current.enable is set, a "current" symlink is established
	

Contract between bootstrap buildfile (phase 1) and main buildfile (phase 2):
----------------------------------------------------------------------------
1) Properties

properties are passed from the bootstrap phase to the main build phase
all properties known to the bootstrap phase are passed through (excluding java, ant, etc properties)
this means that the machine.properties or build-config.properties can contain properties that are not used by
the bootstrap phase but are intended for consumption by the main phase

-- the following properties are guaranteed (they will be preserved in future versions of the bootstrap build file):

	dir.machine.build-runtime
	name.product
	number.build
	dir.machine.build-runtime
	config.build.type
	config.build.targets

	config.buildtype.<config.build.type> will be set to "true", according to the value of <config.build.type> - useful for conditionals

	number.build (full qualifier, example "1440R_2_0_0")
	number.build.changeset (changeset number, example "1440")
	number.build.buildtype (build type, example "R" for release builds)
	number.build.branchtype (branch specifier, example "2_0_0")
	number.build.short (changeset + build type, example "1440R")

-- the following properties will be passed to the main build phase to support 1.X backcompat:
	baseLocation 
	buildDirectory
	build.number

2) when invoking the main build from the boostrap build, the following algorithm is used to select the main buildfile:
	a) first an attempt is made to find the property "relpath.main.buildfile". this can be defined in the following locations in order of precedence:
		-- in build-config.properties
		-- in <workingdir>/buildfile-location.properties
		-- in <workingdir>/build/buildfile-location.properties
	   if this property is defined, main build file is <workingdir>/<relpath.main.buildfile>
	b) otherwise, if <workingdir>/build/build.xml exists, that is used
	c) otherwise, if <workingdir>/newbuild/build.xml exists, that is used (backcompat, remove after 1.X is done)
	-- can be overridden by setting the property "file.bootstrap.main-build-file"
	
3) when the main build has finished successfully, it must create a properties file for the bootstrap to read, located:
	a) <workingdir>/build-results.properties	
	b) <workingdir>/product_version.properties (backcompat, remove after 1.X is done)
   This file MUST contain the following properties:
	number.version.major=14
	number.version.minor=0
	number.version.service=2
	dir.build.output (backcompat: output.location)


Build modules:
--------------
Each build module is a subdirectory of <workingdir>/build/modules
Each subdirectory contains a build.xml and (optionally) a build.properties file
All of the module build.xmls are merged at build time (this means it's ok for a module to depend on a task from another module)
All of the module build.properties are merged at build time (order unspecified, so property names should be unique)
Each module build.xml must include a target called "<MODULE_DIR_NAME>" and a target called "clean.<MODULE_DIR_NAME>"
this target is called when doing a build with an "all modules" build configuration
other targets in the module can be called when:
	a) a build configuration explictly names a target in the module
	b) another module explictly names a target in the module, through a depends, antcall, or similar
Each module will have two properties defined for it by default (of course these can be overridden in the module's build.properties) -
	name.dist-directory.<MODULE_DIR_NAME>
	name.filename-prefix.<MODULE_DIR_NAME>

Each module is given an opportunity to participate in a overall "clean" process at the start of each build. The reason an overall clean process is used instead of each module taking care of its own cleaning is for efficiency - if two modules have a common dependency then that dependency will only get cleaned once at the start of the build.

All module clean targets are always called at the start of each build, no matter which modules are being run as part of the build configuration.


Running the build with ant's mail logger:
-----------------------------------------
1) copy maillogger.properties.template to maillogger.properties and edit it
2) run the build like this:
ant -logger org.apache.tools.ant.listener.MailLogger -propertyfile maillogger.properties


Eclipse 3.2 PDE Build:
----------------------
Eclipse 3.2 introduced a few major new pde build features:
1) hook points for custom callbacks in the generated plugin and feature build scripts
2) pde build can now generate RCP product features from a .product file (https://bugs.eclipse.org/bugs/show_bug.cgi?id=107272)
3) pde build now supports fetching from repositories other than cvs (https://bugs.eclipse.org/bugs/show_bug.cgi?id=34757)
4) mac / intel support (https://bugs.eclipse.org/bugs/show_bug.cgi?id=138047 , https://bugs.eclipse.org/bugs/show_bug.cgi?id=98889)

The integrated help has been expanded for 3.2, and now includes much more pde build information. Most of it is in Eclipse help -> Plugin Development Environment Guide. To see what's new, look at "What's New".

Browse the pde build dev mailing list here: http://dev.eclipse.org/mhonarc/lists/pde-build-dev/maillist.html

In order to see minor changes made for 3.2, do a bugzilla search:
at: https://bugs.eclipse.org/bugs/query.cgi?format=advanced
product: PDE
component: Build
Version: 3.2
(optional) Resolution: Fixed

Design principles of the build scripts
--------------------------------------

1) Do not encode redundant information. eg there is no need to mark macro attribute names
with an "input" tag (like input.myattributename), since it is clear from the usage and definition
that they are inputs to the macrodef. eg no need to document task dependencies in the task header comment,
since it is clear by looking at the task what the dependencies are

2) global assumptions can be stated at the beginning of a build file (or in a readme.txt) and
do not have to be repeated for each task (eg the presence of pde build tasks)

3) it is not neccessary to define a property for absolutely every literal string that appears
in a build file. eg for a filename that is only used once, in one target, and has no meaning to other
targets it is not really neccessary to define a property for that name. only define a property when
it lowers the cost of changing the build without needlessly increasing the complexity of the build

4) prefer declarative solutions. eg use <xslt> task and an xslt instead of writing a custom ant task
to tweak a generated xml file

Target design principles
------------------------
1) target names use underscores:
	<target name="load_build_configuration">

many targets have corresponding skip.<name> and force.<name> properties
these properties are never set by the build script itself and are intended for external configuration

force takes precedence over skip if both are set

<target name="check_<target_name>">
	<condition property="check.<target_name>" value="true">
		<or>
			<not>
				<isset property="skip.<name>" />
			</not>
			<isset property="force.<name>" />
		</or>
	</condition>
</target>

<target name="<target_name>" depends="check_<target_name>" if="check.<target_name>">
....
</target>


Properties, Macro Attributes, XSLT global parameters design principles
----------------------------------------------------------------------
in general, unless specified otherwise below:
<datatype>.[category].<name>

datatypes:
	dir:		full path to a directory
	file:		full path to a file
	relpath:	a relative path (never including a leading path separator character)
	name: 		an identifier
	number:		a number
	url:		a url
	password:	a password
	path:		a generic path of some kind (use dir, file, or relpath if possible)
	refid:		a reference to a path-like structure
	skip:		a boolean specifying that an operation or task should be skipped
	force:		a boolean specifying that an operation or task should be forced
	config:		configuration data

use hypens as separators:
	${file.build-config.properties}

Macrodef design principles
--------------------------

name: macro.[category/attribute].<name>
examples:
macro.rcpbuild
macro.internal.pdescript


Task / Macro Header Template design principles
----------------------------------------------

Suggested to use a header where possible:

	<!-- ====================================================================== -->
	<!-- name - description                                                     -->
	<!--                                                                        -->
	<!-- Properties assumed:                                                    -->
	<!--    prop1 - use                                                         -->
	<!--                                                                        -->
	<!-- (Macro) Parameters:                                                    -->
	<!--    param1 - use                                                        -->
	<!--    param1 - use (optional, default xxx)                                -->
	<!--                                                                        -->
	<!-- ====================================================================== -->
	
1) no blank lines between the header and start of task / macro
2) one blank line between the end of a task / macro and start of next header