// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.ConflictResolvedHandler;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.util.Check;

public class LocalWorkspaceResolvedConflictHandler implements ConflictResolvedHandler {
    private final ConflictResolvedHandler parentHandler;

    private final List<GetOperation> allOperations = new ArrayList<GetOperation>();
    private ChangePendedFlags flags = ChangePendedFlags.UNKNOWN;

    public LocalWorkspaceResolvedConflictHandler(final ConflictResolvedHandler parentHandler) {
        Check.notNull(parentHandler, "parentHandler"); //$NON-NLS-1$

        this.parentHandler = parentHandler;
    }

    @Override
    public void conflictResolved(
        final Conflict conflict,
        final GetOperation[] getOps,
        final GetOperation[] undoOps,
        final Conflict[] resolvedConflicts,
        final ChangePendedFlags flags) {
        for (final GetOperation getOp : getOps) {
            allOperations.add(getOp);
        }

        for (final GetOperation undoOp : undoOps) {
            allOperations.add(undoOp);
        }

        this.flags = this.flags.combine(flags);

        parentHandler.conflictResolved(conflict, getOps, undoOps, resolvedConflicts, flags);
    }

    public GetOperation[] getAllOperations() {
        return allOperations.toArray(new GetOperation[allOperations.size()]);
    }

    public ChangePendedFlags getFlags() {
        return flags;
    }
}
