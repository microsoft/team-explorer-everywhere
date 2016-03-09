// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.connect;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.connect.ServerListControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.util.Check;

public class ServerListDialog extends BaseDialog {
    private final ServerList serverList;

    private ServerListControl serverListControl;

    public ServerListDialog(final Shell parentShell, final ServerList serverList) {
        super(parentShell);

        Check.notNull(serverList, "serverList"); //$NON-NLS-1$
        this.serverList = serverList;

        setOptionIncludeDefaultButtons(false);
        addButtonDescription(IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, true);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        dialogArea.setLayout(layout);

        final Label label = new Label(dialogArea, SWT.NONE);
        label.setText(Messages.getString("ServerListDialog.TfsLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(label);

        serverListControl = new ServerListControl(dialogArea, SWT.NONE);
        serverListControl.setServerList(serverList);

        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().applyTo(serverListControl);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ServerListDialog.AddRemoveDialogTitle"); //$NON-NLS-1$
    }

    public ServerListConfigurationEntry[] getSelectedServerListEntries() {
        return serverListControl.getSelectedServerListEntries();
    }

    public ServerListConfigurationEntry getLastAddedServerListEntry() {
        return serverListControl.getLastAddedServerListEntry();
    }

    public TFSConnection getLastAddedConnection() {
        return serverListControl.getLastAddedConnection();
    }
}
