// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.events;

import java.util.EventListener;

import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;

/**
 * <p>
 * Defines an interface for listeners of the {@link PolicyStateChangedEvent}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface PolicyStateChangedListener extends EventListener {
    /**
     * Invoked when the state of a checkin policy object (
     * {@link PolicyInstance}) has changed.
     *
     * @param e
     *        the event that describes the changes.
     */
    public void onPolicyStateChanged(PolicyStateChangedEvent e);
}
