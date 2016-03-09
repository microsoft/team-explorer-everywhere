// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.checkinpolicies;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.DefinePoliciesControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyConfiguration;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoader;
import com.microsoft.tfs.core.clients.versioncontrol.TeamProject;
import com.microsoft.tfs.util.Check;

public class DefinePoliciesDialog extends BaseDialog {
    private final TeamProject teamProject;
    private final PolicyConfiguration[] initialPolicyConfigurations;
    private final PolicyLoader policyLoader;

    private DefinePoliciesControl policyControl;

    public DefinePoliciesDialog(
        final Shell parentShell,
        final TeamProject teamProject,
        final PolicyConfiguration[] initialPolicyConfigurations,
        final PolicyLoader policyLoader) {
        super(parentShell);

        Check.notNull(teamProject, "teamProject"); //$NON-NLS-1$
        Check.notNull(initialPolicyConfigurations, "initialPolicyConfigurations"); //$NON-NLS-1$
        Check.notNull(policyLoader, "policyLoader"); //$NON-NLS-1$

        this.teamProject = teamProject;
        this.initialPolicyConfigurations = initialPolicyConfigurations;
        this.policyLoader = policyLoader;
    }

    public PolicyConfiguration[] getPolicyConfigurations() {
        return policyControl.getPolicyConfigurations();
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final FillLayout layout = new FillLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.spacing = getSpacing();
        dialogArea.setLayout(layout);

        policyControl = new DefinePoliciesControl(dialogArea, SWT.NONE, teamProject, policyLoader);
        policyControl.setPolicyConfigurations(initialPolicyConfigurations);
    }

    @Override
    protected String provideDialogTitle() {
        return MessageFormat.format(
            Messages.getString("DefinePoliciesDialog.CheckinPolicyDialogTitleFormat"), //$NON-NLS-1$
            teamProject.getName());
    }
}
