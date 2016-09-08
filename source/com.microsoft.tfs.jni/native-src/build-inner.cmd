@echo off
rem
rem Copyright (c) Microsoft. All rights reserved.
rem Licensed under the MIT license. See License.txt in the repository root.
rem
rem Builds the native libraries for a Windows host and runs unit tests.
rem
rem Compiles native C code into DLLs for Windows using the Visual Studio
rem C compiler (cl.exe).  Standard installs of 64-bit VS 2010 and 64-bit
rem Java 6 are expected.  Run this program in the directory where you have a copy
rem of the Team Explorer Everywhere code checked out (this path must contain all of
rem com.microsoft.tfs.jni, com.microsoft.tfs.logging, and com.microsoft.tfs.util).
rem
rem When you set your environment variables, DO NOT surround their values
rem with quotes.  This breaks the variable substitution in the compiler
rem call.
rem
rem Environment variables that affect this script:
rem
rem   DEBUG      Defines debug constants and flags for the native build
rem   NOJAVAH    Skips the javac and javah steps (for quick rebuilds when
rem              changing only the native code and not the Java interfaces)

if "%1" == "" goto usageerror
set ARCH=%1

rem Where headers, objects, DLLs are built
set BUILD_TMP=autobuild.%ARCH%

rem ###########################################################
rem Compiler Flags
rem ###########################################################

rem Suppress version banner
set CFLAGS=%CFLAGS% -nologo

rem SDL requirements: GS=buffers security check
set CFLAGS=%CFLAGS% -GS

rem SDL requirements: banned functions
set CFLAGS=%CFLAGS% -D"_SDL_BANNED_RECOMMENDED=1"

rem SDL requirements: use code analysis (PREfast).  This slows 
rem compilation down a bit, but it's nice to have the warnings
rem enabled so we catch and fix bugs early.
set CFLAGS=%CFLAGS% -analyze

rem High warnings (and treat as errors)
rem set CFLAGS=%CFLAGS% -W3 -WX
set CFLAGS=%CFLAGS% -W3

rem Zi=Complete debugging information (in PDB file)
set CFLAGS=%CFLAGS% -Zi

rem All files are "C" source files (not C++)
set CFLAGS=%CFLAGS% -TC

rem Unicode and other definitions
set CFLAGS=%CFLAGS% -D"SECURITY_WIN32" -D"UNICODE" -D"_UNICODE"

rem Include path
set CFLAGS=%CFLAGS% -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -I"%BUILD_TMP%" -I"common" -I"win32"

rem Switch for debug vs. release.  Defining DEBUG and _DEBUG may enable
rem noisy debugging to stdout/stderr in native code.

if defined DEBUG (
	echo+
	echo @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	echo @@@ DEBUG environment variable is set, building debug libraries
	echo @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	echo+
	set CFLAGS=%CFLAGS% -Od -DDEBUG -D_DEBUG
) else (
	rem Ox=Maximum optimization
	set CFLAGS=%CFLAGS% -Ox -DNDEBUG
)

rem ###########################################################
rem # Linker Flags
rem ###########################################################

rem nologo=obviously don't print the logo
rem release=sets a flag in the DLL header
rem debug=creates a PDB file and includes debug symbols in the DLL
rem wx=all linker warnings are errors
rem opt:ref=Remove unreferenced functions and data (makes much smaller DLLs)
rem opt:icf=Perform identical COMDAT folding (makes slightly smaller DLLs)
set LFLAGS=%LFLAGS% -nologo -release -debug -wx -opt:ref -opt:icf

rem SDL requires these flags.
if "%ARCH%" == "x86" set LFLAGS=%LFLAGS% -safeseh -nxcompat -dynamicbase
if "%ARCH%" == "x64" set LFLAGS=%LFLAGS% -dynamicbase

rem ###########################################################
rem # Set environment variables for the arch required.
rem ###########################################################

if "%ARCH%" == "x86" set DESTDIR=..\os\win32\x86
if "%ARCH%" == "x64" set DESTDIR=..\os\win32\x86_64

