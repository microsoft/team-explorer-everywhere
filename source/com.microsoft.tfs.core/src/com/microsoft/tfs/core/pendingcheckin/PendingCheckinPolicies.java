// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;

/**
 * <p>
 * Evaluates the contents of the containing {@link PendingCheckin}.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface PendingCheckinPolicies {
    /**
     * Evaluates checkin policies.
     *
     * @return any failures detected.
     * @param policyContext
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     * @throws PolicyEvaluationCancelledException
     *         if the user canceled the policy evaluation.
     */
    public PolicyFailure[] evaluate(PolicyContext policyContext) throws PolicyEvaluationCancelledException;

    /**
     * @return the state of a policy evaluator.
     */
    public PolicyEvaluatorState getPolicyEvaluatorState();
}
