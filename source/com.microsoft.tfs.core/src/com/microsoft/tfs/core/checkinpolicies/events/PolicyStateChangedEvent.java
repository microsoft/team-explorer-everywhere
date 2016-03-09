// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.events;

import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Event fired when the state of a {@link PolicyInstance} object has changed
 * because of initialization or evaluation.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class PolicyStateChangedEvent extends CoreClientEvent {
    private final PolicyFailure[] failures;
    private final PolicyInstance policy;

    /**
     * Describes the policy state change.
     *
     * @param source
     *        the source of the change (must not be <code>null</code>)
     * @param failures
     *        the failures that accompany the change (may be null).
     * @param policy
     *        the policy that changed (must not be <code>null</code>)
     */
    public PolicyStateChangedEvent(
        final EventSource source,
        final PolicyFailure[] failures,
        final PolicyInstance policy) {
        super(source);

        Check.notNull(policy, "policy"); //$NON-NLS-1$

        this.failures = failures;
        this.policy = policy;
    }

    /**
     * @return the set of failures that now exist, after this change. An emtpy
     *         array signifies no failures.
     */
    public PolicyFailure[] getFailures() {
        if (failures == null) {
            return new PolicyFailure[0];
        }

        /*
         * We currently do not clone the objects to prevent subclasses of
         * PolicyFailure from having to implement Cloneable correctly (because
         * if they don't, we may get an exception here).
         */
        return failures;
    }

    /**
     * @return the policy that changed.
     */
    public PolicyInstance getPolicy() {
        return policy;
    }
}
