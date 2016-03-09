This "translations" folder holds translations of product resources for this 
branch.  Translated resource files are manually added and maintained, 
and must follow strict layout rules so the automated build can assemble 
language packs correctly.

Layout:

	translations/<lang>_<COUNTRY>/<project>/...

The first part ("translations") is this directory.

The second part ("<lang>_<COUNTRY>") is an ISO locale string like "en_CA"
(for Canadian English).  It can be just a language like "fr" (no 
country) if desired.  There won't be any "en" or "en_US" resources in 
this archive, because those are always kept inside the source code projects.

The third part ("<project>") is some project name which exists in ../source,
often an Eclipse plug-in name.  For example, "com.microsoft.tfs.util".

The fourth part ("...") is the collection of translated resources in 
exactly the same structure (same directory and file names!) used in the 
English resource archive that was sent for translation.  This layout is
slightly different than the layout in the original plug-in source folders
(for example, the "src" dir is usually not part of resource paths).
Also, these resources have simple names like "messages.properties" which 
match the original names, NOT locale-specific names like "messages_en_CA.properties".  
It is critical to mirror the original structure so the build system can 
correctly handle these resources (it inserts the locale names when required).
