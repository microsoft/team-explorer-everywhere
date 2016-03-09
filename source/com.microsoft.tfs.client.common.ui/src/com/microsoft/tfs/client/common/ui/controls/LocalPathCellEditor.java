// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.celleditor.TextWithDialogCellEditor;

public class LocalPathCellEditor extends TextWithDialogCellEditor {
    public LocalPathCellEditor(final Composite parent, final int style) {
        super(parent, style);
    }

    @Override
    protected String openDialog(final Shell shell, final String currentValue) {
        final DirectoryDialog dialog = new DirectoryDialog(shell);
        dialog.setText(Messages.getString("LocalPathCellEditor.BrowseDialogTitle")); //$NON-NLS-1$
        dialog.setMessage(Messages.getString("LocalPathCellEditor.BrowseDialogText")); //$NON-NLS-1$
        if (currentValue != null) {
            dialog.setFilterPath(currentValue);
        }
        String s = dialog.open();
        if (s == null) {
            s = currentValue;
        }
        return s;
    }
}
