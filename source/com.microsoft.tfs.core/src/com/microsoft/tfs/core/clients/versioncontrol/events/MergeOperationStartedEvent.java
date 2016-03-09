// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Event fired when a merge operation has started. This class contains no useful
 * data, it only exists to signal that an operation has begun.
 *
 * @since TEE-SDK-10.1
 */
public class MergeOperationStartedEvent extends OperationStartedEvent {
    public MergeOperationStartedEvent(final EventSource source, final Workspace workspace) {
        super(source, workspace, ProcessType.MERGE);
    }
}