if "%ARCH%" == "x86" set SYMBOLDIR=..\symbols\win32\x86
if "%ARCH%" == "x64" set SYMBOLDIR=..\symbols\win32\x86_64

if "%ARCH%" == "x86" set TESTJAVA=C:\Program Files (x86)\Java\jre6\bin\java.exe
if "%ARCH%" == "x64" set TESTJAVA=C:\Program Files\Java\jre6\bin\java.exe

if "%ARCH%" == "x86" set VCARCH=x86
if "%ARCH%" == "x64" set VCARCH=amd64

call "C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\vcvarsall.bat" %VCARCH%

rem ###########################################################
rem Clean up old builds
rem ###########################################################

if not defined NOJAVAH (
  rem Full clean
  if exist "%BUILD_TMP%" rd /s /q "%BUILD_TMP%"
) else (
  rem Only delete the native files 
  if exist "%BUILD_TMP%" del "%BUILD_TMP%\*.dll" "%BUILD_TMP%\*.obj"
)

if not exist "%BUILD_TMP%" md "%BUILD_TMP%"

rem ###########################################################
rem Compile the Java classes
rem ###########################################################

echo Compiling wrapper classes...
set CLASSPATH=.;%BUILD_TMP%;junit-3.8.2.jar;..\..\com.microsoft.tfs.logging\lib\commons-logging-1.1.3\commons-logging-1.1.3.jar;..\..\com.microsoft.tfs.logging\lib\log4j-1.2.14\log4j-1.2.14.jar

