// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.microsoft.tfs.checkinpolicies.build.Messages;
import com.microsoft.tfs.checkinpolicies.build.settings.BuildPolicyConfiguration;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.util.Check;

public class BuildPolicyDialog extends BaseDialog {
    private final BuildPolicyConfiguration configuration;

    private TabItem generalTabItem;
    private TabItem markersTabItem;

    /**
     * The given configuration is not modified. Call {@link #getConfiguration()}
     * .
     */
    public BuildPolicyDialog(final Shell parentShell, final BuildPolicyConfiguration configuration) {
        super(parentShell);

        Check.notNull(configuration, "configuration"); //$NON-NLS-1$
        this.configuration = new BuildPolicyConfiguration(configuration);
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * hookAddToDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        SWTUtil.gridLayout(dialogArea);

        final TabFolder folder = new TabFolder(dialogArea, SWT.TOP);
        GridDataBuilder.newInstance().fill().grab().applyTo(folder);

        generalTabItem = new TabItem(folder, SWT.NONE);
        generalTabItem.setText(Messages.getString("BuildPolicyDialog.GeneralTabItemText")); //$NON-NLS-1$
        final GeneralTabControl generalTabControl = new GeneralTabControl(folder, SWT.NONE, configuration);
        generalTabItem.setControl(generalTabControl);

        markersTabItem = new TabItem(folder, SWT.NONE);
        markersTabItem.setText(Messages.getString("BuildPolicyDialog.MarkersTabItemText")); //$NON-NLS-1$
        final MarkerTabControl markerTabControl = new MarkerTabControl(folder, SWT.NONE, configuration);
        markersTabItem.setControl(markerTabControl);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("BuildPolicyDialog.DialogTitle"); //$NON-NLS-1$
    }

    public BuildPolicyConfiguration getConfiguration() {
        return configuration;
    }
}
