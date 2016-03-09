// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients;

import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A base class for all TFS core client events. Provides access to an
 * {@link EventSource} object that provides event source context.
 * </p>
 *
 * @since TEE-SDK-11.0
 * @threadsafety immutable
 */
public abstract class CoreClientEvent {
    /*
     * This class does not extend {@link EventObject} because that class does
     * not use a final field to store its source and performs no other
     * synchronization, so it is not thread-safe. Thread-safety is often
     * important in events, so this class is immutable.
     */

    private final EventSource source;

    /**
     * Creates an event object with the given {@link EventSource}.
     *
     * @param source
     *        an {@link EventSource} object that describes the environment at
     *        the time this event was fired (must not be <code>null</code>)
     */
    public CoreClientEvent(final EventSource source) {
        Check.notNull(source, "source"); //$NON-NLS-1$

        this.source = source;
    }

    /**
     * @return the {@link EventSource} object that describes the environment at
     *         the time this event was fired.
     */
    public EventSource getEventSource() {
        return source;
    }
}