set SOURCEFILES=^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\Check.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\CollatorFactory.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\Messages.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\Platform.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\PlatformVersion.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\FileHelpers.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\GUID.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\TypesafeEnum.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\BitField.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\LocaleInvariantStringHelpers.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\StringUtil.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\HashUtils.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\NewlineUtils.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\base64\Base64.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\base64\BinaryEncoder.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\base64\BinaryDecoder.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\base64\Encoder.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\base64\Decoder.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\base64\EncoderException.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\base64\DecoderException.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\process\ProcessRunner.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\process\ProcessOutputReader.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\process\ProcessFinishedHandler.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\tasks\CanceledException.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\tasks\TaskMonitor.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\tasks\NullTaskMonitor.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\tasks\TaskMonitorService.java ^
        ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\IOUtils.java ^
        ..\src\com\microsoft\tfs\jni\internal\auth\NativeAuth.java ^
        ..\src\com\microsoft\tfs\jni\internal\console\NativeConsole.java ^
        ..\src\com\microsoft\tfs\jni\internal\filesystem\NativeFileSystem.java ^
        ..\src\com\microsoft\tfs\jni\internal\keychain\NativeKeychain.java ^
        ..\src\com\microsoft\tfs\jni\internal\negotiate\NativeNegotiate.java ^
        ..\src\com\microsoft\tfs\jni\internal\negotiate\NegotiateException.java ^
        ..\src\com\microsoft\tfs\jni\internal\negotiate\UnavailableNegotiate.java ^
        ..\src\com\microsoft\tfs\jni\internal\ntlm\JavaNTLM.java ^
        ..\src\com\microsoft\tfs\jni\internal\ntlm\MD4.java ^
        ..\src\com\microsoft\tfs\jni\internal\ntlm\NativeNTLM.java ^
        ..\src\com\microsoft\tfs\jni\internal\ntlm\NTLMException.java ^
        ..\src\com\microsoft\tfs\jni\internal\ntlm\NTLMVersionException.java ^
        ..\src\com\microsoft\tfs\jni\internal\platformmisc\NativePlatformMisc.java ^
        ..\src\com\microsoft\tfs\jni\internal\synchronization\NativeSynchronization.java ^
        ..\src\com\microsoft\tfs\jni\internal\wincredential\NativeWinCredential.java ^
        ..\src\com\microsoft\tfs\jni\internal\LibraryNames.java ^
        ..\src\com\microsoft\tfs\jni\loader\NativeLoader.java ^
        ..\src\com\microsoft\tfs\jni\AuthenticationEngine.java ^
        ..\src\com\microsoft\tfs\jni\Console.java ^
        ..\src\com\microsoft\tfs\jni\ConsoleUtils.java ^
        ..\src\com\microsoft\tfs\jni\FileSystem.java ^
        ..\src\com\microsoft\tfs\jni\FileSystemAttributes.java ^
        ..\src\com\microsoft\tfs\jni\FileSystemTime.java ^
        ..\src\com\microsoft\tfs\jni\FileSystemUtils.java ^
        ..\src\com\microsoft\tfs\jni\Keychain.java ^
        ..\src\com\microsoft\tfs\jni\KeychainAuthenticationType.java ^
        ..\src\com\microsoft\tfs\jni\KeychainEnum.java ^
        ..\src\com\microsoft\tfs\jni\KeychainInternetPassword.java ^
        ..\src\com\microsoft\tfs\jni\KeychainProtocol.java ^
        ..\src\com\microsoft\tfs\jni\KeychainUtils.java ^
        ..\src\com\microsoft\tfs\jni\Messages.java ^
        ..\src\com\microsoft\tfs\jni\MessageWindow.java ^
        ..\src\com\microsoft\tfs\jni\Negotiate.java ^
        ..\src\com\microsoft\tfs\jni\NegotiateEngine.java ^
        ..\src\com\microsoft\tfs\jni\NTLM.java ^
        ..\src\com\microsoft\tfs\jni\NTLMEngine.java ^
        ..\src\com\microsoft\tfs\jni\PlatformMisc.java ^
        ..\src\com\microsoft\tfs\jni\PlatformMiscUtils.java ^
        ..\src\com\microsoft\tfs\jni\RegistryException.java ^
        ..\src\com\microsoft\tfs\jni\RegistryKey.java ^
        ..\src\com\microsoft\tfs\jni\RegistryValue.java ^
        ..\src\com\microsoft\tfs\jni\RootKey.java ^
        ..\src\com\microsoft\tfs\jni\Synchronization.java ^
        ..\src\com\microsoft\tfs\jni\SynchronizationUtils.java ^
        ..\src\com\microsoft\tfs\jni\ValueType.java ^
        ..\src\com\microsoft\tfs\jni\WellKnownSID.java ^
        ..\src\com\microsoft\tfs\jni\WinCredential.java ^
        ..\src\com\microsoft\tfs\jni\WinCredentialUtils.java ^
        ..\tests\com\microsoft\tfs\jni\internal\console\ExecConsole.java ^
        ..\tests\com\microsoft\tfs\jni\internal\console\NativeConsoleTest.java ^
        ..\tests\com\microsoft\tfs\jni\internal\console\UnixExecConsole.java ^
        ..\tests\com\microsoft\tfs\jni\internal\console\WindowsExecConsole.java ^
        ..\tests\com\microsoft\tfs\jni\internal\filesystem\ExecFileSystem.java ^
        ..\tests\com\microsoft\tfs\jni\internal\filesystem\NativeFileSystemTest.java ^
        ..\tests\com\microsoft\tfs\jni\internal\filesystem\UnixExecFileSystem.java ^
        ..\tests\com\microsoft\tfs\jni\internal\filesystem\WindowsExecFileSystem.java ^
        ..\tests\com\microsoft\tfs\jni\internal\keychain\NativeKeychainTest.java ^
        ..\tests\com\microsoft\tfs\jni\internal\negotiate\NativeNegotiateTest.java ^
        ..\tests\com\microsoft\tfs\jni\internal\ntlm\NativeNTLMTest.java ^
        ..\tests\com\microsoft\tfs\jni\internal\platformmisc\ExecPlatformMisc.java ^
        ..\tests\com\microsoft\tfs\jni\internal\platformmisc\NativePlatformMiscTest.java ^
        ..\tests\com\microsoft\tfs\jni\internal\platformmisc\UnixExecPlatformMisc.java ^
        ..\tests\com\microsoft\tfs\jni\internal\platformmisc\WindowsExecPlatformMisc.java ^
        ..\tests\com\microsoft\tfs\jni\internal\registry\NativeRegistryTest.java ^
        ..\tests\com\microsoft\tfs\jni\internal\synchronization\NativeSynchronizationTest.java ^
        ..\tests\com\microsoft\tfs\jni\AllNativeTests.java ^
        ..\tests\com\microsoft\tfs\jni\MessageWindowTest.java ^
        ..\tests\com\microsoft\tfs\jni\ExecHelpers.java

