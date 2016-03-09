// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.internal;

import java.util.Comparator;

import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationStatus;

/**
 * <p>
 * Compares {@link PolicyEvaluationStatus} objects using their internal priority
 * field. If these are equal, the secondary sort is undefined (though it's
 * actually the ID string).
 * </p>
 *
 * @threadsafety thread-safe
 */
public class PolicyEvaluationStatusComparator implements Comparator<PolicyEvaluationStatus> {
    /**
     * Creates a {@link PolicyEvaluationStatusComparator}.
     */
    public PolicyEvaluationStatusComparator() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final PolicyEvaluationStatus status1, final PolicyEvaluationStatus status2) {
        if (status1.getPriority() == status2.getPriority()) {
            /*
             * Use the ID as the secondary, undocumented, sort criteron.
             */
            return status1.getPolicyType().getID().compareTo(status2.getPolicyType().getID());
        }

        // Subtract 2 from 1 because we invert priority values.
        return status1.getPriority() - status2.getPriority();
    }
}
