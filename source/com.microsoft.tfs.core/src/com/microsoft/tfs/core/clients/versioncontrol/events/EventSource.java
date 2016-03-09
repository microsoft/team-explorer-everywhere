// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * An {@link EventSource} instance accompanies each event fired by
 * {@link VersionControlEventEngine}, and can be used by the code that handles
 * the event to know things about where the event originated (originating
 * thread, etc.).
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class EventSource {
    private final Thread originatingThread;

    /**
     * Creates an {@link EventSource} describing the events and state that
     * preceded the event's happening. Each property of the event can be defined
     * by the caller.
     * <p>
     * If you want an {@link EventSource} whose originating thread is the
     * current thread, simply use {@link #newFromHere()}.
     *
     * @see EventSource#newFromHere()
     *
     * @param originatingThread
     *        the thread that originally made the call to the method that is
     *        documented to fire the event that this source describes (not
     *        null).
     */
    public EventSource(final Thread originatingThread) {
        Check.notNull(originatingThread, "originatingThread"); //$NON-NLS-1$

        this.originatingThread = originatingThread;
    }

    /**
     * Creates a new {@link EventSource} describing an event source using the
     * current thread and any other information gathered from the current
     * context of execution. Code in core that fires events from the same thread
     * that the caller called into core from can use this method as a shortcut
     * for constructing {@link EventSource} objects.
     *
     * @return a new {@link EventSource} initialized from the current execution
     *         context (thread, etc.).
     */
    public static EventSource newFromHere() {
        return new EventSource(Thread.currentThread());
    }

    /**
     * Gets the originating thread.
     *
     * @return the thread that originally made the call to the method that is
     *         documented to fire the event that this source describes.
     */
    public Thread getOriginatingThread() {
        return originatingThread;
    }
}