if not defined NOJAVAH (
  rem We require a minimum of Java 1.5
  "%JAVA_HOME%\bin\javac" -source 1.5 %SOURCEFILES% -classpath %CLASSPATH% -encoding "UTF-8" -d "%BUILD_TMP%"
)

if not exist "%BUILD_TMP%\com\microsoft\tfs\jni\AllNativeTests.class" goto javacerror

rem ###########################################################
rem Copy localized message files so that tests can run
rem ###########################################################

if not defined NOJAVAH (
  echo Copying localized message data...
  copy ..\..\com.microsoft.tfs.util\src\com\microsoft\tfs\util\messages.properties "%BUILD_TMP%\com\microsoft\tfs\util"
  copy ..\src\com\microsoft\tfs\jni\messages.properties "%BUILD_TMP%\com\microsoft\tfs\jni"
)

rem ###########################################################
rem Generate .h with javah
rem ###########################################################

if not defined NOJAVAH (
  echo Generating C headers...
  "%JAVA_HOME%\bin\javah" -classpath "%BUILD_TMP%" -o "%BUILD_TMP%\native_auth.h" com.microsoft.tfs.jni.internal.auth.NativeAuth
  "%JAVA_HOME%\bin\javah" -classpath "%BUILD_TMP%" -o "%BUILD_TMP%\native_console.h" com.microsoft.tfs.jni.internal.console.NativeConsole
  "%JAVA_HOME%\bin\javah" -classpath "%BUILD_TMP%" -o "%BUILD_TMP%\native_filesystem.h" com.microsoft.tfs.jni.internal.filesystem.NativeFileSystem
  "%JAVA_HOME%\bin\javah" -classpath "%BUILD_TMP%" -o "%BUILD_TMP%\native_misc.h" com.microsoft.tfs.jni.internal.platformmisc.NativePlatformMisc
  "%JAVA_HOME%\bin\javah" -classpath "%BUILD_TMP%" -o "%BUILD_TMP%\native_synchronization.h" com.microsoft.tfs.jni.internal.synchronization.NativeSynchronization
  "%JAVA_HOME%\bin\javah" -classpath "%BUILD_TMP%" -o "%BUILD_TMP%\native_registry.h" com.microsoft.tfs.jni.RegistryKey
  "%JAVA_HOME%\bin\javah" -classpath "%BUILD_TMP%" -o "%BUILD_TMP%\native_messagewindow.h" com.microsoft.tfs.jni.MessageWindow
)

if not exist "%BUILD_TMP%\native_auth.h" goto javaherror
if not exist "%BUILD_TMP%\native_console.h" goto javaherror
if not exist "%BUILD_TMP%\native_filesystem.h" goto javaherror
if not exist "%BUILD_TMP%\native_misc.h" goto javaherror
if not exist "%BUILD_TMP%\native_synchronization.h" goto javaherror
if not exist "%BUILD_TMP%\native_registry.h" goto javaherror
if not exist "%BUILD_TMP%\native_messagewindow.h" goto javaherror

rem ###########################################################
rem Echo versions as reminders
rem ###########################################################

echo+
echo Compiler version:
cl.exe |findstr Microsoft
echo Linker version:
link.exe nul |findstr Microsoft
echo+

rem ###########################################################
rem Compile and link native code
rem ###########################################################

echo Compiling native C code...

