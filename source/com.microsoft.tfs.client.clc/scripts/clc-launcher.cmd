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

for %%i in ("%BASE_DIRECTORY%lib\*.jar") do (
    set CLC_CLASSPATH=!CLC_CLASSPATH!;"%%i"
)

setlocal DISABLEDELAYEDEXPANSION

java -Xmx2048M -cp %CLC_CLASSPATH% %TF_ADDITIONAL_JAVA_ARGS% @@LAUNCHER_CLASS@@ %*

set RETURN_VALUE=%errorlevel%
goto end

:missingCoreJar
echo Unable to find a required JAR: %BASE_DIRECTORY%\lib\com.microsoft.tfs.core.jar does not exist
set RETURN_VALUE=1

:end
if "%TP_NON_INTERACTIVE%" NEQ "" exit %RETURN_VALUE%
exit /B %RETURN_VALUE%
