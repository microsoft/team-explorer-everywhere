// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

public class InternalWorkspaceConflictInfo {
    public static final InternalWorkspaceConflictInfo[] EMPTY_ARRAY = new InternalWorkspaceConflictInfo[0];

    private final WorkspaceInfo primaryWorkspace;
    private final WorkspaceInfo secondaryWorkspace;
    private final String conflictingPath;

    public InternalWorkspaceConflictInfo(
        final WorkspaceInfo primaryWorkspace,
        final WorkspaceInfo secondaryWorkspace,
        final String conflictingPath) {
        this.primaryWorkspace = primaryWorkspace;
        this.secondaryWorkspace = secondaryWorkspace;
        this.conflictingPath = conflictingPath;
    }

    /**
     * @return Workspace that will be preserved in the cache
     */
    public WorkspaceInfo getPrimaryWorkspace() {
        return primaryWorkspace;
    }

    /**
     * @return Workspace that will be removed from the cache
     */
    public WorkspaceInfo getSecondaryWorkspace() {
        return secondaryWorkspace;
    }

    /**
     * @return Local path that is causing the conflict
     */
    public String getConflictingPath() {
        return this.conflictingPath;
    }

    /**
     * @return a warning string for the current locale with info about the
     *         conflict
     */
    public String getWarningUI() {
        return MessageFormat.format(
            Messages.getString("InternalWorkspaceConflictInfo.ConflictingWorkspaceInTheCacheFormat"), //$NON-NLS-1$
            conflictingPath,
            primaryWorkspace.getDisplayName(),
            primaryWorkspace.getServerURI(),
            secondaryWorkspace.getDisplayName(),
            secondaryWorkspace.getServerURI());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof InternalWorkspaceConflictInfo == false) {
            return false;
        }

        final InternalWorkspaceConflictInfo other = (InternalWorkspaceConflictInfo) obj;

        return primaryWorkspace.equals(other.primaryWorkspace)
            && secondaryWorkspace.equals(other.secondaryWorkspace)
            && LocalPath.equals(conflictingPath, other.conflictingPath);
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + (primaryWorkspace.hashCode());
        result = result * 37 + (secondaryWorkspace.hashCode());
        result = result * 37 + (LocalPath.hashCode(conflictingPath));

        return result;
    }
}