rem Compile everything to objects
@echo on
cl -c win32\auth_sspi.c -Fo"%BUILD_TMP%\auth_sspi.obj" %CFLAGS%
cl -c win32\console_jni.c -Fo"%BUILD_TMP%\console_jni.obj" %CFLAGS%
cl -c win32\filesystem_jni.c -Fo"%BUILD_TMP%\filesystem_jni.obj" %CFLAGS%
cl -c win32\misc_jni.c -Fo"%BUILD_TMP%\misc_jni.obj" %CFLAGS%
cl -c win32\synchronization_jni.c -Fo"%BUILD_TMP%\synchronization_jni.obj" %CFLAGS%
cl -c win32\registry_jni.c -Fo"%BUILD_TMP%\registry_jni.obj" %CFLAGS%
cl -c win32\wincred_jni.c -Fo"%BUILD_TMP%\wincred_jni.obj" %CFLAGS%
cl -c win32\messagewindow_jni.c -Fo"%BUILD_TMP%\messagewindow_jni.obj" %CFLAGS%
cl -c common\auth.c -Fo"%BUILD_TMP%\auth.obj" %CFLAGS%
cl -c common\logger_log4j.c -Fo"%BUILD_TMP%\logger_log4j.obj" %CFLAGS%
cl -c common\objects.c -Fo"%BUILD_TMP%\objects.obj" %CFLAGS%
cl -c common\util.c -Fo"%BUILD_TMP%\util.obj" %CFLAGS%
@echo off

if not exist "%BUILD_TMP%\auth_sspi.obj" goto compileerror
if not exist "%BUILD_TMP%\console_jni.obj" goto compileerror
if not exist "%BUILD_TMP%\filesystem_jni.obj" goto compileerror
if not exist "%BUILD_TMP%\misc_jni.obj" goto compileerror
if not exist "%BUILD_TMP%\synchronization_jni.obj" goto compileerror
if not exist "%BUILD_TMP%\registry_jni.obj" goto compileerror
if not exist "%BUILD_TMP%\wincred_jni.obj" goto compileerror
if not exist "%BUILD_TMP%\messagewindow_jni.obj" goto compileerror
if not exist "%BUILD_TMP%\auth.obj" goto compileerror
if not exist "%BUILD_TMP%\util.obj" goto compileerror
if not exist "%BUILD_TMP%\logger_log4j.obj" goto compileerror

rem Link individual libraries.

@echo on
link -dll "%BUILD_TMP%\auth.obj" "%BUILD_TMP%\auth_sspi.obj" "%BUILD_TMP%\util.obj" "%BUILD_TMP%\logger_log4j.obj" -out:"%BUILD_TMP%\native_auth.dll" %LFLAGS%
link -dll "%BUILD_TMP%\console_jni.obj" "%BUILD_TMP%\util.obj" -out:"%BUILD_TMP%\native_console.dll" %LFLAGS%
link -dll "%BUILD_TMP%\filesystem_jni.obj" "%BUILD_TMP%\util.obj" "%BUILD_TMP%\objects.obj" advapi32.lib -out:"%BUILD_TMP%\native_filesystem.dll" %LFLAGS%
link -dll "%BUILD_TMP%\misc_jni.obj" "%BUILD_TMP%\util.obj" advapi32.lib -out:"%BUILD_TMP%\native_misc.dll" %LFLAGS%
link -dll "%BUILD_TMP%\synchronization_jni.obj" "%BUILD_TMP%\util.obj" -out:"%BUILD_TMP%\native_synchronization.dll" %LFLAGS%
link -dll "%BUILD_TMP%\registry_jni.obj" "%BUILD_TMP%\util.obj" advapi32.lib -out:"%BUILD_TMP%\native_registry.dll" %LFLAGS%
link -dll "%BUILD_TMP%\wincred_jni.obj" "%BUILD_TMP%\util.obj" advapi32.lib -out:"%BUILD_TMP%\native_credential.dll" %LFLAGS%
link -dll "%BUILD_TMP%\messagewindow_jni.obj" "%BUILD_TMP%\util.obj" user32.lib -out:"%BUILD_TMP%\native_messagewindow.dll" %LFLAGS%
@echo off

