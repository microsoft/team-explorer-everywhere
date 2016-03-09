// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import java.util.EventObject;

import com.microsoft.tfs.util.Check;

public class WorkingFolderDataEditEvent extends EventObject {
    private final WorkingFolderData workingFolder;
    private final boolean isNew;
    private final String columnPropertyName;

    public WorkingFolderDataEditEvent(
        final WorkingFolderDataTable control,
        final WorkingFolderData workingFolder,
        final boolean isNew,
        final String columnPropertyName) {
        super(control);

        Check.notNull(workingFolder, "workingFolder"); //$NON-NLS-1$
        Check.notNull(columnPropertyName, "columnPropertyName"); //$NON-NLS-1$

        this.workingFolder = workingFolder;
        this.isNew = isNew;
        this.columnPropertyName = columnPropertyName;
    }

    public WorkingFolderDataTable getControl() {
        return (WorkingFolderDataTable) getSource();
    }

    public WorkingFolderData getWorkingFolder() {
        return workingFolder;
    }

    public boolean isNew() {
        return isNew;
    }

    public String getColumnPropertyName() {
        return columnPropertyName;
    }
}
