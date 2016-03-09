// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public class WorkingFolderDataCollection {
    private final List workingFolders = new ArrayList();

    public WorkingFolderDataCollection() {

    }

    public WorkingFolderDataCollection(final Workspace workspace) {
        this(workspace.getFolders());
    }

    public WorkingFolderDataCollection(final WorkingFolder[] workingFolders) {
        Check.notNull(workingFolders, "workingFolders"); //$NON-NLS-1$

        for (int i = 0; i < workingFolders.length; i++) {
            add(new WorkingFolderData(workingFolders[i]));
        }
    }

    public WorkingFolder[] createWorkingFolders() {
        final WorkingFolder[] result = new WorkingFolder[workingFolders.size()];
        int ix = 0;
        for (final Iterator it = workingFolders.iterator(); it.hasNext();) {
            final WorkingFolderData workingFolderData = (WorkingFolderData) it.next();
            result[ix++] = workingFolderData.createWorkingFolder();
        }
        return result;
    }

    public WorkingFolderDataCollection(final WorkingFolderData[] workingFolderData) {
        Check.notNull(workingFolderData, "workingFolderData"); //$NON-NLS-1$

        workingFolders.addAll(Arrays.asList(workingFolderData));
    }

    public WorkingFolderData[] getWorkingFolderData() {
        return (WorkingFolderData[]) workingFolders.toArray(new WorkingFolderData[workingFolders.size()]);
    }

    public void add(final WorkingFolderData workingFolderData) {
        Check.notNull(workingFolderData, "workingFolderData"); //$NON-NLS-1$

        workingFolders.add(workingFolderData);
    }

    public void remove(final WorkingFolderData workingFolderData) {
        Check.notNull(workingFolderData, "workingFolderData"); //$NON-NLS-1$

        workingFolders.remove(workingFolderData);
    }
}
