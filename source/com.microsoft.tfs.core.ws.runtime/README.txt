The com.microsoft.tfs.core.ws.runtime project provides run-time classes that code generated
by com.microsoft.tfs.core.ws.generator needs.

Some libraries from third-party projects (Woodstox) are included and re-exported from 
this project.  These libraries define and implement some classes generated code
requires.

Programs using classes written by com.microsoft.tfs.core.ws.generator should take a dependency on 
com.microsoft.tfs.core.ws.runtime.