// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoader;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Standard implementation of {@link PendingCheckinPolicies} that uses a
 * {@link PolicyEvaluator} to contain and evaluate policies. A constructor
 * exists to allow you to specify an existing {@link PolicyEvaluator}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class StandardPendingCheckinPolicies implements PendingCheckinPolicies {
    private final PolicyEvaluator evaluator;

    /**
     * Constructs a @ StandardPendingCheckinPolicies} for the given pending
     * checkin and client, using an existing {@link PolicyEvaluator} and the
     * {@link PolicyLoader} it was configured with. The evalutor's
     * {@link PolicyEvaluator#setPendingCheckin(PendingCheckin)} method is
     * called immediately to configure the checkin, then other methods are
     * called later by this object.
     * <p>
     * This construction option is available so clients can manage a single
     * evaluator object that will be used for all evaluation tasks, simplifying
     * event management and increasing the efficiency of policy instance
     * caching.
     *
     * @param pendingCheckin
     *        the pending checkin (must not be <code>null</code>)
     * @param client
     *        the version control client in use (must not be <code>null</code>)
     * @param evaluator
     *        the existing policy evaluator object to use (may be
     *        <code>null</code>)
     */
    public StandardPendingCheckinPolicies(
        final PendingCheckin pendingCheckin,
        final VersionControlClient client,
        final PolicyEvaluator evaluator) {
        Check.notNull(pendingCheckin, "pendingCheckin"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        this.evaluator = evaluator;

        if (this.evaluator != null) {
            this.evaluator.setPendingCheckin(pendingCheckin);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicyFailure[] evaluate(final PolicyContext policyContext) throws PolicyEvaluationCancelledException {
        Check.notNull(policyContext, "policyContext"); //$NON-NLS-1$

        if (evaluator == null) {
            return new PolicyFailure[0];
        }

        return evaluator.evaluate(policyContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PolicyEvaluatorState getPolicyEvaluatorState() {
        if (evaluator == null) {
            return PolicyEvaluatorState.POLICIES_LOAD_ERROR;
        }

        return evaluator.getPolicyEvaluatorState();
    }
}
