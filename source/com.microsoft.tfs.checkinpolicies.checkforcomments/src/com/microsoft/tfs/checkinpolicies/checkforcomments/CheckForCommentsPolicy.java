// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.checkforcomments;

import com.microsoft.tfs.core.checkinpolicies.PolicyBase;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyType;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;

/**
 * A TFS Check-in Policy that ensures a non-empty comment is supplied with a
 * check-in.
 * <p>
 * This policy does not require a GUI/CLC duality, because there is no graphical
 * configuration or evaluation.
 */
public class CheckForCommentsPolicy extends PolicyBase {
    private final static PolicyType TYPE =
        new PolicyType(
            "com.teamprise.checkinpolicies.checkforcomments.CheckForCommentsPolicy-1", //$NON-NLS-1$

            Messages.getString("CheckForCommentsPolicy.Name"), //$NON-NLS-1$

            Messages.getString("CheckForCommentsPolicy.ShortDescription"), //$NON-NLS-1$

            Messages.getString("CheckForCommentsPolicy.LongDescription"), //$NON-NLS-1$

            Messages.getString("CheckForCommentsPolicy.InstallInstructions")); //$NON-NLS-1$

    /**
     * All policy implementations must include a zero-argument constructor, so
     * they can be dynamically created by the policy framework.
     */
    public CheckForCommentsPolicy() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyBase#canEdit()
     */
    @Override
    public boolean canEdit() {
        // We offer no user-configurable settings.
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#edit(com.microsoft.
     * tfs.core .checkinpolicies.PolicyEditArgs)
     */
    @Override
    public boolean edit(final PolicyEditArgs policyEditArgs) {
        return false;
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
        final PendingCheckin pc = getPendingCheckin();
        final String comment = pc.getPendingChanges().getComment();

        if (pc.getPendingChanges().getCheckedPendingChanges().length > 0) {
            if (comment == null || comment.length() == 0) {
                return new PolicyFailure[] {
                    new PolicyFailure(Messages.getString("CheckForCommentsPolicy.PolicyFailureText"), this) //$NON-NLS-1$
                };
            }
        }

        return new PolicyFailure[0];
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyBase#getPolicyType()
     */
    @Override
    public PolicyType getPolicyType() {
        /*
         * This class statically defines a type which is always appropriate.
         */
        return CheckForCommentsPolicy.TYPE;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#loadConfiguration(com
     * .microsoft.tfs.core.memento.Memento)
     */
    @Override
    public void loadConfiguration(final Memento configurationMemento) {
        // Nothing to load.
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#saveConfiguration(com
     * .microsoft.tfs.core.memento.Memento)
     */
    @Override
    public void saveConfiguration(final Memento configurationMemento) {
        // Nothing to save.
    }
}
