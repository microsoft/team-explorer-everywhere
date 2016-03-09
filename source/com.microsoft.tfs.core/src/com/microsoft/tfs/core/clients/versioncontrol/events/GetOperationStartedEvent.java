// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when a get operation has started.
 *
 * @since TEE-SDK-10.1
 */
public class GetOperationStartedEvent extends OperationStartedEvent {
    private final GetRequest[] requests;

    public GetOperationStartedEvent(final EventSource source, final Workspace workspace, final GetRequest[] requests) {
        super(source, workspace, ProcessType.GET);

        Check.notNull(requests, "requests"); //$NON-NLS-1$

        this.requests = requests;
    }

    /**
     * @return the request objects that initiated this get operation.
     */
    public GetRequest[] getRequests() {
        return requests;
    }
}
