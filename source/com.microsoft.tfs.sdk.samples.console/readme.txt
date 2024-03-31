
Instructions for Using Console Samples

1. Edit src/com/microsoft/tfs/sdk/samples/console/ConsoleSettings.java to 
   specify your TFS project collection URL, credentials, and other preferred 
   values.

2. Run "ant compile" to compile the Java code in this directory.

3. Run each snippet program like:
   
   (Windows)
   java -classpath ..\..\redist\lib\com.microsoft.tfs.sdk-14.134.0.jar;.\bin com.microsoft.tfs.sdk.samples.console.ConsoleSampleClassName

   (Unix and Mac OS)
   java -classpath ../../redist/lib/com.microsoft.tfs.sdk-14.134.0.jar:./bin com.microsoft.tfs.sdk.samples.console.ConsoleSampleClassName
