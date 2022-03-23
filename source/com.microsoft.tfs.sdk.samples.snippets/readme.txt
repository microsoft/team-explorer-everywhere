
Instructions for Using Snippets

1. Edit src/com/microsoft/tfs/sdk/samples/snippets/SnippetSettings.java to
   specify your TFS project collection URL, credentials, and other preferred 
   values.

2. Run "ant compile" to compile the Java code in this directory.

3. Run each snippet program like:

   (Windows)
   java -Dcom.microsoft.tfs.jni.native.base-directory=..\..\redist\native -classpath ..\..\redist\lib\com.microsoft.tfs.sdk-14.137.0.jar;.\bin com.microsoft.tfs.sdk.samples.snippets.SnippetClassName

   (Unix and Mac OS)
   java -Dcom.microsoft.tfs.jni.native.base-directory=../../redist/native -classpath ../../redist/lib/com.microsoft.tfs.sdk-14.137.0.jar:./bin com.microsoft.tfs.sdk.samples.snippets.SnippetClassName
