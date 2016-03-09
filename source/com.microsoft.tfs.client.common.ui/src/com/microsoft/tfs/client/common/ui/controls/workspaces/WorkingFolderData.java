// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.util.Check;

public class WorkingFolderData {
    private String serverItem;
    private String localItem;
    private WorkingFolderType type;

    public WorkingFolderData(final WorkingFolder workingFolder) {
        Check.notNull(workingFolder, "workingFolder"); //$NON-NLS-1$

        if (workingFolder.getDepth() == RecursionType.ONE_LEVEL) {
            serverItem = ServerPath.combine(workingFolder.getServerItem(), WorkingFolder.DEPTH_ONE_STRING);
        } else {
            serverItem = workingFolder.getServerItem();
        }

        localItem = workingFolder.getLocalItem();
        type = workingFolder.getType();
        if (type == null) {
            type = WorkingFolderType.MAP;
        }
    }

    public WorkingFolderData(final String serverItem, final String localItem, final WorkingFolderType type) {
        this.serverItem = serverItem;
        this.localItem = localItem;
        this.type = type;
    }

    @Override
    public String toString() {
        return (type == null ? "[no type set]" : type.toString()) //$NON-NLS-1$
            + " " //$NON-NLS-1$
            + (serverItem == null ? "[no server item set]" : "[" + serverItem + "]") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + " => " //$NON-NLS-1$
            + (localItem == null ? "[no local item set]" : "[" + localItem + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public WorkingFolder createWorkingFolder() {
        if (serverItem == null || serverItem.trim().length() == 0) {
            throw new IllegalStateException("serverItem is null or empty"); //$NON-NLS-1$
        }

        if (type == null) {
            throw new IllegalStateException("type is null"); //$NON-NLS-1$
        }

        if (WorkingFolderType.CLOAK != type && (localItem == null || localItem.trim().length() == 0)) {
            throw new IllegalStateException("localItem is null or empty and working folder is not a cloak"); //$NON-NLS-1$
        }

        String workingFolderServerItem;
        RecursionType recursionType;

        if (WorkingFolderType.CLOAK != type
            && WorkingFolder.DEPTH_ONE_STRING.equals(ServerPath.getFileName(serverItem))) {
            workingFolderServerItem = ServerPath.getParent(serverItem);
            recursionType = RecursionType.ONE_LEVEL;
        } else {
            workingFolderServerItem = serverItem;
            recursionType = RecursionType.FULL;
        }

        return new WorkingFolder(workingFolderServerItem, LocalPath.canonicalize(localItem), type, recursionType);
    }

    public String getLocalItem() {
        return localItem;
    }

    public void setLocalItem(final String localItem) {
        this.localItem = localItem;
    }

    public String getServerItem() {
        return serverItem;
    }

    public void setServerItem(final String serverItem) {
        this.serverItem = serverItem;
    }

    public WorkingFolderType getType() {
        return type;
    }

    public void setType(final WorkingFolderType type) {
        this.type = type;
    }

    public boolean isCloak() {
        return WorkingFolderType.CLOAK == type;
    }
}
