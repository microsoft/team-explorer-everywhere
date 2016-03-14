// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.dialogs;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.ProjectFileControl;
import com.microsoft.tfs.client.common.ui.teambuild.egit.Messages;
import com.microsoft.tfs.core.clients.build.GitProperties;
import com.microsoft.tfs.util.StringUtil;

public class GitProjectFileControl extends ProjectFileControl {
    String repoLocalPath = null;

    private boolean repositoryCloned;
    private boolean branchCheckedOut;
    private boolean localCopyExists;

    public GitProjectFileControl(final Composite parent, final int style) {
        super(parent, style);
    }

    @Override
    protected void validate() {
        isValidServerPath = true;
        final String configFolderPath = configFolderText.getText().trim();

        if (StringUtil.isNullOrEmpty(configFolderPath)
            || !configFolderPath.startsWith(GitProperties.GitPathBeginning)) {
            isValidServerPath = false;
        } else {
            final String pathPattern = GitProperties.GitPathBeginning + ".+/.+/.+/.*"; //$NON-NLS-1$
            isValidServerPath = configFolderPath.matches(pathPattern);
        }

        super.validate();
    }

    @Override
    public void clearProjectFileStatus() {
        super.clearProjectFileStatus();
        repositoryCloned = false;
        branchCheckedOut = false;
        localCopyExists = false;
    }

    public void setRepositoryCloned(final boolean cloned) {
        repositoryCloned = cloned;
    }

    public void setBranchCheckedOut(final boolean checkedOut) {
        branchCheckedOut = checkedOut;
    }

    public void setLocalCopyExists(final boolean exists) {
        localCopyExists = exists;
    }

    public boolean getLocalCopyExists() {
        return localCopyExists;
    }

    @Override
    public void setLocalPath(final String path) {
        repoLocalPath = path;
    }

    @Override
    protected String getBuildFileWarningMessage() {
        if (!isValidServerPath) {
            return Messages.getString("GitProjectFileControl.InvalidServerPathText"); //$NON-NLS-1$
        }

        if (!repositoryCloned) {
            return Messages.getString("GitProjectFileControl.BuildFileWarningNotCloned"); //$NON-NLS-1$
        }

        if (!branchCheckedOut) {
            return Messages.getString("GitProjectFileControl.BuildFileWarningNotCheckedOut"); //$NON-NLS-1$
        }

        if (localCopyExists) {
            return Messages.getString("GitProjectFileControl.BuildFileWarningNotCommitted"); //$NON-NLS-1$
        }

        return Messages.getString("GitProjectFileTabPage.BuildFileWarning"); //$NON-NLS-1$
    }

    @Override
    protected String getSummaryLabelTextFormat() {
        return Messages.getString("GitProjectFileTabPage.SummaryLabelTextFormat"); //$NON-NLS-1$
    }

    @Override
    protected String getConfigFolderLabelText() {
        return Messages.getString("GitProjectFileTabPage.ConfigFolderLabelText"); //$NON-NLS-1$
    }

    @Override
    protected boolean canCreatProject() {
        return super.canCreatProject() && repositoryCloned && branchCheckedOut && !localCopyExists;
    }
}
