// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.events;

import java.util.EventListener;

/**
 * <p>
 * Defines an interface for listeners of the {@link PolicyLoadErrorEvent}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface PolicyLoadErrorListener extends EventListener {
    /**
     * Invoked when a policy instance failed to load.
     *
     * @param e
     *        the event that describes the changes.
     */
    public void onPolicyLoadError(PolicyLoadErrorEvent e);
}
