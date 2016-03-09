// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.events;

import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Event fired when the state of a {@link PolicyEvaluator} object has changed,
 * usually because of a new team project source or selected items in the pending
 * checkin have changed. This is not the event thrown when a
 * {@link PolicyInstance} re-evaluates (see {@link PolicyStateChangedEvent}).
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
public final class PolicyEvaluatorStateChangedEvent extends CoreClientEvent {
    private final PolicyEvaluator evaluator;

    /**
     * Describes the policy state change.
     *
     * @param source
     *        the source of the change (must not be <code>null</code>)
     * @param evaluator
     *        the {@link PolicyEvaluator} that changed (must not be
     *        <code>null</code>)
     */
    public PolicyEvaluatorStateChangedEvent(final EventSource source, final PolicyEvaluator evaluator) {
        super(source);

        Check.notNull(evaluator, "evaluator"); //$NON-NLS-1$

        this.evaluator = evaluator;
    }

    /**
     * @return the evaluator that changed.
     */
    public PolicyEvaluator getEvaluator() {
        return evaluator;
    }
}
