// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when some kinds of core operations, which may fire other events
 * in the course if its execution, have completed. Every core operation that
 * fires a {@link OperationCompletedEvent} will have fired exactly one
 * {@link OperationStartedEvent} before it.
 * <p>
 * The primary use of this started/completed pattern is to "batch" up events
 * fired in between, so UI elements can be updated more efficiently (e.g. during
 * an "undo" operation).
 * <p>
 * This class is abstract and is designed to be extended to contain specific
 * operation data (see {@link UndoOperationCompletedEvent},
 * {@link GetOperationCompletedEvent}, {@link PendOperationCompletedEvent},
 * etc.).
 *
 * @since TEE-SDK-10.1
 */
public abstract class OperationCompletedEvent extends CoreClientEvent {
    private final Workspace workspace;
    private final ProcessType type;

    public OperationCompletedEvent(final EventSource source, final Workspace workspace, final ProcessType type) {
        super(source);

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$

        this.workspace = workspace;
        this.type = type;
    }

    /**
     * @return the workspace where this operation completed.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * @return the type of processing that fired this event.
     */
    public ProcessType getProcessType() {
        return type;
    }
}
