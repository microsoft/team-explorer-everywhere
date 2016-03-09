// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.conflict;

import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;

public interface ConflictResolvedHandler {
    void conflictResolved(
        Conflict conflict,
        GetOperation[] getOps,
        GetOperation[] undoOps,
        Conflict[] resolvedConflicts,
        ChangePendedFlags flags);
}
