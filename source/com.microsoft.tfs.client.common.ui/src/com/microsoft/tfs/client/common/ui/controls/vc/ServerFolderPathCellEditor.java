// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ServerItemTreeDialog;
import com.microsoft.tfs.client.common.ui.framework.celleditor.TextWithDialogCellEditor;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.util.Check;

public class ServerFolderPathCellEditor extends TextWithDialogCellEditor {
    private ServerItemSource serverItemSource;

    public ServerFolderPathCellEditor(final Composite parent, final int style) {
        this(parent, style, null);
    }

    public ServerFolderPathCellEditor(
        final Composite parent,
        final int style,
        final TFSTeamProjectCollection connection) {
        super(parent, style);

        if (connection != null) {
            setConnection(connection);
        }
    }

    public void setConnection(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        serverItemSource = new VersionedItemSource(connection);
    }

    @Override
    protected String openDialog(final Shell shell, final String currentValue) {
        if (serverItemSource == null) {
            throw new IllegalStateException("you must call setConnection() before the dialog is opened"); //$NON-NLS-1$
        }

        final ServerItemTreeDialog dialog =
            new ServerItemTreeDialog(
                shell,
                Messages.getString("ServerFolderPathCellEditor.BrowseForFolderDialogTitle"), //$NON-NLS-1$
                currentValue,
                serverItemSource,
                ServerItemType.ALL_FOLDERS);

        String newValue;

        if (IDialogConstants.OK_ID == dialog.open()) {
            newValue = dialog.getSelectedServerPath();
        } else {
            newValue = currentValue;
        }

        return newValue;
    }
}
