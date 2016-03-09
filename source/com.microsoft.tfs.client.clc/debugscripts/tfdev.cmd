@echo off
rem
rem  Script for debugging the CLC while developing in Eclipse.  Eclipse builds the classes into
rem  ../bin and the projects the CLC depends on must reside at ../..
rem

set LAUNCHER_APPLICATION=Team Foundation Version Control Tool
set LAUNCHER_CLASS=com.microsoft.tfs.client.clc.vc.Main
set SETTINGS_VENDOR=Microsoft
set SETTINGS_VENDOR_LOWERCASE=microsoft
set SETTINGS_APPLICATION=Team Explorer
set SETTINGS_VERSION=10.0

setlocal ENABLEEXTENSIONS
setlocal ENABLEDELAYEDEXPANSION

set BASE_DIRECTORY=%~dp0
set SETTINGS_DIRECTORY=%USERPROFILE%\Local Settings\Application Data\%SETTINGS_VENDOR%\%SETTINGS_APPLICATION%\%SETTINGS_VERSION%\

for %%i in ("%SETTINGS_DIRECTORY%policies\*.jar") do set CLC_CLASSPATH=!CLC_CLASSPATH!;"%%i"

for %%i in ("%BASE_DIRECTORY%lib\*.jar") do set CLC_CLASSPATH=!CLC_CLASSPATH!;"%%i"

rem  List of dependent projects.
set DEP_PROJECTS=com.microsoft.tfs.client.clc com.microsoft.tfs.core com.microsoft.tfs.core.httpclient com.microsoft.tfs.client.common com.microsoft.tfs.client.common.pid com.microsoft.tfs.core.ws.runtime com.microsoft.tfs.core.ws com.microsoft.tfs.util com.microsoft.tfs.jni com.microsoft.tfs.logging com.microsoft.tfs.console

rem  Add all the dependent project "bin" dirs to the classpath
for %%a in (%DEP_PROJECTS%) do (for /d %%u in (%BASE_DIRECTORY%..\..\%%a\bin) do set CLC_CLASSPATH=!CLC_CLASSPATH!;%%u)

rem  Include all the "lib" and "libs" directories
set DEP_PROJECTS_LIBDIRS=
for %%i in (%DEP_PROJECTS%) do set DEP_PROJECTS_LIBDIRS=!DEP_PROJECTS_LIBDIRS!;%BASE_DIRECTORY%..\..\%%i\lib;%BASE_DIRECTORY%..\..\%%i\libs

rem  Include all the directories inside the "lib" and "libs" directories (getting long here)
for %%a in (%DEP_PROJECTS_LIBDIRS%) do (for /d %%u in (%%a\*) do set DEP_PROJECTS_LIBDIRS=!DEP_PROJECTS_LIBDIRS!;%%u)

rem  Add each JAR in each libdir to the classpath
for %%a in (%DEP_PROJECTS_LIBDIRS%) do (for %%u in (%%a\*.jar) do set CLC_CLASSPATH=!CLC_CLASSPATH!;%%u)

rem  Set up debugging options
if DEFINED JAVA_DEBUG set JAVA_DEBUG_FLAGS=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y

setlocal DISABLEDELAYEDEXPANSION

java -Xmx512M %JAVA_DEBUG_FLAGS% -cp %CLC_CLASSPATH% "-Dcom.microsoft.tfs.jni.native.base-directory=%BASE_DIRECTORY%..\..\com.microsoft.tfs.jni\os" %LAUNCHER_CLASS% %*

set RETURN_VALUE=%errorlevel%
goto end

:end
if "%TP_NON_INTERACTIVE%" NEQ "" exit %RETURN_VALUE%
exit /B %RETURN_VALUE%
