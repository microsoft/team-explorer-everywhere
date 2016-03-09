// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.conflict;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;

public interface ConflictResolveErrorHandler {
    void conflictResolveError(Conflict conflict, Exception exception);
}
