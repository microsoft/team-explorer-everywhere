// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.checkinpolicies;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.RegularExpressionTableData;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyScopeControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.util.Check;

public class PolicyScopeDialog extends BaseDialog {
    private final String policyName;
    private final String[] scopeExpressions;

    private PolicyScopeControl scopeControl;

    public PolicyScopeDialog(final Shell parentShell, final String policyName, final String[] scopeExpressions) {
        super(parentShell);

        Check.notNull(policyName, "policyName"); //$NON-NLS-1$
        Check.notNull(scopeExpressions, "scopeExpressions"); //$NON-NLS-1$

        this.policyName = policyName;
        this.scopeExpressions = scopeExpressions;
    }

    public String[] getScopeExpressions() {
        final RegularExpressionTableData[] expressions = scopeControl.getScopeExpressions();
        final String[] ret = new String[expressions.length];

        for (int i = 0; i < expressions.length; i++) {
            ret[i] = expressions[i].getExpression();
        }

        return ret;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        SWTUtil.fillLayout(dialogArea);
        scopeControl = new PolicyScopeControl(dialogArea, SWT.NONE);

        final RegularExpressionTableData[] expressions = new RegularExpressionTableData[scopeExpressions.length];
        for (int i = 0; i < scopeExpressions.length; i++) {
            expressions[i] = new RegularExpressionTableData(scopeExpressions[i]);
        }

        scopeControl.setScopeExpressions(expressions);

        addButtonDescription(IDialogConstants.HELP_ID, IDialogConstants.HELP_LABEL, false);
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (buttonId == IDialogConstants.HELP_ID) {
            final String text = Messages.getString("PolicyScopeDialog.CheckInPolicyRegexDialogText"); //$NON-NLS-1$

            final MessageDialog dialog =
                new MessageDialog(
                    getShell(),
                    Messages.getString("PolicyScopeDialog.CheckInPolicyRegexDialogTitle"), //$NON-NLS-1$
                    null,
                    text,
                    MessageDialog.INFORMATION,
                    new String[] {
                        IDialogConstants.OK_LABEL
            }, 0);

            dialog.open();
        }
    }

    @Override
    protected String provideDialogTitle() {
        return MessageFormat.format(
            Messages.getString("PolicyScopeDialog.ScopeExpressionsDialogTitleFormat"), //$NON-NLS-1$
            policyName);
    }
}
