@echo off
rem
rem  Configures the classpath for Java and starts the @@LAUNCHER_APPLICATION@@.
rem

setlocal ENABLEDELAYEDEXPANSION

set BASE_DIRECTORY=%~dp0
set SETTINGS_DIRECTORY=%USERPROFILE%\Local Settings\Application Data\@@SETTINGS_VENDOR@@\@@SETTINGS_APPLICATION@@\@@SETTINGS_VERSION@@\

if not exist "%BASE_DIRECTORY%lib\com.microsoft.tfs.core.jar" goto missingCoreJar

set CLC_CLASSPATH=

rem Add check-in policy implementations in the user's home directory
rem first, so they can override standard CLC libraries.

for %%i in ("%SETTINGS_DIRECTORY%policies\*.jar") do set CLC_CLASSPATH=!CLC_CLASSPATH!;"%%i"

rem Standard CLC resources.  Site-wide check-in policies can be dropped in 
rem the lib directory.
rem
rem 3/25/2015 Workaround added:
rem Make sure that AppInsights SDK is the last in class path, 
rem otherwise it will destroy CLC logging.
rem Can be removed after (and if) the issue is fixed in AppINsights

set AI_JAR=
for %%i in ("%BASE_DIRECTORY%lib\*.jar") do (
    for /F "tokens=1 delims=- " %%j in ("%%~ni") do (
        if /I "%%j" == "applicationinsights" (
            set AI_JAR="%%i"
        ) else (
            set CLC_CLASSPATH=!CLC_CLASSPATH!;"%%i"
        )
    )
)
set CLC_CLASSPATH=%CLC_CLASSPATH%;%AI_JAR%

setlocal DISABLEDELAYEDEXPANSION

java -Xmx2048M -cp %CLC_CLASSPATH% %TF_ADDITIONAL_JAVA_ARGS% "-Dcom.microsoft.tfs.jni.native.base-directory=%BASE_DIRECTORY%native" @@LAUNCHER_CLASS@@ %*

set RETURN_VALUE=%errorlevel%
goto end

:missingCoreJar
echo Unable to find a required JAR: %BASE_DIRECTORY%\lib\com.microsoft.tfs.core.jar does not exist
set RETURN_VALUE=1

:end
if "%TP_NON_INTERACTIVE%" NEQ "" exit %RETURN_VALUE%
exit /B %RETURN_VALUE%
