// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.util.FileEncoding;

/**
 * {@link AutomergeWritableConflictResolution} handles the special type of
 * conflict resolution for automerging writable conflicts. In particular, we
 * must delete the conflict on the server, then do a get of the file on the
 * requested version. This will produce new version conflicts, which we can
 * automerge.
 *
 * @since TEE-SDK-10.1
 */
public class AutomergeWritableConflictResolution extends ConflictResolution {
    // this list holds unresolved conflicts from our automerge attempt
    private final List<Conflict> conflicts = new ArrayList<Conflict>();

    public AutomergeWritableConflictResolution(
        final ConflictDescription conflictDescription,
        final String description,
        final String helpText) {
        super(conflictDescription, description, helpText, ConflictResolutionOptions.NONE);
    }

    @Override
    public ConflictResolution newForConflictDescription(final ConflictDescription conflictDescription) {
        return new AutomergeWritableConflictResolution(conflictDescription, getDescription(), getHelpText());
    }

    @Override
    public void setNewPath(final String newPath) {
        throw new TECoreException(
            //@formatter:off
            Messages.getString("AutomergeWritableConflictResolution.AutomergeWritableConflictResolutionCannotAcceptNewPath")); //$NON-NLS-1$
            //@formatter:on
    }

    @Override
    public void setEncoding(final FileEncoding newEncoding) {
        throw new TECoreException(
            //@formatter:off
            Messages.getString("AutomergeWritableConflictResolution.AutomergeWritableConflictResolutionCannotAcceptNewEncoding")); //$NON-NLS-1$
            //@formatter:on
    }

    @Override
    protected ConflictResolutionStatus work() throws Exception {
        final Workspace workspace = getConflictDescription().getWorkspace();
        final Conflict conflict = getConflictDescription().getConflict();

        // determine the local path - do not rely on
        // conflictdescription.getlocalpath()
        // as that is for ui display purposes, not to pass to the server
        final String localPath =
            (conflict.getTargetLocalItem() != null) ? conflict.getTargetLocalItem() : conflict.getSourceLocalItem();

        // setup an itemspec
        final ItemSpec itemSpec = new ItemSpec(localPath, RecursionType.NONE);
        final VersionSpec versionSpec = new ChangesetVersionSpec(conflict.getTheirVersion());

        // first resolve the conflict on the server
        conflict.setResolution(Resolution.DELETE_CONFLICT);
        workspace.resolveConflict(conflict);

        if (conflict.isResolved() == false) {
            throw new Exception(
                Messages.getString("AutomergeWritableConflictResolution.CouldNotResolveWritableConflict")); //$NON-NLS-1$
        }

        // try to pend an edit for this file
        if (workspace.pendEdit(new ItemSpec[] {
            itemSpec
        }, LockLevel.UNCHANGED, null, GetOptions.NONE, PendChangesOptions.NONE) < 1) {
            throw new Exception(
                MessageFormat.format(
                    Messages.getString("AutomergeWritableConflictResolution.CouldNotPendEditFormat"), //$NON-NLS-1$
                    LocalPath.getFileName(localPath)));
        }

        // now do a get to the conflict version, which should produce new
        // (version) conflicts
        final GetStatus getStatus = workspace.get(new GetRequest(itemSpec, versionSpec), GetOptions.NONE);

        if (getStatus == null || getStatus.isCanceled()) {
            throw new Exception(
                Messages.getString("AutomergeWritableConflictResolution.CouldNotUpdateLocalVersionForAutomerge")); //$NON-NLS-1$
        } else if (getStatus.getNumFailures() > 0) {
            throw new Exception(getStatus.getFailures()[0].getMessage());
        }

        // if there are no conflicts here (unlikely), we're done
        if (getStatus.getNumConflicts() == 0) {
            return ConflictResolutionStatus.SUCCESS;
        }

        final Conflict[] newConflicts = workspace.queryConflicts(new String[] {
            localPath
        }, false);

        // try to resolve each conflict by automerging
        boolean hasUnresolvedConflicts = false;
        for (int i = 0; i < newConflicts.length; i++) {
            newConflicts[i].setResolution(Resolution.ACCEPT_MERGE);
            workspace.resolveConflict(newConflicts[i]);

            // if the conflict could not be automerged, mark this for new
            // conflict action
            if (newConflicts[i].isResolved() == false) {
                // add this to the list of unresolved conflicts
                conflicts.add(newConflicts[i]);
                hasUnresolvedConflicts = true;
            }
        }

        if (hasUnresolvedConflicts) {
            return ConflictResolutionStatus.SUCCEEDED_WITH_CONFLICTS;
        } else {
            return ConflictResolutionStatus.SUCCESS;
        }
    }

    @Override
    public Conflict[] getConflicts() {
        return conflicts.toArray(new Conflict[conflicts.size()]);
    }
}
