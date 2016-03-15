#!/usr/bin/env bash
#
# Copyright (c) Microsoft. All rights reserved.
# Licensed under the MIT license. See License.txt in the repository root.
#
# Builds the native libraries for a Unix host and runs unit tests.

set -e

# Option defaults
PREVIEW=0
DEBUG=0
DISABLE_ALL_TESTS=0
DISABLE_INT_TESTS=0
VALGRIND=0
DEST=../os
DEST_DEFAULT=$DEST
DEST_SPECIFIED=0

usage() {
    echo "usage: $0 [-preview] [-debug] [-noTests] [-noInteractiveTests] [-valgrind] [<outputDirectory>]"
    echo
    echo "Builds native libraries for this Unix platform."
    echo
    echo "  If -preview specified, no build is done.  Instead, the "
    echo "  relative paths of the expected output files are printed" 
    echo "  to standard output."
    echo
    echo "  If -debug is provided, non-optimized and debug versions of the"
    echo "  library are built.  The DEBUG preprocessor macro is defined"
    echo "  and compiler flags are changed."
    echo 
    echo "  If -noInteractiveTests is provided, unit tests that require input"
    echo "  do not run."
    echo
    echo "  If -noTests is provided, no unit tests run."
    echo
    echo "  If -valgrind is provided, the tests are run inside the Valgrind"
    echo "  profiling program."
    echo
    echo "  If <outputDirectory> is set, the output files are copied there"
    echo "  instead of the default location ($DEST_DEFAULT)."
}

# Parse options
while [ $# -ge 1 ] ; do

	case "$1" in
	    -h|-help|--help)
	        usage
	        exit 1
	        ;;
	    -preview)
	    	PREVIEW=1
	        ;;
	    -debug)
	        DEBUG=1
	        ;;
	    -noTests)
	    	DISABLE_ALL_TESTS=1
	        ;;
	    -noInteractiveTests)
	        DISABLE_INT_TESTS=1
	        ;;
	    -valgrind)
	        VALGRIND=1
	        ;;
	    -*)
	    	echo "Unknown option $1."
	        echo
    	    usage
        	exit 1
        	;;
	    *)
	    	if [ "$DEST_SPECIFIED" = 0 ] ; then
	    		DEST=$1
	    		DEST_SPECIFIED=1
    		else
    			echo "Too many arguments at $1."
	        	echo
	        	usage
	        	exit 1
	    	fi
	        ;;
	esac
	
    shift
done

# Builds a library using GCC obeying CFLAGS and LDFLAGS.
#
# Arguments:
# 1: Shared library output file
# 2: Input files
build_library_gcc() {
    if [ "$PREVIEW" = 0 ]; then
        # Newer versions of GCC (4.6.1 on Ubuntu 11.10 specifically) need LDFLAGS after all the objects.
        CMD="gcc $CFLAGS -o $1 $2 $LDFLAGS"

        echo $CMD
        eval $CMD
    fi

    DEPLOY="$DEPLOY `basename $1`"
}

# Builds a library using c99 obeying CFLAGS and LDFLAGS.
#
# Arguments:
# 1: Shared library output file
# 2: path to base directory where EBCDIC source lives (this is prepended to each input file before
#    it is compiled).
# 3: Input files
build_library_zos() {
    # Java's side deck is an input to our link step.  Use "j9vm" instead of "classic" for Java 5.
    JAVA_SIDEDECK="$JAVA_HOME/bin/classic/libjvm.x"

    # Our objects and sidedeck
    OBJS=""
    SIDEDECK=`basename $1 | sed -e "s/\.so$/.x/"`

    if [ "$PREVIEW" = 0 ]; then
        for SOURCE in $3 ; do
            EBCDIC_SOURCE=$2/$SOURCE

            OBJ=`basename $EBCDIC_SOURCE | sed -e "s/\.c$/.o/"`

            CMD="c99 $CFLAGS -c -o $TMP/$OBJ $EBCDIC_SOURCE"
            echo $CMD
            eval $CMD

            OBJS="$OBJS $TMP/$OBJ"
        done

        # "Bind" step (called linking on other platforms).
        CMD="c99 $LDFLAGS -o $1 $OBJS $JAVA_SIDEDECK $LD_EXTRA"
        echo $CMD
        eval $CMD

        # xlc doesn't seem to have an option for where to put the side deck, so
        # we move it from the current directory to where we want it ($TMP).
        CMD="mv ./$SIDEDECK $TMP/$SIDEDECK"
        echo $CMD
        eval $CMD
    fi

    DEPLOY="$DEPLOY `basename $1` $SIDEDECK"
}

