// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.events;

import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Event fired when a policy implementation failed to load. These events are
 * fired before the {@link PolicyEvaluatorStateChangedEvent} is fired.
 * </p>
 * <p>
 * Because events are fired outside of synchronization in
 * {@link PolicyEvaluator}, the {@link PolicyEvaluator}'s state may not still be
 * {@link PolicyEvaluatorState#POLICIES_LOAD_ERROR} when a listener queries it.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class PolicyLoadErrorEvent extends CoreClientEvent {
    private final PolicyEvaluator policyEvaluator;
    private final Throwable error;

    /**
     * Creates a {@link PolicyLoadErrorEvent}.
     *
     * @param source
     *        the source of the change (must not be <code>null</code>)
     * @param policyEvaluator
     *        the evaluator where the error occurred (must not be
     *        <code>null</code>)
     * @param error
     *        the error that caused the failure (must not be <code>null</code>)
     */
    public PolicyLoadErrorEvent(
        final EventSource source,
        final PolicyEvaluator policyEvaluator,
        final Throwable error) {
        super(source);

        Check.notNull(policyEvaluator, "policyEvaluator"); //$NON-NLS-1$
        Check.notNull(error, "error"); //$NON-NLS-1$

        this.policyEvaluator = policyEvaluator;
        this.error = error;
    }

    /**
     * @return the {@link PolicyEvaluator} that encountered the error.
     */
    public PolicyEvaluator getPolicyLoader() {
        return policyEvaluator;
    }

    /**
     * @return the error that caused the failure.
     */
    public Throwable getError() {
        return error;
    }

}
