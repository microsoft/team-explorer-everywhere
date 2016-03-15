#!/bin/sh
#
# This script is executed via SSH from Team Build to build native libraries
# on Unix machines.  It basically just runs "build.sh" once to gather the
# expected output file, then again to do the build, then copies the results
# somewhere Team Build can find them.
#
# Also standard error is redirected to standard output, so we don't fail 
# the build if javah/javac/gcc want to write warnings.  Exit codes are the
# important failure detection mechanism. 

SCRIPT_DIR=`dirname $0`
BUILD_PROG=$SCRIPT_DIR/build.sh

# The MSBuild script looks for output files in this directory so it can check
# them in.  The structure should be the same as the "os" directory in the JNI
# project.
#
# This "os" dir is not the one inside the com.microsoft.tfs.jni project.
OUTPUT_DIR=$SCRIPT_DIR/../../os

# Configure environments on each machine.

case `hostname` in
    tfsxp-ubuntu606-x86)
    	export JAVA_HOME=/opt/jdk1.5.0_09
    	export PATH=$JAVA_HOME/bin:$PATH
        ;;
    tfsxp-ubuntu606-x64)
    	export JAVA_HOME=/opt/jdk1.5.0_09
        export PATH=$JAVA_HOME/bin:$PATH
        ;;
   	tfsxpmacppc)
   		export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Home
        ;;
    tfsxp-solaris10-x86)
    	JAVA_HOME=/usr/jdk/jdk1.5.0_20
    	export JAVA_HOME
    	PATH=$JAVA_HOME/bin:/usr/bin:/usr/ucb:/etc:/usr/sfw/bin:$PATH
    	export PATH
    	;;
    tfsxp-solaris10-x64)
    	JAVA_HOME=/usr/jdk/jdk1.5.0_20
    	export JAVA_HOME
    	# Notice the "bin/amd64" to find the 64-bit Java bins!
    	PATH=$JAVA_HOME/bin/amd64:/usr/bin:/usr/ucb:/etc:/usr/sfw/bin:$PATH
    	export PATH
    	;;
    tfsxp-ubuntu-606-ppc)
    	export JAVA_HOME=/usr/ibm-java2-ppc-50
        export PATH=$JAVA_HOME/bin:$PATH
    	;;
    blade2000)
    	JAVA_HOME=/usr/jdk/jdk1.5.0_08
    	export JAVA_HOME
		PATH=$JAVA_HOME/bin:$PATH
		export PATH
    	;;
    rs6000)
    	JAVA_HOME=/usr/java5
    	export JAVA_HOME
		PATH=$JAVA_HOME/bin:$PATH
		export PATH
    	;;
    zx6000)
    	JAVA_HOME=/opt/java1.5
    	export JAVA_HOME
    	PATH=$JAVA_HOME/bin:/opt/hp-gcc-4.2.4/bin:/usr/local/bin:$PATH
    	export PATH
    	;;
    hpux)
    	JAVA_HOME=/opt/java1.5
    	export JAVA_HOME
    	PATH=$JAVA_HOME/bin:/usr/local/bin:$PATH
    	export PATH
    	;;
    *)
        echo "@@ Skipping environment configuration for unknown hostname `hostname`, build might fail!"
        ;;
esac

echo "@@ Running $BUILD_PROG with output $OUTPUT_DIR"

set +e
sh "$BUILD_PROG" -noInteractiveTests "$OUTPUT_DIR" 2>&1
RET=$?
set -e

echo "@@ $BUILD_PROG returned $RET"
exit $RET
