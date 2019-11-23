// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.tests.synchronization;

import com.microsoft.tfs.jni.internal.synchronization.NativeSynchronization;
import com.microsoft.tfs.util.Platform;
import junit.framework.TestCase;

import java.io.File;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NativeSynchronizationTest extends TestCase {
    private NativeSynchronization nativeImpl;
    private ExecutorService executorService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.nativeImpl = new NativeSynchronization();
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override protected void tearDown() throws Exception {
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(1, TimeUnit.MINUTES));

        super.tearDown();
    }

    public void testMutex() throws Exception {
        if (!Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return; // currently the underlying implementation is Windows-only
        }

        String mutexName = UUID.randomUUID().toString();

        final long mutexId = nativeImpl.createMutex(mutexName);

        if (mutexId < 0) {
            throw new Exception(MessageFormat.format("Could not create mutex {0}, got null", mutexName)); //$NON-NLS-1$
        }

        /* Acquisition should succeed. */
        assertEquals("Could not acquire mutex " + mutexName, 1, nativeImpl.waitForMutex(mutexId, -1)); //$NON-NLS-1$

        /* Already acquired - acquisition should fail (would block) */
        assertEquals(
            "Accidentally acquired mutex " + mutexName, //$NON-NLS-1$
            0,
            (int) executorService.submit(() -> nativeImpl.waitForMutex(mutexId, 0)).get());

        /* Release */
        assertEquals("Could not release mutex " + mutexName, true, nativeImpl.releaseMutex(mutexId)); //$NON-NLS-1$

        /* Acquisition should succeed. */
        assertEquals("Could not acquire mutex " + mutexName, 1, nativeImpl.waitForMutex(mutexId, 0)); //$NON-NLS-1$

        /* Already acquired - acquisition should fail (would block) */
        assertEquals(
            "Accidentally acquired mutex " + mutexName, //$NON-NLS-1$
            0,
            (int) executorService.submit(() -> nativeImpl.waitForMutex(mutexId, 0)).get());

        /* Release */
        assertEquals("Could not release mutex " + mutexName, true, nativeImpl.releaseMutex(mutexId)); //$NON-NLS-1$
    }

}
