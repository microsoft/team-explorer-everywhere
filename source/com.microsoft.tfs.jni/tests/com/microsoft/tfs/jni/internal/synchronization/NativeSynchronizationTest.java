// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.synchronization;

import java.io.File;
import java.text.MessageFormat;

import junit.framework.TestCase;

public class NativeSynchronizationTest extends TestCase {
    private NativeSynchronization nativeImpl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.nativeImpl = new NativeSynchronization();
    }

    public void testSemaphore() throws Exception {
        // TODO figure out why this doesn't work on Unix and re-enable the test
        if (true) {
            return;
        }

        /*
         * On some Unix systems the semaphore appears on the filesystem, and it
         * follows POSIX filesystem semantics on all Unixes, so pick a path that
         * will be writable.
         */
        final File semFile = File.createTempFile("semtest", ".tmp"); //$NON-NLS-1$//$NON-NLS-2$
        final String semaphoreName = semFile.getAbsolutePath();
        semFile.delete();

        final long semaphoreId = nativeImpl.createSemaphore(semaphoreName, 1);

        if (semaphoreId < 0) {
            throw new Exception(MessageFormat.format("Could not create semaphore {0}, got null", semaphoreName)); //$NON-NLS-1$
        }

        /* Acquisition should succeed. */
        assertEquals("Could not acquire semaphore " + semaphoreName, nativeImpl.waitForSemaphore(semaphoreId, -1), 1); //$NON-NLS-1$

        /* Already acquired - acquisition should fail (would block) */
        assertEquals(
            "Accidentally acquired semaphore " + semaphoreName, //$NON-NLS-1$
            nativeImpl.waitForSemaphore(semaphoreId, 0),
            0);

        /* Release */
        assertEquals("Could not release semaphore " + semaphoreName, nativeImpl.releaseSemaphore(semaphoreId), true); //$NON-NLS-1$

        /* Acquisition should succeed. */
        assertEquals("Could not acquire semaphore " + semaphoreName, nativeImpl.waitForSemaphore(semaphoreId, 0), 1); //$NON-NLS-1$

        /* Already acquired - acquisition should fail (would block) */
        assertEquals(
            "Accidentally acquired semaphore " + semaphoreName, //$NON-NLS-1$
            nativeImpl.waitForSemaphore(semaphoreId, 0),
            0);

        /* Release */
        assertEquals("Could not release semaphore " + semaphoreName, nativeImpl.releaseSemaphore(semaphoreId), true); //$NON-NLS-1$
    }

}
