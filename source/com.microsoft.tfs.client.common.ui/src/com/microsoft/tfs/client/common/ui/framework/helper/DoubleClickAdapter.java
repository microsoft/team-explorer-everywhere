// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

public abstract class DoubleClickAdapter implements IDoubleClickListener {
    protected abstract void doubleClick(Object item);

    @Override
    public void doubleClick(final DoubleClickEvent event) {
        final ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            doubleClick(structuredSelection.getFirstElement());
        }
    }
}
