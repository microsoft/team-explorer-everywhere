// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.branch;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;

public class BranchObjectModelContentProvider implements ITreeContentProvider {
    private BranchObjectModel model = null;

    @Override
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof BranchObject) {
            final BranchObject b = (BranchObject) parentElement;
            return model.getChildren(b).toArray();
        }
        if (parentElement instanceof ItemIdentifier) {
            final ItemIdentifier id = (ItemIdentifier) parentElement;
            return getChildren(model.toBranchObject(id));
        }
        return null;
    }

    @Override
    public Object getParent(final Object element) {
        if (element instanceof BranchObject) {
            final BranchObject b = (BranchObject) element;
            return model.getParent(b);
        }
        if (element instanceof ItemIdentifier) {
            final ItemIdentifier id = (ItemIdentifier) element;
            return getParent(model.toBranchObject(id));
        }
        return null;
    }

    @Override
    public boolean hasChildren(final Object element) {
        if (element instanceof BranchObject) {
            final BranchObject b = (BranchObject) element;
            return model.getChildren(b).size() > 0;
        }
        if (element instanceof ItemIdentifier) {
            final ItemIdentifier id = (ItemIdentifier) element;
            return hasChildren(model.toBranchObject(id));
        }
        return false;
    }

    @Override
    public Object[] getElements(final Object inputElement) {
        if (inputElement instanceof BranchObjectModel) {
            final BranchObjectModel b = (BranchObjectModel) inputElement;
            return b.getRoots();
        }
        return null;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        if (newInput instanceof BranchObjectModel) {
            model = (BranchObjectModel) newInput;
        }
    }

}
