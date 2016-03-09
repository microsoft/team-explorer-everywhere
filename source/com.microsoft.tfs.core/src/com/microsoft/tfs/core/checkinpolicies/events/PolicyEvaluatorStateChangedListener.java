// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.events;

import java.util.EventListener;

import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;

/**
 * <p>
 * Defines an interface for listeners of the
 * {@link PolicyEvaluatorStateChangedEvent}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface PolicyEvaluatorStateChangedListener extends EventListener {
    /**
     * Invoked when the state of a ({@link PolicyEvaluator}) has changed.
     *
     * @param e
     *        the event that describes the changes.
     */
    public void onPolicyEvaluatorStateChanged(PolicyEvaluatorStateChangedEvent e);
}
