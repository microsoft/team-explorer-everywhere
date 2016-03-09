// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties.branches;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.microsoft.tfs.core.util.Hierarchical;

public class BranchesPropertiesContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getChildren(final Object element) {
        return ((Hierarchical) element).getChildren();
    }

    @Override
    public Object getParent(final Object element) {
        return ((Hierarchical) element).getParent();
    }

    @Override
    public boolean hasChildren(final Object element) {
        return ((Hierarchical) element).hasChildren();
    }

    @Override
    public Object[] getElements(final Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
    }

}
