// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Information for conflicting changes.
 *
 * @since TEE-SDK-10.1
 */
public class ConflictEvent extends CoreClientEvent {
    static final long serialVersionUID = 5900213734846004035L;
    private final String serverItem;
    private final Workspace workspace;
    private final String message;
    private final boolean resolvable;

    public ConflictEvent(
        final EventSource source,
        final String serverItem,
        final Workspace workspace,
        final String message,
        final boolean resolvable) {
        super(source);

        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(serverItem, "serverItem"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(message, "message"); //$NON-NLS-1$

        this.serverItem = serverItem;
        this.workspace = workspace;
        this.message = message;
        this.resolvable = resolvable;
    }

    public String getServerItem() {
        return serverItem;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getMessage() {
        return message;
    }

    public boolean isResolvable() {
        return resolvable;
    }
}
