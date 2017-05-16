@setlocal ENABLEDELAYEDEXPANSION
@echo off

if "%1" == "" goto usageerror
set REPO_ROOT=%1
set INDIR=%REPO_ROOT%\build\output\_signing
if not exist %INDIR% goto inputerror

echo Pack signed JAR files back to their archives in "%REPO_ROOT%\build\output\bin"
call :pack_signed "%INDIR%"

set INDIR=%REPO_ROOT%\build\output\bin
set OUTDIR=%REPO_ROOT%\build\output\_packages
if exist "%OUTDIR%" rmdir /S /Q "%OUTDIR%"

echo Move signed CLC, TEE, and SDK archives to "%OUTDIR"
for /R "%INDIR%" %%i in (*-signed.zip) do call :move_signed %%i %OUTDIR%

goto :end

:pack_signed

"%JAVA_HOME%\bin\java" -jar "%REPO_ROOT%\build\signing\TEESignMerge.jar" "%1"

goto :eof

:move_signed

set FILENAME=%~n1
set FILENAME=!FILENAME:~0,-7!

set FILEPATH=%~dp1
call :get_folder_name "!FILEPATH:~0,-1!"

mkdir "%2\%FILEFOLDER%"
move /Y "%1" "%2\%FILEFOLDER%\%FILENAME%.zip"

goto :eof

:get_folder_name
set FILEFOLDER=%~n1
goto :eof

:usageerror
echo The TEE repository root directory is not specified as the first parameter of the script.
exit /B 201

:inputerror
echo The input directory %INDIR% is missing.  Is this really the signed output root?
exit /B 202

:end