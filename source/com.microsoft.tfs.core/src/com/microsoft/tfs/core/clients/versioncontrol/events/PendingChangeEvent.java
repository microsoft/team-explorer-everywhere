// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Information for many kinds of events fired from pending changes. The
 * {@link OperationStatus} field denotes the type of event this class describes.
 *
 * @since TEE-SDK-10.1
 */
public class PendingChangeEvent extends CoreClientEvent {
    static final long serialVersionUID = 1424346462712443465L;
    private final Workspace workspace;
    private final PendingChange change;
    private final OperationStatus operation;
    private final ChangePendedFlags flags;

    public PendingChangeEvent(
        final EventSource source,
        final Workspace workspace,
        final PendingChange change,
        final OperationStatus operation,
        final ChangePendedFlags flags) {
        super(source);

        this.workspace = workspace;
        this.change = change;
        this.operation = operation;
        this.flags = flags;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public PendingChange getPendingChange() {
        return change;
    }

    public OperationStatus getOperationStatus() {
        return operation;
    }

    public ChangePendedFlags getFlags() {
        return flags;
    }
}
