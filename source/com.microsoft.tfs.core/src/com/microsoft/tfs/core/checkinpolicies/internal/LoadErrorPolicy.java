// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.internal;

import com.microsoft.tfs.core.checkinpolicies.PolicyBase;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyType;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A stand-in policy implementation for one which failed to load correctly.
 * These failures are modeled as real implementations so they can produce
 * failures that can be sent to TFS.
 * </p>
 * <p>
 * This class is internal because only {@link PolicyEvaluator} needs to create
 * these.
 * </p>
 *
 * @threadsafety thread-safe
 */
public class LoadErrorPolicy extends PolicyBase {
    private final String failureMessage;
    private final PolicyType failedPolicyType;

    /**
     * This implementation does not require a zero-argument constructor because
     * it is not constructed via reflection. It is only used during error cases.
     */
    public LoadErrorPolicy(final String failureMessage, final PolicyType failedPolicyType) {
        super();

        Check.notNull(failureMessage, "failureMessage"); //$NON-NLS-1$

        this.failureMessage = failureMessage;
        this.failedPolicyType = failedPolicyType;
    }

    @Override
    public boolean canEdit() {
        return false;
    }

    @Override
    public boolean edit(final PolicyEditArgs policyEditArgs) {
        return false;
    }

    @Override
    public PolicyFailure[] evaluate(final PolicyContext context) throws PolicyEvaluationCancelledException {
        return new PolicyFailure[] {
            new PolicyFailure(failureMessage, this)
        };
    }

    @Override
    public PolicyType getPolicyType() {
        return failedPolicyType;
    }

    @Override
    public void loadConfiguration(final Memento configurationMemento) {
    }

    @Override
    public void saveConfiguration(final Memento configurationMemento) {
    }
}
