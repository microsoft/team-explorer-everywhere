// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl.VersionDescription;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

public class ServerItemVersionDialog extends ServerItemTreeDialog {
    private final TFSRepository repository;

    private final VersionSpec versionSpec;

    private VersionPickerControl versionPicker;

    public ServerItemVersionDialog(
        final Shell parentShell,
        final TFSRepository repository,
        final String title,
        final String initialPath,
        final ServerItemSource serverItemSource,
        final ServerItemType[] visibleTypes,
        final VersionSpec versionSpec) {
        super(parentShell, title, initialPath, serverItemSource, visibleTypes);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.versionSpec = versionSpec;
    }

    @Override
    protected void hookAddToDialogArea(final Composite composite) {
        super.hookAddToDialogArea(composite);

        versionPicker = new VersionPickerControl(composite, SWT.NONE);
        versionPicker.setRepository(repository);
        versionPicker.setVersionSpec(versionSpec);
        versionPicker.setPath(getInitialPath());
    }

    public VersionSpec getVersionSpec() {
        return versionPicker.getVersionSpec();
    }

    public VersionDescription getVersionType() {
        return versionPicker.getVersionType();
    }

    @Override
    protected void selectedServerItemChanged(final TypedServerItem serverItem) {
        super.selectedServerItemChanged(serverItem);
        if (serverItem != null && versionPicker != null) {
            versionPicker.setPath(serverItem.getServerPath());
        }
    }
}
