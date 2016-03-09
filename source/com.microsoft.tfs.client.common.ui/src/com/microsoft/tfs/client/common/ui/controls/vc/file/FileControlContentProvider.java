// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.file;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;

public class FileControlContentProvider implements IStructuredContentProvider {
    private boolean showDeletedItems;

    public FileControlContentProvider(final boolean showDeletedItems) {
        this.showDeletedItems = showDeletedItems;
    }

    public void setShowDeletedItems(final boolean showDeletedItems) {
        this.showDeletedItems = showDeletedItems;
    }

    @Override
    public Object[] getElements(final Object inputElement) {
        final TFSFolder folder = (TFSFolder) inputElement;
        if (folder == null) {
            return new Object[0];
        }

        // System.out.println("getting elements for [" + folder + "]");

        return folder.getChildren(showDeletedItems).toArray();
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
    }
}
