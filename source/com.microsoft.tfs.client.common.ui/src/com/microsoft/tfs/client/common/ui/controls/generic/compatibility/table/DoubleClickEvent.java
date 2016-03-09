// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.compatibility.table;

import java.util.EventObject;

import org.eclipse.jface.viewers.ISelection;

public class DoubleClickEvent extends EventObject {
    private final ISelection selection;

    public DoubleClickEvent(final Object source, final ISelection selection) {
        super(source);
        this.selection = selection;
    }

    public ISelection getSelection() {
        return selection;
    }
}
