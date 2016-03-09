// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when a pend operation has started.
 *
 * @since TEE-SDK-10.1
 */
public class PendOperationStartedEvent extends OperationStartedEvent {
    private final ChangeRequest[] requests;

    public PendOperationStartedEvent(
        final EventSource source,
        final Workspace workspace,
        final ChangeRequest[] requests) {
        super(source, workspace, ProcessType.PEND);

        Check.notNull(requests, "requests"); //$NON-NLS-1$

        this.requests = requests;
    }

    /**
     * @return the request objects that were requested to be pended (but have
     *         not been tried yet).
     */
    public ChangeRequest[] getRequests() {
        return requests;
    }
}
