// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class CalculateDefaultBranchPathCommand extends TFSConnectedCommand {
    private static final String BRANCH_DEFAULT_NAME = "-branch"; //$NON-NLS-1$

    private final TFSRepository repository;
    private final String baseServerPath;

    private String defaultBranchPath;

    public CalculateDefaultBranchPathCommand(final TFSRepository repository, final String baseServerPath) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(baseServerPath, "baseServerPath"); //$NON-NLS-1$

        this.repository = repository;
        this.baseServerPath = baseServerPath;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return Messages.getString("CalculateDefaultBranchPathCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("CalculateDefaultBranchPathCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("CalculateDefaultBranchPathCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final ServerItemPath serverItemPath = new ServerItemPath(baseServerPath);

        if (serverItemPath.isRoot()) {
            return new Status(
                IStatus.ERROR,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("CalculateDefaultBranchPathCommand.CannotBranchRoot"), //$NON-NLS-1$
                null);
        }

        if (serverItemPath.equals(serverItemPath.getTeamProject())) {
            return new Status(
                IStatus.ERROR,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("CalculateDefaultBranchPathCommand.CannotBranchProject"), //$NON-NLS-1$
                null);
        }

        String proposedPath = null;

        int attempt = 0;
        boolean proposedNameExists = true;

        while (proposedNameExists) {
            // Loop until we find a name that does not exists on the server.
            proposedPath = calculatePath(serverItemPath, attempt++);
            proposedNameExists = repository.getVersionControlClient().testItemExists(proposedPath);
        }

        defaultBranchPath = proposedPath;

        return Status.OK_STATUS;
    }

    /**
     * Calculate the proposed name for the new branch.
     */
    protected String calculatePath(final ServerItemPath serverItemPath, final int attemptCount) {
        String fileName = serverItemPath.getName();
        final int periodPos = fileName.lastIndexOf('.');

        // We don't want to treat files like .project as all extension, i.e.
        // .project should become .project-branch
        final String extension = (periodPos < 1) ? "" : fileName.substring(periodPos); //$NON-NLS-1$

        final String attempt = (attemptCount == 0) ? "" : "" + attemptCount; //$NON-NLS-1$ //$NON-NLS-2$

        if (extension.length() > 0) {
            fileName = fileName.substring(0, fileName.length() - extension.length());
        }

        return serverItemPath.getParent().getFullPath() + "/" + fileName + BRANCH_DEFAULT_NAME + attempt + extension; //$NON-NLS-1$
    }

    public String getDefaultBranchPath() {
        return defaultBranchPath;
    }
}