if not exist "%BUILD_TMP%\native_auth.dll" goto compileerror
if not exist "%BUILD_TMP%\native_console.dll" goto compileerror
if not exist "%BUILD_TMP%\native_filesystem.dll" goto compileerror
if not exist "%BUILD_TMP%\native_misc.dll" goto compileerror
if not exist "%BUILD_TMP%\native_synchronization.dll" goto compileerror
if not exist "%BUILD_TMP%\native_registry.dll" goto compileerror
if not exist "%BUILD_TMP%\native_credential.dll" goto compileerror
if not exist "%BUILD_TMP%\native_messagewindow.dll" goto compileerror

rem ###########################################################
rem Run tests
rem ###########################################################

echo+
echo Running tests.
echo+
echo+

echo Running Java at: %TESTJAVA%
echo+

"%TESTJAVA%" -Djava.library.path="%BUILD_TMP%" -classpath %CLASSPATH% junit.textui.TestRunner com.microsoft.tfs.jni.AllNativeTests

if %ERRORLEVEL% neq 0 goto end

echo Copying to path for checkin...
if exist %DESTDIR%\native_auth.dll del %DESTDIR%\native_auth.dll
if exist %DESTDIR%\native_auth.dll goto delerror
if exist %SYMBOLDIR%\native_auth.pdb del %SYMBOLDIR%\native_auth.pdb
if exist %SYMBOLDIR%\native_auth.pdb goto delerror

if exist %DESTDIR%\native_console.dll del %DESTDIR%\native_console.dll
if exist %DESTDIR%\native_console.dll goto delerror
if exist %SYMBOLDIR%\native_console.pdb del %SYMBOLDIR%\native_console.pdb
if exist %SYMBOLDIR%\native_console.pdb goto delerror

if exist %DESTDIR%\native_filesystem.dll del %DESTDIR%\native_filesystem.dll
if exist %DESTDIR%\native_filesystem.dll goto delerror
if exist %SYMBOLDIR%\native_filesystem.pdb del %SYMBOLDIR%\native_filesystem.pdb
if exist %SYMBOLDIR%\native_filesystem.pdb goto delerror

if exist %DESTDIR%\native_misc.dll del %DESTDIR%\native_misc.dll
if exist %DESTDIR%\native_misc.dll goto delerror
if exist %SYMBOLDIR%\native_misc.pdb del %SYMBOLDIR%\native_misc.pdb
if exist %SYMBOLDIR%\native_misc.pdb goto delerror

if exist %DESTDIR%\native_synchronization.dll del %DESTDIR%\native_synchronization.dll
if exist %DESTDIR%\native_synchronization.dll goto delerror
if exist %SYMBOLDIR%\native_synchronization.pdb del %SYMBOLDIR%\native_synchronization.pdb
if exist %SYMBOLDIR%\native_synchronization.pdb goto delerror

if exist %DESTDIR%\native_registry.dll del %DESTDIR%\native_registry.dll
if exist %DESTDIR%\native_registry.dll goto delerror
if exist %SYMBOLDIR%\native_registry.pdb del %SYMBOLDIR%\native_registry.pdb
if exist %SYMBOLDIR%\native_registry.pdb goto delerror

if exist %DESTDIR%\native_credential.dll del %DESTDIR%\native_credential.dll
if exist %DESTDIR%\native_credential.dll goto delerror
if exist %SYMBOLDIR%\native_credential.pdb del %SYMBOLDIR%\native_credential.pdb
if exist %SYMBOLDIR%\native_credential.pdb goto delerror

if exist %DESTDIR%\native_messagewindow.dll del %DESTDIR%\native_messagewindow.dll
if exist %DESTDIR%\native_messagewindow.dll goto delerror
if exist %SYMBOLDIR%\native_messagewindow.pdb del %SYMBOLDIR%\native_messagewindow.pdb
if exist %SYMBOLDIR%\native_messagewindow.pdb goto delerror

