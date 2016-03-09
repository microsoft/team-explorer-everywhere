// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse.share;

import org.eclipse.core.resources.IProject;

import com.microsoft.tfs.util.Check;

/**
 * Configuration for the share project wizard. Includes project -> server path
 * mappings.
 */
public final class ShareProjectConfiguration {
    private final IProject project;
    private final ShareProjectAction action;
    private final String serverPath;

    /**
     * Configuration for the share project wizard. Includes project -> server
     * path mappings.
     *
     * @param project
     *        The IProject to connect
     * @param serverPath
     *        The server path to map and add to (may be null to indicate that
     *        the project already has a working folder mapping)
     */
    public ShareProjectConfiguration(final IProject project, final ShareProjectAction action, final String serverPath) {
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(action, "action"); //$NON-NLS-1$
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        this.project = project;
        this.action = action;
        this.serverPath = serverPath;
    }

    public IProject getProject() {
        return project;
    }

    public ShareProjectAction getAction() {
        return action;
    }

    public String getServerPath() {
        return serverPath;
    }
}