// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

public class ReconcileBlockedByProjectRenameException extends VersionControlException {
    private static final long serialVersionUID = 5479844393617668713L;

    private static final String OLD_PROJECT_NAMES_PROPERTY = "Microsoft.TeamFoundation.VersionControl.OldProjectNames"; //$NON-NLS-1$
    private static final String NEW_PROJECT_NAMES_PROPERTY = "Microsoft.TeamFoundation.VersionControl.NewProjectNames"; //$NON-NLS-1$
    private static final String NEW_PROJECT_REVISION_ID_PROPERTY =
        "Microsoft.TeamFoundation.VersionControl.NewProjectRevisionId"; //$NON-NLS-1$

    private String[] oldProjectNames;
    private String[] newProjectNames;

    public ReconcileBlockedByProjectRenameException(final String message) {
        super(message);
    }

    public ReconcileBlockedByProjectRenameException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * The set of old project names.
     */
    public String[] getOldProjectNames() {
        if (null == oldProjectNames) {
            oldProjectNames = getProperties().getStringArrayProperty(OLD_PROJECT_NAMES_PROPERTY);
        }

        return oldProjectNames;
    }

    /**
     * The set of new project names.
     */
    public String[] getNewProjectNames() {
        if (null == newProjectNames) {
            newProjectNames = getProperties().getStringArrayProperty(NEW_PROJECT_NAMES_PROPERTY);
        }

        return newProjectNames;
    }

    /**
     * The new project revision ID.
     */
    public int getNewProjectRevisionId() {
        return getProperties().getIntProperty(NEW_PROJECT_REVISION_ID_PROPERTY);
    }
}
