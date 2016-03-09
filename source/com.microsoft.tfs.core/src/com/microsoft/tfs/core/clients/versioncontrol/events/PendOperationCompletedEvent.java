// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when a pend operation has completed.
 *
 * @since TEE-SDK-10.1
 */
public class PendOperationCompletedEvent extends OperationCompletedEvent {
    private final ChangeRequest[] requests;

    public PendOperationCompletedEvent(
        final EventSource source,
        final Workspace workspace,
        final ChangeRequest[] requests) {
        super(source, workspace, ProcessType.PEND);

        Check.notNull(requests, "requests"); //$NON-NLS-1$

        this.requests = requests;
    }

    /**
     * @return the request objects that were pended (but not always
     *         successfully).
     */
    public ChangeRequest[] getRequests() {
        return requests;
    }
}
