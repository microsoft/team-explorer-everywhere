// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal;

import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.util.Check;

/**
 * Represents a problem encountered during a get operation which may be able to
 * be resolved if the get is retried.
 */
public final class GetWarning {
    private final GetOperation operation;
    private final OperationStatus status;

    public GetWarning(final GetOperation operation, final OperationStatus status) {
        super();

        Check.notNull(operation, "operation"); //$NON-NLS-1$
        Check.notNull(status, "status"); //$NON-NLS-1$

        this.operation = operation;
        this.status = status;
    }

    public GetOperation getOperation() {
        return operation;
    }

    public OperationStatus getStatus() {
        return status;
    }

}
