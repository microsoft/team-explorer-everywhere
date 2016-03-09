// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.console.NativeConsoleTest;
import com.microsoft.tfs.jni.internal.filesystem.NativeFileSystemTest;
import com.microsoft.tfs.jni.internal.keychain.NativeKeychainTest;
import com.microsoft.tfs.jni.internal.negotiate.NativeNegotiateTest;
import com.microsoft.tfs.jni.internal.ntlm.NativeNTLMTest;
import com.microsoft.tfs.jni.internal.platformmisc.NativePlatformMiscTest;
import com.microsoft.tfs.jni.internal.registry.NativeRegistryTest;
import com.microsoft.tfs.jni.internal.synchronization.NativeSynchronizationTest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * A suite that includes all the native tests so we can run them easily from the
 * command line as part of the JNI build process.
 *
 * @threadsafety unknown
 */
public class AllNativeTests extends TestCase {
    /**
     * If this system property is set, interactive unit tests should do no work
     * (they will succeed but not capture the UI).
     */
    private static final String DISABLE_INTERACTIVE_TESTS_PROPERTY = "com.microsoft.tfs.jni.disable-interactive-tests"; //$NON-NLS-1$

    public static boolean interactiveTestsDisabled() {
        return System.getProperty(AllNativeTests.DISABLE_INTERACTIVE_TESTS_PROPERTY) != null;
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTestSuite(NativeConsoleTest.class);
        suite.addTestSuite(NativeFileSystemTest.class);
        suite.addTestSuite(NativeKeychainTest.class);
        suite.addTestSuite(NativeNegotiateTest.class);
        suite.addTestSuite(NativeNTLMTest.class);
        suite.addTestSuite(NativePlatformMiscTest.class);
        suite.addTestSuite(NativeRegistryTest.class);
        suite.addTestSuite(NativeSynchronizationTest.class);
        suite.addTestSuite(MessageWindowTest.class);
        return suite;
    }
}
