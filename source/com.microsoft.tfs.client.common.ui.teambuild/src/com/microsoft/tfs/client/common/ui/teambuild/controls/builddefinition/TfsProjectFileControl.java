// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.StringUtil;

public class TfsProjectFileControl extends ProjectFileControl {
    public TfsProjectFileControl(final Composite parent, final int style) {
        super(parent, style);
    }

    @Override
    protected void validate() {
        final String configFolderPath = configFolderText.getText().trim();
        isValidServerPath =
            !StringUtil.isNullOrEmpty(configFolderPath) && configFolderPath.startsWith(ServerPath.ROOT);
        super.validate();
    }

    @Override
    protected String getBuildFileWarningMessage() {
        if (!isValidServerPath) {
            return Messages.getString("TfsProjectFileControl.InvalidServerPathText"); //$NON-NLS-1$
        }

        return Messages.getString("ProjectFileTabPage.BuildFileWarning"); //$NON-NLS-1$
    }

    @Override
    protected String getSummaryLabelTextFormat() {
        return Messages.getString("ProjectFileTabPage.SummaryLabelTextFormat"); //$NON-NLS-1$
    }

    @Override
    protected String getConfigFolderLabelText() {
        return Messages.getString("ProjectFileTabPage.ConfigFolderLabelText"); //$NON-NLS-1$
    }
}
