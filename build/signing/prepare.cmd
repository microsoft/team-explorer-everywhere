@setlocal ENABLEDELAYEDEXPANSION
rem @echo off

if "%1" == "" goto usageerror
set REPO_ROOT=%1
set INDIR=%REPO_ROOT%\build\output\bin
if not exist %INDIR%\clc goto inputerror

set OUTDIR=%REPO_ROOT%\build\output\_signing
if exist %OUTDIR% rmdir /S /Q %OUTDIR%

set SOURCE=
for /R %INDIR% %%i in (TEE-CLC-1*.zip TFSEclipsePlugin-UpdateSiteArchive-1*.zip TFS-SDK-1*.zip) do set SOURCE=!SOURCE! %%i

"%JAVA_HOME%\bin\java" -jar %REPO_ROOT%\build\signing\TEESignPrep.jar %OUTDIR% !SOURCE!

goto end

:usageerror
echo The TEE repository root directory is not specified as the first parameter of the script.
exit /B 101

:inputerror
echo The input directory %INDIR% is missing the "clc" directory.  Is this really the build output root?
exit /B 102

:end