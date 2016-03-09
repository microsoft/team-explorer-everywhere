// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.workitempolicy;

import com.microsoft.tfs.core.checkinpolicies.PolicyBase;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyType;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;

public class WorkItemPolicy extends PolicyBase {
    private final static PolicyType TYPE =
        new PolicyType(
            "com.teamprise.checkinpolicies.workitempolicy.WorkItemPolicy-1", //$NON-NLS-1$

            Messages.getString("WorkItemPolicy.Name"), //$NON-NLS-1$

            Messages.getString("WorkItemPolicy.ShortDescription"), //$NON-NLS-1$

            Messages.getString("WorkItemPolicy.LongDescription"), //$NON-NLS-1$

            Messages.getString("WorkItemPolicy.InstallInstructions")); //$NON-NLS-1$

    @Override
    public boolean canEdit() {
        return false;
    }

    @Override
    public boolean edit(final PolicyEditArgs policyEditArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PolicyType getPolicyType() {
        return TYPE;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#evaluate(com.microsoft
     * .tfs.core.checkinpolicies.PolicyContext)
     */
    @Override
    public PolicyFailure[] evaluate(final PolicyContext context) throws PolicyEvaluationCancelledException {
        final PendingCheckin pendingCheckin = getPendingCheckin();
        if (pendingCheckin.getPendingChanges().getCheckedPendingChanges().length > 0) {
            if (pendingCheckin.getWorkItems().getCheckedWorkItems().length == 0) {
                return new PolicyFailure[] {
                    new PolicyFailure(Messages.getString("WorkItemPolicy.PolicyFailureText"), this) //$NON-NLS-1$
                };
            }
        }
        return new PolicyFailure[0];
    }

    @Override
    public void loadConfiguration(final Memento configurationMemento) {
    }

    @Override
    public void saveConfiguration(final Memento configurationMemento) {
    }
}
