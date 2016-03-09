// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class BranchHierarchyEditorInput implements IEditorInput {

    private final String item;

    public BranchHierarchyEditorInput(final String item) {
        this.item = item;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return Messages.getString("BranchHierarchyEditorInput.ToolTipText"); //$NON-NLS-1$
    }

    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

    public String getItem() {
        return item;
    }

    @Override
    public String getName() {
        return ServerPath.getFileName(item);
    }

}
