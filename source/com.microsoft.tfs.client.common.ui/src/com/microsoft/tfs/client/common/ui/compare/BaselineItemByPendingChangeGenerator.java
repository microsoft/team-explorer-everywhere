// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.compare;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.client.common.ui.framework.compare.DifferencerInputGenerator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Generates a {@link TFSPendingChangeBaselineNode} for a {@link PendingChange}.
 * This generator is useful for getting the baseline file content for a pending
 * change on this computer without contacting the server (if it's a local
 * workspace).
 * <p>
 * This class can be used with both local and server workspaces, but it will
 * contact the server in the latter case.
 */
public class BaselineItemByPendingChangeGenerator implements DifferencerInputGenerator {
    private final Workspace workspace;
    private final PendingChange pendingChange;

    public BaselineItemByPendingChangeGenerator(final Workspace workspace, final PendingChange pendingChange) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(pendingChange, "pendingChange"); //$NON-NLS-1$

        this.workspace = workspace;
        this.pendingChange = pendingChange;
    }

    @Override
    public String getLoggingDescription() {
        String path = pendingChange.getServerItem();
        if (path == null) {
            path = pendingChange.getLocalItem();
        }

        return MessageFormat.format(
            "{0} (workspace version)", //$NON-NLS-1$
            path);
    }

    @Override
    public Object getInput(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        return new TFSPendingChangeBaselineNode(workspace, pendingChange);
    }
}