if not exist %DESTDIR% md %DESTDIR%
if not exist %SYMBOLDIR% md %SYMBOLDIR%
if not exist %DESTDIR% goto copyerror

copy "%BUILD_TMP%\native_auth.dll" %DESTDIR%\native_auth.dll
if not exist %DESTDIR%\native_auth.dll goto copyerror
copy "%BUILD_TMP%\native_auth.pdb" %SYMBOLDIR%\native_auth.pdb
if not exist %SYMBOLDIR%\native_auth.pdb goto copyerror

copy "%BUILD_TMP%\native_console.dll" %DESTDIR%\native_console.dll
if not exist %DESTDIR%\native_console.dll goto copyerror
copy "%BUILD_TMP%\native_console.pdb" %SYMBOLDIR%\native_console.pdb
if not exist %SYMBOLDIR%\native_console.pdb goto copyerror

copy "%BUILD_TMP%\native_filesystem.dll" %DESTDIR%\native_filesystem.dll
if not exist %DESTDIR%\native_filesystem.dll goto copyerror
copy "%BUILD_TMP%\native_filesystem.pdb" %SYMBOLDIR%\native_filesystem.pdb
if not exist %SYMBOLDIR%\native_filesystem.pdb goto copyerror

copy "%BUILD_TMP%\native_misc.dll" %DESTDIR%\native_misc.dll
if not exist %DESTDIR%\native_misc.dll goto copyerror
copy "%BUILD_TMP%\native_misc.pdb" %SYMBOLDIR%\native_misc.pdb
if not exist %SYMBOLDIR%\native_misc.pdb goto copyerror

copy "%BUILD_TMP%\native_synchronization.dll" %DESTDIR%\native_synchronization.dll
if not exist %DESTDIR%\native_synchronization.dll goto copyerror
copy "%BUILD_TMP%\native_synchronization.pdb" %SYMBOLDIR%\native_synchronization.pdb
if not exist %SYMBOLDIR%\native_synchronization.pdb goto copyerror

copy "%BUILD_TMP%\native_registry.dll" %DESTDIR%\native_registry.dll
if not exist %DESTDIR%\native_registry.dll goto copyerror
copy "%BUILD_TMP%\native_registry.pdb" %SYMBOLDIR%\native_registry.pdb
if not exist %SYMBOLDIR%\native_registry.pdb goto copyerror

copy "%BUILD_TMP%\native_credential.dll" %DESTDIR%\native_credential.dll
if not exist %DESTDIR%\native_credential.dll goto copyerror
copy "%BUILD_TMP%\native_credential.pdb" %SYMBOLDIR%\native_credential.pdb
if not exist %SYMBOLDIR%\native_credential.pdb goto copyerror

copy "%BUILD_TMP%\native_messagewindow.dll" %DESTDIR%\native_messagewindow.dll
if not exist %DESTDIR%\native_messagewindow.dll goto copyerror
copy "%BUILD_TMP%\native_messagewindow.pdb" %SYMBOLDIR%\native_messagewindow.pdb
if not exist %SYMBOLDIR%\native_messagewindow.pdb goto copyerror

echo Done.  Libraries copied to %DESTDIR%, symbols copied to %SYMBOLDIR%
goto end

:delerror
echo Couldn't delete an existing library in %DESTDIR% or %SYMBOLDIR%!  Did you check out these files?
goto end

:copyerror
echo Couldn't copy files to %DESTDIR% or %SYMBOLDIR%!  Do these directories exist?
goto end

:compileerror
echo Native code failed to compile.
goto end

:javacerror
echo java failed to build the Java class wrappers.
goto end

:javaherror
echo javah failed to build headers.
goto end

:jvmerror
echo JAVA_HOME must be set to an installed JDK.
goto end

:usageerror
echo+
echo usage: %0 "x86"^|"x64"
echo+
echo Specify 32-bit or 64-bit target.  Ensure cl.exe and link.exe in your path 
echo are for the specified target.
goto end

:end

set PATH=%OLDPATH%