# We have to detect our platform and architecture, and map them to Eclipse's
# standard architectures and platforms, so we can package them correctly in
# an archive for the class loader to find.
E=echo
case `uname -s` in
    AIX)
        PLATFORM=aix
        ARCH=`uname -p`
        ;;
    Linux)
        PLATFORM=linux
        ARCH=`uname -m`
        ;;
    SunOS)
        PLATFORM=solaris
        ARCH=`uname -p`
        E=/usr/ucb/echo
        ;;
    HP-UX)
        PLATFORM=hpux
        ARCH=`uname -m`
        TMP=`mktemp -d .`
        mkdir $TMP
        ;;
    Darwin)
        PLATFORM=macosx
        ARCH="universal"
        ;;
    FreeBSD)
        PLATFORM=freebsd
        ARCH=`uname -m`
        ;;
    OS/390)
        PLATFORM=zos
        ARCH=390
        JAVA_SOURCE_ENCODING_FLAGS="-encoding ISO8859-1"
        ;;
    *)
        echo "ERROR: Your operating system (\"`uname -s`\") is not supported.  Please edit "
        echo "ERROR: $0 to support it."
        exit 1
        ;;
esac

# On Mac OS X, we always know where we can find the JDK.
if [ "$PLATFORM" != "macosx" ] ; then
    if [ -z "$JAVA_HOME" ] ; then
        echo "JAVA_HOME must be set to an installed JDK."
        exit 10
    fi
fi

