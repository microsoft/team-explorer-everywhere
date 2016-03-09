// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.shutdown;

import java.util.EventListener;

/**
 * <p>
 * {@link ShutdownEventListener}s are registered with the
 * {@link ShutdownManager} and invoked when parts or all of core is being shut
 * down (as part of a manual operation or during process exit).
 * <p>
 * {@link #onShutdown()} may be invoked multiple times during an object's
 * lifetime (see method notes below), and may be invoked by any running thread.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface ShutdownEventListener extends EventListener {
    /**
     * <p>
     * Signals the listener that the JVM is shutting down. May be invoked
     * multiple times during a listener's lifetime if the method implementation
     * does not remove the listener from the shutdown manager.
     * </p>
     * <p>
     * This method may be invoked by any running thread, so the implementer must
     * perform its own data synchronization.
     * </p>
     */
    public void onShutdown();
}
