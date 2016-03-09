// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Standard implementation of {@link PendingCheckin}.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class StandardPendingCheckin implements PendingCheckin {
    private final PendingCheckinNotes notes;
    private final PendingCheckinPolicies policies;
    private final PendingCheckinPendingChanges pendingChanges;
    private final PendingCheckinWorkItems workItems;

    /**
     * Creates a {@link StandardPendingCheckin} using the user-supplied
     * evaluator and the loader it was already configured with.
     */
    public StandardPendingCheckin(
        final Workspace workspace,
        final PendingChange[] allChanges,
        final PendingChange[] checkedChanges,
        final String comment,
        final CheckinNote checkinNotes,
        final WorkItemCheckinInfo[] checkedWorkItems,
        final PolicyEvaluator evaluator) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(allChanges, "allChanges"); //$NON-NLS-1$
        Check.notNull(checkedChanges, "checkedChanges"); //$NON-NLS-1$
        Check.notNull(comment, "comment"); //$NON-NLS-1$
        Check.notNull(checkinNotes, "checkinNotes"); //$NON-NLS-1$
        Check.notNull(checkedWorkItems, "checkedWorkItems"); //$NON-NLS-1$

        /*
         * Create an instance of the affected team projects container, and share
         * it between the implementations of our other pending checkin classes.
         * The implementations will use this "live" container object so changes
         * in one place affect how all the implementations work.
         */
        final AffectedTeamProjects affectedTeamProjects = new AffectedTeamProjects(checkedChanges);

        notes = new StandardPendingCheckinNotes(checkinNotes, workspace.getClient(), affectedTeamProjects);

        pendingChanges = new StandardPendingCheckinPendingChanges(
            workspace,
            allChanges,
            checkedChanges,
            affectedTeamProjects,
            comment);

        workItems = new StandardPendingCheckinWorkItems(checkedWorkItems);

        /*
         * Policies must be set after pending changes and work items, because
         * policies needs to query these for its pending changes contents during
         * construction.
         */
        policies = new StandardPendingCheckinPolicies(this, workspace.getClient(), evaluator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingCheckinNotes getCheckinNotes() {
        return notes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingCheckinPolicies getCheckinPolicies() {
        return policies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingCheckinPendingChanges getPendingChanges() {
        return pendingChanges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingCheckinWorkItems getWorkItems() {
        return workItems;
    }
}
