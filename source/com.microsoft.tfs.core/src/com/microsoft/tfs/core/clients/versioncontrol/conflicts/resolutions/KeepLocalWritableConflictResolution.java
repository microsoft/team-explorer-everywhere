// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueue;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.util.FileEncoding;

public class KeepLocalWritableConflictResolution extends CoreConflictResolution {
    public KeepLocalWritableConflictResolution(
        final ConflictDescription conflictDescription,
        final String description,
        final String helpText) {
        super(conflictDescription, description, helpText, ConflictResolutionOptions.NONE, Resolution.DELETE_CONFLICT);
    }

    @Override
    public ConflictResolution newForConflictDescription(final ConflictDescription conflictDescription) {
        return new KeepLocalWritableConflictResolution(conflictDescription, getDescription(), getHelpText());
    }

    @Override
    public void setNewPath(final String newPath) {
        throw new TECoreException(Messages.getString("KeepLocalWritableConflictResolution.CannotAcceptNewPath")); //$NON-NLS-1$
    }

    @Override
    public void setEncoding(final FileEncoding newEncoding) {
        throw new TECoreException(Messages.getString("KeepLocalWritableConflictResolution.CannotAcceptNewEncoding")); //$NON-NLS-1$
    }

    @Override
    public ConflictResolutionStatus work() {
        final ConflictResolutionStatus status = super.work();

        if (!ConflictResolutionStatus.SUCCESS.equals(status)) {
            return status;
        }

        final ConflictDescription conflictDescription = getConflictDescription();
        final Workspace workspace = conflictDescription.getWorkspace();

        /* Process the resolved conflicts */
        final UpdateLocalVersionQueue ulvq = new UpdateLocalVersionQueue(workspace);

        try {
            ulvq.queueUpdate(
                conflictDescription.getConflict().getTheirServerItem(),
                conflictDescription.getConflict().getTheirItemID(),
                conflictDescription.getConflict().getTargetLocalItem(),
                conflictDescription.getConflict().getTheirVersion(),
                null);
        } finally {
            ulvq.close();
        }

        if (workspace.pendEdit(new String[] {
            conflictDescription.getConflict().getTheirServerItem()
        }, RecursionType.NONE, LockLevel.UNCHANGED, null, GetOptions.NONE, PendChangesOptions.NONE) != 1) {
            return ConflictResolutionStatus.FAILED;
        }

        return ConflictResolutionStatus.SUCCESS;
    }
}
