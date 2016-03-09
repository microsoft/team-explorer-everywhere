// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class ShelvesetConflictDescription extends VersionConflictDescription {
    protected ShelvesetConflictDescription(
        final Workspace workspace,
        final Conflict conflict,
        final ItemSpec[] conflictItemSpecs) {
        super(workspace, conflict, conflictItemSpecs);
    }

    @Override
    public ConflictCategory getConflictCategory() {
        return ConflictCategory.SHELVESET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return Messages.getString("ShelvesetConflictDescription.Name"); //$NON-NLS-1$
    }

    @Override
    public String getRemoteFileDescription() {
        return Messages.getString("ShelvesetConflictDescription.ServerFileDescription"); //$NON-NLS-1$
    }
}