# Convert the architecture we got back from uname into something Eclipse
# understands.  Some of these are already the correct names, but we include
# them so other aliases may be added if they show up in some distribution/OS.
case $ARCH in
    i386|i486|i586|i686)
        ARCH=x86

        # Solaris reports i386 for both x86 and AMD64 versions, so we have
        # to do more detection for architecture size.
        if [ "$PLATFORM" = "solaris" ] ; then
            if [ `isainfo -b` = "64" ] ; then
                ARCH="x86_64"
            fi
        fi

        ;;
    x86_64|amd64)
        ARCH=x86_64
        ;;
    sparc)
        ARCH=sparc
        ;;
    ppc|powerpc|ppc64)
        # ppc64 will map here but we will force 32-bit code compilation
        # below for both AIX and Linux.  Most applications on these platforms
        # are 32-bit even with 64-bit kernels.
        ARCH=ppc
        ;;
    universal)
        ;;
    9000/*)
        ARCH=PA_RISC
        ;;
    ia64)
        # This script can't build two different libs in one pass, so we just
        # do 32-bit on Itanium for now, since that's all Eclipse 3.4 supports.
        # In the future this script could be extended to build two different
        # shared libraries.
        ARCH=ia64_32
        ;;
    390)
        ;;
    arm*)
        ARCH=arm
        ;;
    *)
        $E "ERROR: Your architecture (\"$ARCH\") is not supported.  Please edit "
        $E "ERROR: $0 to support it."
        exit 2
    ;;
esac

# The simple names of the libraries we generate.  A "lib" prefix
# and/or file extension is added per-platform.
LIBRARY_AUTH="native_auth"
LIBRARY_CONSOLE="native_console"
LIBRARY_FILESYSTEM="native_filesystem"
LIBRARY_MISC="native_misc"
LIBRARY_SYNCHRONIZATION="native_synchronization"
LIBRARY_KEYCHAIN="native_keychain"

# The sources required by each library we compile and link.
SOURCES_AUTH="common/util.c common/objects.c common/logger_log4j.c common/auth.c unix/auth_gss.c"
SOURCES_CONSOLE="common/util.c common/objects.c unix/console_jni.c"
SOURCES_FILESYSTEM="common/util.c common/objects.c unix/filesystem_jni.c"
SOURCES_MISC="common/util.c common/objects.c unix/misc_jni.c"
SOURCES_SYNCHRONIZATION="common/util.c common/objects.c unix/synchronization_jni.c"
SOURCES_KEYCHAIN="common/util.c common/objects.c common/logger_log4j.c unix/keychain_jni.c"

# Do the build (if building).
if [ "$PREVIEW" = 0 ] ; then
    # If we didn't do one of the special TMP creations above, create one now.
    if [ -z "$TMP" ] ; then
        TMP=`mktemp -d autobuild.XXXXXX`
    fi

    if [ ! -d "$TMP" ] ; then
        $E "Temporary directory $TMP disappeared!"
        exit 3
    fi

    # A log4j configuration file exists in this directory for assistance debugging.
    CLASSPATH=.:$TMP:junit-3.8.2.jar:../../com.microsoft.tfs.logging/lib/commons-logging-1.1/commons-logging-1.1.jar:../../com.microsoft.tfs.logging/lib/log4j-1.2.14/log4j-1.2.14.jar
    export CLASSPATH

    $E ":: Building for $PLATFORM/$ARCH"
    $E ":: Using temporary directory $TMP"
    $E
    $E -n "- Compiling Java classes... "
    SOURCEFILES="\
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/Check.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/CollatorFactory.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/Messages.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/Platform.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/PlatformVersion.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/FileHelpers.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/GUID.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/TypesafeEnum.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/BitField.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/StringHelpers.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/HashUtils.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/LocaleInvariantStringHelpers.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/NewlineUtils.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/base64/Base64.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/base64/BinaryEncoder.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/base64/BinaryDecoder.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/base64/Encoder.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/base64/Decoder.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/base64/EncoderException.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/base64/DecoderException.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/process/ProcessRunner.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/process/ProcessOutputReader.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/process/ProcessFinishedHandler.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/tasks/CanceledException.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/tasks/TaskMonitor.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/tasks/NullTaskMonitor.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/tasks/TaskMonitorService.java \
        ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/IOUtils.java \
        ../src/com/microsoft/tfs/jni/internal/auth/NativeAuth.java \
        ../src/com/microsoft/tfs/jni/internal/console/NativeConsole.java \
        ../src/com/microsoft/tfs/jni/internal/filesystem/NativeFileSystem.java \
        ../src/com/microsoft/tfs/jni/internal/keychain/NativeKeychain.java \
        ../src/com/microsoft/tfs/jni/internal/negotiate/NativeNegotiate.java \
        ../src/com/microsoft/tfs/jni/internal/negotiate/NegotiateException.java \
        ../src/com/microsoft/tfs/jni/internal/negotiate/UnavailableNegotiate.java \
        ../src/com/microsoft/tfs/jni/internal/ntlm/JavaNTLM.java \
        ../src/com/microsoft/tfs/jni/internal/ntlm/MD4.java \
        ../src/com/microsoft/tfs/jni/internal/ntlm/NativeNTLM.java \
        ../src/com/microsoft/tfs/jni/internal/ntlm/NTLMException.java \
        ../src/com/microsoft/tfs/jni/internal/ntlm/NTLMVersionException.java \
        ../src/com/microsoft/tfs/jni/internal/platformmisc/NativePlatformMisc.java \
        ../src/com/microsoft/tfs/jni/internal/synchronization/NativeSynchronization.java \
        ../src/com/microsoft/tfs/jni/internal/LibraryNames.java \
        ../src/com/microsoft/tfs/jni/loader/NativeLoader.java \
        ../src/com/microsoft/tfs/jni/AuthenticationEngine.java \
        ../src/com/microsoft/tfs/jni/Console.java \
        ../src/com/microsoft/tfs/jni/ConsoleUtils.java \
        ../src/com/microsoft/tfs/jni/FileSystem.java \
        ../src/com/microsoft/tfs/jni/FileSystemAttributes.java \
        ../src/com/microsoft/tfs/jni/FileSystemTime.java \
        ../src/com/microsoft/tfs/jni/FileSystemUtils.java \
        ../src/com/microsoft/tfs/jni/Keychain.java \
        ../src/com/microsoft/tfs/jni/KeychainEnum.java \
        ../src/com/microsoft/tfs/jni/KeychainAuthenticationType.java \
        ../src/com/microsoft/tfs/jni/KeychainInternetPassword.java \
        ../src/com/microsoft/tfs/jni/KeychainProtocol.java \
        ../src/com/microsoft/tfs/jni/KeychainUtils.java \
        ../src/com/microsoft/tfs/jni/Messages.java \
        ../src/com/microsoft/tfs/jni/MessageWindow.java \
        ../src/com/microsoft/tfs/jni/Negotiate.java \
        ../src/com/microsoft/tfs/jni/NegotiateEngine.java \
        ../src/com/microsoft/tfs/jni/NTLM.java \
        ../src/com/microsoft/tfs/jni/NTLMEngine.java \
        ../src/com/microsoft/tfs/jni/PlatformMisc.java \
        ../src/com/microsoft/tfs/jni/PlatformMiscUtils.java \
        ../src/com/microsoft/tfs/jni/RegistryException.java \
        ../src/com/microsoft/tfs/jni/RegistryKey.java \
        ../src/com/microsoft/tfs/jni/RegistryValue.java \
        ../src/com/microsoft/tfs/jni/RootKey.java \
        ../src/com/microsoft/tfs/jni/Synchronization.java \
        ../src/com/microsoft/tfs/jni/SynchronizationUtils.java \
        ../src/com/microsoft/tfs/jni/ValueType.java \
        ../src/com/microsoft/tfs/jni/WellKnownSID.java \
        ../tests/com/microsoft/tfs/jni/internal/console/ExecConsole.java \
        ../tests/com/microsoft/tfs/jni/internal/console/NativeConsoleTest.java \
        ../tests/com/microsoft/tfs/jni/internal/console/UnixExecConsole.java \
        ../tests/com/microsoft/tfs/jni/internal/console/WindowsExecConsole.java \
        ../tests/com/microsoft/tfs/jni/internal/filesystem/ExecFileSystem.java \
        ../tests/com/microsoft/tfs/jni/internal/filesystem/NativeFileSystemTest.java \
        ../tests/com/microsoft/tfs/jni/internal/filesystem/UnixExecFileSystem.java \
        ../tests/com/microsoft/tfs/jni/internal/filesystem/WindowsExecFileSystem.java \
        ../tests/com/microsoft/tfs/jni/internal/keychain/NativeKeychainTest.java \
        ../tests/com/microsoft/tfs/jni/internal/negotiate/NativeNegotiateTest.java \
        ../tests/com/microsoft/tfs/jni/internal/ntlm/NativeNTLMTest.java \
        ../tests/com/microsoft/tfs/jni/internal/platformmisc/ExecPlatformMisc.java \
        ../tests/com/microsoft/tfs/jni/internal/platformmisc/NativePlatformMiscTest.java \
        ../tests/com/microsoft/tfs/jni/internal/platformmisc/UnixExecPlatformMisc.java \
        ../tests/com/microsoft/tfs/jni/internal/platformmisc/WindowsExecPlatformMisc.java \
        ../tests/com/microsoft/tfs/jni/internal/registry/NativeRegistryTest.java \
        ../tests/com/microsoft/tfs/jni/internal/synchronization/NativeSynchronizationTest.java \
        ../tests/com/microsoft/tfs/jni/MessageWindowTest.java \
        ../tests/com/microsoft/tfs/jni/AllNativeTests.java \
        ../tests/com/microsoft/tfs/jni/ExecHelpers.java"
        

    # We require a minimum of Java 1.5
    javac $JAVA_SOURCE_ENCODING_FLAGS -source 1.5 $SOURCEFILES -encoding "UTF-8" -d "$TMP"
    $E " done."

    # Copy message files over so that tests can run
    $E -n "- Copying localized message data... "
    cp ../../com.microsoft.tfs.util/src/com/microsoft/tfs/util/messages.properties "$TMP/com/microsoft/tfs/util"
    cp ../src/com/microsoft/tfs/jni/messages.properties "$TMP/com/microsoft/tfs/jni"
    $E "done."

    $E -n "- Generating C headers... "
    javah -jni -o "$TMP/native_auth.h" com.microsoft.tfs.jni.internal.auth.NativeAuth
    javah -jni -o "$TMP/native_console.h" com.microsoft.tfs.jni.internal.console.NativeConsole
    javah -jni -o "$TMP/native_filesystem.h" com.microsoft.tfs.jni.internal.filesystem.NativeFileSystem
    javah -jni -o "$TMP/native_keychain.h" com.microsoft.tfs.jni.internal.keychain.NativeKeychain
    javah -jni -o "$TMP/native_misc.h" com.microsoft.tfs.jni.internal.platformmisc.NativePlatformMisc
    javah -jni -o "$TMP/native_synchronization.h" com.microsoft.tfs.jni.internal.synchronization.NativeSynchronization
    
    $E "done."

    $E "- Compiling native C library:"
    $E
    CFLAGS="-I$TMP -Icommon -Iunix -DLOGGER_LOG4J"
fi

# Global debug flags.

if [ "$DEBUG" = 1 ]; then
    CFLAGS="$CFLAGS -DDEBUG -g"
fi

case $PLATFORM in
    macosx)
        # Can only build ppc64 on 10.5 machines
        os_minor=`sw_vers -productVersion | sed -e "s/^10\.//" | sed -e "s/\..*//"`

		arch_support="-arch i386 -arch x86_64"

		if [ "$os_minor" = "5" -o "$os_minor" = "6" -o "$os_minor" = "7" ]; then
			arch_support="${arch_support} -arch ppc"
		fi

        if [ "$os_minor" = "5" ]; then
            arch_support="${arch_support} -arch ppc64"
        fi

		JAVA_FRAMEWORK=`readlink /usr/bin/java | sed -e "s/\/Commands\/java$//"`

        CFLAGS="$CFLAGS -fstack-protector -Wstack-protector -D_FORTIFY_SOURCE=2 -I${JAVA_FRAMEWORK}/Headers $arch_support -DMACOS_X -DHAS_STAT_MTIMESPEC"
        LDFLAGS="$LDFLAGS -bundle -framework JavaVM -framework SystemConfiguration -framework CoreServices -mmacosx-version-min=10.5"

        build_library_gcc "$TMP/lib$LIBRARY_CONSOLE.jnilib" "$SOURCES_CONSOLE"
        build_library_gcc "$TMP/lib$LIBRARY_FILESYSTEM.jnilib" "$SOURCES_FILESYSTEM"
        build_library_gcc "$TMP/lib$LIBRARY_MISC.jnilib" "$SOURCES_MISC"
        build_library_gcc "$TMP/lib$LIBRARY_SYNCHRONIZATION.jnilib" "$SOURCES_SYNCHRONIZATION"

        LDFLAGS_BACKUP="$LDFLAGS"
        LDFLAGS="$LDFLAGS -framework Security"
        build_library_gcc "$TMP/lib$LIBRARY_KEYCHAIN.jnilib" "$SOURCES_KEYCHAIN"
        LDFLAGS="$LDFLAGS_BACKUP"

        CFLAGS_BACKUP="$CFLAGS"
        CFLAGS="$CFLAGS -I/usr/include/gssapi -DDYNAMIC_GSSAPI"
        build_library_gcc "$TMP/lib$LIBRARY_AUTH.jnilib" "$SOURCES_AUTH"
        CFLAGS="$CFLAGS_BACKUP"

        ;;
    solaris)
    	# -Wno-unknown-pragmas prevents "warning: ignoring #pragma ident" in /usr/include/gssapi/gssapi.h
        CFLAGS="$CFLAGS -I$JAVA_HOME/include -I$JAVA_HOME/include/solaris"
        CFLAGS="$CFLAGS -O2 -fno-strict-aliasing -fPIC -W -Wall -Wno-unknown-pragmas -Wno-unused"
        CFLAGS="$CFLAGS -Wno-parentheses -DNDEBUG -pthreads -D__solaris__"
        CFLAGS="$CFLAGS -D_REENTRANT -DTRACING -DMACRO_MEMSYS_OPS -DBREAKPTS"
        CFLAGS="$CFLAGS -DHAS_STAT_MTIM"
        CFLAGS="$CFLAGS -static-libgcc"

		# sem_open and friends need -lrt
        LDFLAGS="$LDFLAGS -lrt -shared"

        if [ "$ARCH" = "sparc" ] ; then
            CFLAGS="$CFLAGS -Dsparc"
        elif [ "$ARCH" = "x86_64" ] ; then
            CFLAGS="$CFLAGS -m64 -Damd64 -DcpuIntel -D_LITTLE_ENDIAN"
        else
            CFLAGS="$CFLAGS -m32 -Di386 -Di586 -DcpuIntel -D_LITTLE_ENDIAN"
        fi

        build_library_gcc "$TMP/lib$LIBRARY_CONSOLE.so" "$SOURCES_CONSOLE"
        build_library_gcc "$TMP/lib$LIBRARY_FILESYSTEM.so" "$SOURCES_FILESYSTEM"
        build_library_gcc "$TMP/lib$LIBRARY_MISC.so" "$SOURCES_MISC"
        build_library_gcc "$TMP/lib$LIBRARY_SYNCHRONIZATION.so" "$SOURCES_SYNCHRONIZATION"

        CFLAGS="$CFLAGS -I/usr/include/kerberosv5 -I/usr/include/gssapi -DDYNAMIC_GSSAPI"

        if [ "$ARCH" = "sparc" ]; then
            CFLAGS="$CFLAGS -I/usr/local/include -I/usr/local/include/gssapi"
        fi
        build_library_gcc "$TMP/lib$LIBRARY_AUTH.so" "$SOURCES_AUTH"

        ;;
    hpux)
        if [ "$ARCH" = "PA_RISC" ] ; then
            EXTENSION="sl"
        else
            EXTENSION="so"
        fi

        CFLAGS="$CFLAGS -I$JAVA_HOME/include -I$JAVA_HOME/include/hp-ux"
        CFLAGS="$CFLAGS -O2 -fno-strict-aliasing -fPIC -W -Wall -Wno-unused"
        CFLAGS="$CFLAGS -Wno-parentheses -DNDEBUG"
        CFLAGS="$CFLAGS -D_REENTRANT -DTRACING -DMACRO_MEMSYS_OPS -DBREAKPTS"

        LDFLAGS="$LDFLAGS -static-libgcc -shared -lc"

        if [ "$ARCH" = "PA_RISC" ] ; then
            # Compile for PA-RISC 1.0 to support old chips.
            CFLAGS="$CFLAGS -march=1.0"
        else
            # Itanium with 32-bit JVM.
            CFLAGS="$CFLAGS -milp32"
        fi

        build_library_gcc "$TMP/lib$LIBRARY_CONSOLE.$EXTENSION" "$SOURCES_CONSOLE"
        build_library_gcc "$TMP/lib$LIBRARY_FILESYSTEM.$EXTENSION" "$SOURCES_FILESYSTEM"
        build_library_gcc "$TMP/lib$LIBRARY_MISC.$EXTENSION" "$SOURCES_MISC"
        build_library_gcc "$TMP/lib$LIBRARY_SYNCHRONIZATION.$EXTENSION" "$SOURCES_SYNCHRONIZATION"

        LDFLAGS="$LDFLAGS -lgssapi_krb5 -lkrb5"
        build_library_gcc "$TMP/lib$LIBRARY_AUTH.$EXTENSION" "$SOURCES_AUTH"

        ;;
    linux)
        CFLAGS="$CFLAGS -std=c99 -finline-functions -Winline"
        CFLAGS="$CFLAGS -I$JAVA_HOME/include -I$JAVA_HOME/include/linux"
        CFLAGS="$CFLAGS -O2 -fno-strict-aliasing -fPIC -pthread -W -Wall -Wno-unused"
        CFLAGS="$CFLAGS -Wno-parentheses -DNDEBUG -DLINUX"
        CFLAGS="$CFLAGS -D_LARGEFILE64_SOURCE -D_GNU_SOURCE -D_REENTRANT -DHAS_STAT_MTIM"

        LDFLAGS="$LDFLAGS -Wl,-O1 -Wl,-soname=$TARGET_SHORT -static-libgcc -shared -lc"

        if [ "$ARCH" = "ppc" ] ; then
            CFLAGS="$CFLAGS -DPOWERPC -D_BIG_ENDIAN"
            CFLAGS="$CFLAGS -mno-power -mno-powerpc"
        elif [ "$ARCH" = "x86" ] ; then
            CFLAGS="$CFLAGS -Di586 -DARCH=\"i586\" -D_LITTLE_ENDIAN"
            LDFLAGS="$LDFLAGS -z defs"
        elif [ "$ARCH" = "x86_64" ] ; then
            CFLAGS="$CFLAGS -Damd64 -DARCH=\"amd64\" -D_LP64=1 -D_LITTLE_ENDIAN"
            LDFLAGS="$LDFLAGS -z defs"
        fi

        build_library_gcc "$TMP/lib$LIBRARY_CONSOLE.so" "$SOURCES_CONSOLE"
        build_library_gcc "$TMP/lib$LIBRARY_FILESYSTEM.so" "$SOURCES_FILESYSTEM"
        build_library_gcc "$TMP/lib$LIBRARY_MISC.so" "$SOURCES_MISC"
        build_library_gcc "$TMP/lib$LIBRARY_SYNCHRONIZATION.so" "$SOURCES_SYNCHRONIZATION"

        # Disabled until GNOME keyring threading problems solved.
        #CFLAGS="$CFLAGS `pkg-config gnome-keyring-1 --cflags`"
		#LDFLAGS="$LDFLAGS `pkg-config gnome-keyring-1  --libs`"
        #build_library_gcc "$TMP/lib$LIBRARY_SECURESTORAGE_GNOME_KEYRING.so" "$SOURCES_SECURESTORAGE_GNOME_KEYRING"

        CFLAGS="$CFLAGS -I/usr/include/gssapi -DDYNAMIC_GSSAPI"
        LDFLAGS="$LDFLAGS -ldl"
        build_library_gcc "$TMP/lib$LIBRARY_AUTH.so" "$SOURCES_AUTH"

        ;;
    aix)
        # AIX is a little odd.  Naked shared objects are usually stored as
        # .o files, not .so, but collections of them can reside in regular .a
        # archives.  JVMs generally look for a .a file to load shared
        # objects from, BUT they don't want to find an archive format
        # file, just the naked shared object.  So, we build a shared
        # object directly into a file ending in .a (which isn't an archive).

        CFLAGS="$CFLAGS -I$JAVA_HOME/include"
        CFLAGS="$CFLAGS -O2 -fno-strict-aliasing -fPIC -pthread -W -Wall -Wno-unused"
        CFLAGS="$CFLAGS -Wno-parentheses -DNDEBUG"
        CFLAGS="$CFLAGS -D_LARGEFILE64_SOURCE -D_GNU_SOURCE -D_REENTRANT -DHAS_STAT_MTIME_N"

        # Disable both POWER and PowerPC specific instructions so we run on both.
        CFLAGS="$CFLAGS -mno-power -mno-powerpc"

        LDFLAGS="$LDFLAGS -static-libgcc -shared -lc"

        if [ "$ARCH" = "ppc" ] ; then
            CFLAGS="$CFLAGS -DPOWERPC -D_BIG_ENDIAN"
        fi

        build_library_gcc "$TMP/lib$LIBRARY_CONSOLE.a" "$SOURCES_CONSOLE"
        build_library_gcc "$TMP/lib$LIBRARY_FILESYSTEM.a" "$SOURCES_FILESYSTEM"
        build_library_gcc "$TMP/lib$LIBRARY_MISC.a" "$SOURCES_MISC"
        build_library_gcc "$TMP/lib$LIBRARY_SYNCHRONIZATION.a" "$SOURCES_SYNCHRONIZATION"

        # AIX 5.2 doesn't have com_err's erro_message, enable IBM version
        CFLAGS="$CFLAGS -I/usr/include/gssapi -I/usr/include/ibm_svc -DHAVE_KRB5_SVC_GET_MSG"
        LDFLAGS="$LDFLAGS -L/usr/krb5/lib -lgssapi_krb5 -lkrb5 -lksvc"
        build_library_gcc "$TMP/lib$LIBRARY_AUTH.a" "$SOURCES_AUTH"

        ;;
    freebsd)
        CFLAGS="$CFLAGS -I$JAVA_HOME/include -I$JAVA_HOME/include/freebsd"
        CFLAGS="$CFLAGS -O2 -fno-strict-aliasing -fPIC -pthread -W -Wall -Wno-unused"
        CFLAGS="$CFLAGS -Wno-parentheses -DNDEBUG"
        CFLAGS="$CFLAGS -D_LARGEFILE64_SOURCE -D_GNU_SOURCE -D_REENTRANT"

        LDFLAGS="$LDFLAGS -Wl,-O1 -Wl,-soname=$TARGET_SHORT -static-libgcc -shared -lc"

        if [ "$ARCH" = "x86" ] ; then
            CFLAGS="$CFLAGS -Di586 -DARCH=\"i586\" -D_LITTLE_ENDIAN"
            LDFLAGS="$LDFLAGS -z defs"
        elif [ "$ARCH" = "x86_64" ] ; then
            CFLAGS="$CFLAGS -Damd64 -DARCH=\"amd64\" -D_LP64=1 -D_LITTLE_ENDIAN"
            LDFLAGS="$LDFLAGS -z defs"
        fi

        build_library_gcc "$TMP/lib$LIBRARY_CONSOLE.so" "$SOURCES_CONSOLE"
        build_library_gcc "$TMP/lib$LIBRARY_FILESYSTEM.so" "$SOURCES_FILESYSTEM"
        build_library_gcc "$TMP/lib$LIBRARY_MISC.so" "$SOURCES_MISC"
        build_library_gcc "$TMP/lib$LIBRARY_SYNCHRONIZATION.so" "$SOURCES_SYNCHRONIZATION"

		CFLAGS="$CFLAGS -DDYNAMIC_GSSAPI"
        LDFLAGS="$LDFLAGS -lgssapi -lgssapi_krb5"
        build_library_gcc "$TMP/lib$LIBRARY_AUTH.so" "$SOURCES_AUTH"

        ;;
    zos)
        if [ "$PREVIEW" = 0 ] ; then
            # xlc requires EBCDIC source code.  Convert all the source items into a temp folder.
            EBCDIC_DIR="$TMP/ebcdic-source"
            for SOURCE in unix/*.[ch] common/*.[ch] ; do
                NEWSOURCE=$EBCDIC_DIR/$SOURCE
                mkdir -p `dirname $NEWSOURCE`
                iconv -f ISO8859-1 -t IBM-1047 $SOURCE > $NEWSOURCE
            done
        fi

        # Make sure to include the new EBCDIC paths.
        CFLAGS="-I$EBCDIC_DIR/common -I$EBCDIC_DIR/unix $CFLAGS"

        # xlc language options.  Must use "extended" because jni.h doesn't work with c99.
        CFLAGS="$CFLAGS -D_POSIX_SOURCE -D_XOPEN_SOURCE=500 -I$JAVA_HOME/include -W \"c,langlvl(extended),float(ieee),dll,exportall\""
        LDFLAGS="$LDFLAGS -W l,dll"

        build_library_zos "$TMP/lib$LIBRARY_CONSOLE.so" "$EBCDIC_DIR" "$SOURCES_CONSOLE"
        build_library_zos "$TMP/lib$LIBRARY_FILESYSTEM.so" "$EBCDIC_DIR" "$SOURCES_FILESYSTEM"
        build_library_zos "$TMP/lib$LIBRARY_MISC.so" "$EBCDIC_DIR" "$SOURCES_MISC"
        build_library_zos "$TMP/lib$LIBRARY_SYNCHRONIZATION.so" "$EBCDIC_DIR" "$SOURCES_SYNCHRONIZATION"

        CFLAGS="$CFLAGS -I/usr/include/skrb/"
        LD_EXTRA="/usr/lib/EUVFKDLL.x"
        build_library_zos "$TMP/lib$LIBRARY_AUTH.so" "$EBCDIC_DIR" "$SOURCES_AUTH"

        ;;
    *)
        $E
        $E "ERROR: Somehow this script got all the way down here thinking your"
        $E "ERROR: platform (\"$PLATFORM/$ARCH\") was supported, when it isn't."
        $E "ERROR: Edit $0 to fix this problem."
        exit 3

        ;;
esac

if [ "$PREVIEW" = 0 -a "$DISABLE_ALL_TESTS" = 0 ] ; then
    $E

    $E "- Running tests."
    $E "-"
    $E

    if [ "$PLATFORM" = "hpux" ] ; then
        SHLIB_PATH="$TMP"
        export SHLIB_PATH
    fi

    TEST_PROPS=
    if [ "$DISABLE_INT_TESTS" = 1 ] ; then
    	TEST_PROPS="-Dcom.microsoft.tfs.jni.disable-interactive-tests=1"
 	fi

	VALGRIND_CMD=
	if [ "$VALGRIND" = 1 ] ; then
		# --trace-child is usually required because Java can exec another process when it starts,
		# but unfortunately catches all the "exec" utils too.
		mkdir -p "val"
		VALGRIND_CMD="valgrind --log-file='val/val-%p.log' --smc-check=all --trace-children=yes --leak-check=full"
	fi
	
    # Disable errexit so we can test for unit test failures manually (and save the log regardless)
    set +e
    
    # Java 1.4 on z/OS requires explicit -cp (doesn't check environment).
    CMD="$VALGRIND_CMD java -cp "$CLASSPATH" $TEST_PROPS -Djava.library.path="$TMP" junit.textui.TestRunner com.microsoft.tfs.jni.AllNativeTests"
    echo $CMD
    echo
    eval $CMD
    TEST_EXIT_CODE=$?

    # Re-enable errexit again
    set -e
        
    # Make a copy of the test log file for later analysis (helps with remote builds)
    JUNIT_LOG="/tmp/test-`date +%Y-%m-%d-%H.%M.%S`.log"
    if [ -f "test.log" ] ; then
        cp -f "test.log" $JUNIT_LOG
    fi

    if [ "$TEST_EXIT_CODE" -ne 0 ] ; then 
        echo "- Tests did not pass, JUnit log file copied to $JUNIT_LOG"
        exit 1
    else
        $E "- Tests passed (no failures, no errors)."
    fi
    
fi

# Compute the platform architecture variant on the destination
if [ "X$ARCH" = "Xuniversal" ]; then
    DEST="$DEST/$PLATFORM"
else
    DEST="$DEST/$PLATFORM/$ARCH"
fi

if [ "$PREVIEW" = 0 ] ; then
    $E -n "- Copying deployments to $DEST ... "
    for a in $DEPLOY; do
        $E -n "$a ... "
        mkdir -p "$DEST"
        cp "$TMP/$a" "$DEST"
    done
    
    $E "done."

    $E -n "- Cleaning up... "
    rm -rf "$TMP"
    $E "done."

    $E
    $E ":: Native library built."
else
    for a in $DEPLOY; do
        echo "$DEST/$a"
    done
fi

