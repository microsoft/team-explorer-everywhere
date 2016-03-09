// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import java.util.ArrayList;
import java.util.Collection;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * This is the base conflict description class. It presents textual descriptions
 * of conflicts, primarily for UI. This class is abstract, to get a
 * {@link ConflictDescription}, you should call
 * {@link ConflictDescription#getConflictDescription(ConflictCategory, Conflict)}
 * .
 *
 * @since TEE-SDK-10.1
 */
public abstract class ConflictDescription {
    private final Workspace workspace;
    private final Conflict conflict;
    private final ItemSpec[] conflictItemSpecs;

    /**
     * Internal constructor for creating a conflict description.
     *
     * @param conflict
     *        the Conflict to wrap
     */
    protected ConflictDescription(
        final Workspace workspace,
        final Conflict conflict,
        final ItemSpec[] conflictItemSpecs) {
        this.workspace = workspace;
        this.conflict = conflict;
        this.conflictItemSpecs = conflictItemSpecs;
    }

    /**
     * Get the workspace this conflict is occurring in.
     *
     * @return The Workspace this conflict occurs in.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * Get the underlying core conflict for this conflict.
     *
     * @return The original Conflict for this conflict
     */
    public Conflict getConflict() {
        return conflict;
    }

    /**
     * The item specs queried to return this conflict. These may be requeried.
     *
     * @return An array of ItemSpecs that were queried to return this conflict.
     *         May be <code>null</code> or empty.
     */
    public ItemSpec[] getConflictItemSpecs() {
        return conflictItemSpecs;
    }

    /**
     * Gets the category of this conflict
     *
     * @return A ConflictCategory for this conflict
     */
    abstract public ConflictCategory getConflictCategory();

    /**
     * Gets the local filename affected by the conflict. Returns the target
     * local file, if defined, or the source local file.
     *
     * @return Local filename or null
     */
    public String getLocalPath() {
        if (conflict.getTargetLocalItem() != null) {
            return conflict.getTargetLocalItem();
        } else if (conflict.getSourceLocalItem() != null) {
            return conflict.getSourceLocalItem();
        }

        return null;
    }

    /**
     * Gets the local filename affected by the conflict. By default this will
     * return the target local file (if not <code>null</code>), then the source
     * local file. This may be overridden with the pathType argument.
     *
     * @param pathType
     *        May be {@link ConflictDescriptionPathType#SOURCE} to prefer the
     *        source local item (if not <code>null</code>)
     * @return The local filename affected by this conflict or <code>null</code>
     */
    public String getLocalPath(final ConflictDescriptionPathType pathType) {
        String[] pathOrder;

        if (ConflictDescriptionPathType.SOURCE == pathType) {
            pathOrder = new String[] {
                conflict.getSourceLocalItem(),
                conflict.getTargetLocalItem()
            };
        } else {
            pathOrder = new String[] {
                conflict.getTargetLocalItem(),
                conflict.getSourceLocalItem()
            };
        }

        for (int i = 0; i < pathOrder.length; i++) {
            if (pathOrder[i] != null) {
                return pathOrder[i];
            }
        }

        return null;
    }

    /**
     * Gets the server filename affected by this conflict. Defaults to the your
     * server file, unless it does not exist, then tries base server file and
     * finally their server file.
     *
     * @return The server filename affected by this conflict.
     */
    public String getServerPath() {
        if (conflict.getYourServerItem() != null) {
            return conflict.getYourServerItem();
        } else if (conflict.getBaseServerItem() != null) {
            return conflict.getBaseServerItem();
        } else if (conflict.getTheirServerItem() != null) {
            return conflict.getTheirServerItem();
        }

        return null;
    }

    /**
     * Gets the description of the local file. For most conflicts, this is
     * generally called the "local" file.
     *
     * @return A String representing the name of the server file
     */
    public String getLocalFileDescription() {
        return Messages.getString("ConflictDescription.LocalFileDescription"); //$NON-NLS-1$
    }

    /**
     * Gets the description of the server's file. For most conflicts, this is
     * generally called the "server" file.
     *
     * @return A String representing the name of the server file
     */
    public String getRemoteFileDescription() {
        return Messages.getString("ConflictDescription.ServerFileDescription"); //$NON-NLS-1$
    }

    public boolean isBaseless() {
        return conflict.isBaseless();
    }

    /**
     * A short description of the conflict, eg "Version Conflict"
     *
     * @return A String representing the short description of the conflict
     */
    abstract public String getName();

    /**
     * A long description of this conflict, suitable for display to the user. eg
     * ("You have a conflicting pending change.")
     *
     * @return A string representing the description of the conflict
     */
    abstract public String getDescription();

    /**
     * The UI should attempt to show a change description (number of
     * local/server/conflicting changes.)
     *
     * @return true to show change description, false otherwise
     */
    public boolean showChangeDescription() {
        return false;
    }

    /**
     * Returns the "change description" - number of local/server/conflicting
     * changes.
     */
    public String getChangeDescription() {
        return Messages.getString("ConflictDescription.ChangeDescription"); //$NON-NLS-1$
    }

    /**
     * Analyze the conflict for mergeability, enablement, etc.
     *
     * @return true if the conflict was analyzed, false if no changes were made
     */
    public boolean analyzeConflict() {
        return false;
    }

    /**
     * Returns whether this conflict has been analyzed for mergeability.
     *
     * @return
     */
    public boolean hasAnalyzed() {
        return false;
    }

    /**
     * Clears any data leftover from the analysis of this conflict.
     */
    public void clearAnalysis() {
    }

    /**
     * Gets the resolution options for this particular conflict with the
     * available merge toolset. If the toolset is not <code>null</code> and
     * contains a matching tool, external resolution options may be available.
     *
     * @param resolutionContributor
     *        an object which contributes resolutions for conflicts. May be
     *        <code>null</code>, but external resolution options won't be
     *        available.
     * @return An array of {@link ConflictResolution}s for this particular
     *         conflict.
     */
    public abstract ConflictResolution[] getResolutions(final ConflictResolutionContributor resolutionContributor);

    /**
     * Determines if a conflict resolution should be enabled (shown to the
     * user.) We may know ahead of time that certain conflict resolutions (for
     * example, AcceptMerge) are invalid (due to analyzing the conflict.)
     *
     * @param resolution
     *        The resolution to examine
     * @return true if the resolution is enabled, false otherwise
     */
    public boolean isResolutionEnabled(final ConflictResolution resolution) {
        return true;
    }

    /**
     * Utility method for subclasses: loads contributed conflict resolution
     * options from the resolution contributor.
     *
     * @param resolutionContributor
     *        The {@link ConflictResolutionContributor} for this product (may be
     *        <code>null</code>)
     * @param resolutionOptions
     *        The {@link ResolutionOptions} for this conflict description (not
     *        <code>null</code>)
     * @return A collection of contributed conflict resolutions (never
     *         <code>null</code>)
     */
    protected final Collection<ConflictResolution> loadContributedResolutions(
        final ConflictResolutionContributor resolutionContributor,
        final ConflictResolutionOptions resolutionOptions) {
        final Collection<ConflictResolution> contributions =
            resolutionContributor.getConflictResolutions(this, resolutionOptions);

        if (contributions != null) {
            return contributions;
        }

        return new ArrayList<ConflictResolution>();
    }
}
