// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class ConflictDescriptionFactory {

    /**
     * Get a ConflictDescription for this particular Conflict.
     *
     * @param workspace
     *        the Workspace that this conflict belongs to
     * @param conflict
     *        the Conflict in question
     * @return a ConflictDescription for this conflict, or null if it is an
     *         unknown conflict type.
     */
    public static final ConflictDescription getConflictDescription(
        final Workspace workspace,
        final Conflict conflict,
        final ItemSpec[] conflictItemSpecs) {
        return getConflictDescription(
            ConflictCategory.getConflictCategory(conflict),
            workspace,
            conflict,
            conflictItemSpecs);
    }

    /**
     * Get a ConflictDescription which describes the type of conflict, but does
     * not hold a particular Conflict. This is used occasionally in the UI when
     * we need to determine text / resolution options for the category of
     * conflicts of this type, not a specific conflict. This should be used
     * carefully, as it can't be resolved.
     *
     *
     * @param category
     *        The ConflictCategory to return a description for
     * @return A dummy ConflictDescription describing the general type of this
     *         conflict. This ConflictDescription MAY NOT be resolved.
     */
    /*
     * TODO: This interface needs improvement. It's not clear that this is
     * really a dummy ConflictDescription which can't be resolved, etc.
     */
    public static final ConflictDescription getConflictDescription(final ConflictCategory category) {
        return getConflictDescription(category, null, null, null);
    }

    /**
     * Get a ConflictDescription which describes the type of conflict.
     *
     * @param category
     *        The ConflictCategory to return a description for
     * @param conflict
     *        The core Conflict
     * @return A ConflictDescription describing the general type of this
     *         conflict.
     */
    public static final ConflictDescription getConflictDescription(
        final ConflictCategory category,
        final Conflict conflict) {
        return getConflictDescription(category, null, conflict, null);
    }

    /**
     * Internal worker to build a ConflictDescription.
     *
     * @param category
     *        The ConflictCategory for this conflict
     * @param workspace
     *        The workspace this conflict belongs to
     * @param conflict
     *        The core Conflict
     * @param conflictItemSpecs
     * @return A ConflictDescription describing this conflict, or null if it is
     *         an unknown conflict type.
     */
    private static final ConflictDescription getConflictDescription(
        final ConflictCategory category,
        final Workspace workspace,
        final Conflict conflict,
        final ItemSpec[] conflictItemSpecs) {
        if (category == ConflictCategory.VERSION) {
            return new VersionConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.MERGE) {
            return new MergeConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.LOCALLY_DELETED) {
            return new LocallyDeletedConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.MERGE_SOURCE_DELETED) {
            return new MergeSourceDeletedConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.SERVER_DELETED) {
            return new ServerDeletedConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.MERGE_TARGET_DELETED) {
            return new MergeTargetDeletedConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.BOTH_DELETED) {
            return new BothDeletedConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.MERGE_BOTH_DELETED) {
            return new MergeBothDeletedConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.FILENAME) {
            return new FilenameConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.WRITABLE) {
            return new WritableConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.SHELVESET) {
            return new ShelvesetConflictDescription(workspace, conflict, conflictItemSpecs);
        } else if (category == ConflictCategory.ROLLBACK_LOCAL) {
            return new RollbackLocalConflictDescription(workspace, conflict, conflictItemSpecs);
        }

        return new UnknownConflictDescription(workspace, conflict, conflictItemSpecs);
    }

    /**
     * Convenience method to get multiple conflict descriptions for multiple
     * conflicts.
     *
     * @param workspace
     *        The workspace this conflict belongs to
     * @param conflicts
     *        The core Conflicts
     * @param conflictItemSpecs
     *        The itemspecs that were queried for this conflict (these will be
     *        requeried later by the UI). may be <code>null</code>
     * @return A list of ConflictDescription describing these conflict,
     *         respectively
     */
    public final static ConflictDescription[] getConflictDescriptions(
        final Workspace workspace,
        final Conflict[] conflicts,
        final ItemSpec[] conflictItemSpecs) {
        final ConflictDescription[] conflictDescriptions = new ConflictDescription[conflicts.length];

        for (int i = 0; i < conflicts.length; i++) {
            conflictDescriptions[i] = getConflictDescription(workspace, conflicts[i], conflictItemSpecs);
        }

        return conflictDescriptions;
    }
}
