SDK LAYOUT PROJECT
------------------

This project is used to create the layout for the SDK redistributable
(excluding the SDK jar file itself, which is created from a build task).
Go look at the build/modules/sdk/build.xml file to see how this is done.

Some, but not all, of the contents of this project end up in the SDK
archive (and some projects are copied into the SDK layout from other TFS
paths because it's easier to dev/test them that way).

The file sdk-includes.txt is used as an "includesfile" for an Ant FileSet
during the SDK build process (one pattern per line).  This file must include 
patterns for all the SDK items IN THIS PROJECT which should end up in the final
layout.  Items like the SDK JAR, native libraries, samples, and Javadoc are 
added in build/modules/sdk/build.xml and should not be listed in sdk-includes.txt.
