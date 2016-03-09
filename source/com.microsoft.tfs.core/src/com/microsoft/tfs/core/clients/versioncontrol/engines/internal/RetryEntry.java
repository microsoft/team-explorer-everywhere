// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 */

public class RetryEntry {
    private final OperationStatus status;
    private final GetOperation retryOp;
    private final GetOperation targetAction;

    public RetryEntry(final OperationStatus status, final GetOperation retryOp, final GetOperation targetAction) {
        this.status = status;
        this.retryOp = retryOp;
        this.targetAction = targetAction;
    }

    public GetOperation getRetryOp() {
        return this.retryOp;
    }

    public OperationStatus getStatus() {
        return this.status;
    }

    public GetOperation getTargetAction() {
        return this.targetAction;
    }
}