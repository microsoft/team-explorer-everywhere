// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.git.commands.QueryGitItemsCommand;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.GitProperties;
import com.microsoft.tfs.core.clients.build.exceptions.BuildException;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.LocaleUtil;

public class CheckBuildFileExistsCommand extends TFSCommand {

    private final Log log = LogFactory.getLog(CheckBuildFileExistsCommand.class);

    private final VersionControlClient versionControl;
    private final String folderPath;
    private boolean buildFileExists;
    private final boolean isGit;

    public CheckBuildFileExistsCommand(
        final VersionControlClient versionControl,
        final String folderPath,
        final boolean isGit) {
        super();
        this.versionControl = versionControl;
        this.folderPath = folderPath;
        this.isGit = isGit;
    }

    @Override
    public String getName() {
        final String messageFormat =
            Messages.getString("CheckBuildFileExistsCommand.CheckingBuildExistsCommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, folderPath);
    }

    @Override
    public String getErrorDescription() {
        final String messageFormat =
            Messages.getString("CheckBuildFileExistsCommand.CheckingBuildExistsErrorTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, folderPath);
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat =
            Messages.getString("CheckBuildFileExistsCommand.CheckingBuildExistsCommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, folderPath);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.shared.command.Command#doRun(org.eclipse
     * .core.runtime. IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) {
        final String requiredPrefix = isGit ? GitProperties.GitPathBeginning : ServerPath.ROOT;

        if (!folderPath.startsWith(requiredPrefix)) {
            final String messageFormat =
                Messages.getString("CheckBuildFileExistsCommand.InvalidBuildFileServerPathFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, folderPath, requiredPrefix);
            return new Status(
                IStatus.ERROR,
                TFSTeamBuildPlugin.PLUGIN_ID,
                Status.OK,
                message,
                new BuildException(message));
        }

        final StringBuilder sb = new StringBuilder(folderPath);
        if (sb.charAt(sb.length() - 1) != ServerPath.PREFERRED_SEPARATOR_CHARACTER) {
            sb.append(ServerPath.PREFERRED_SEPARATOR_CHARACTER);
        }

        final String itemPath = sb.append(BuildConstants.PROJECT_FILE_NAME).toString();

        return isGit ? checkGitVC(progressMonitor, itemPath) : checkTFVC(progressMonitor, itemPath);
    }

    private IStatus checkTFVC(final IProgressMonitor progressMonitor, final String itemPath) {
        try {
            buildFileExists = versionControl.getItem(itemPath, LatestVersionSpec.INSTANCE) != null;
        } catch (final VersionControlException e) {
            // encountered a version control exception (i.e. file does not
            // exist). Ignore it
            // but log just in case it was something else.
            buildFileExists = false;
            log.warn("Ignoring exception when checking for TFSBuild.proj in " + folderPath); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }

    private IStatus checkGitVC(final IProgressMonitor progressMonitor, final String itemPath) {
        try {
            final AtomicReference<String> projectName = new AtomicReference<String>();
            final AtomicReference<String> repositoryName = new AtomicReference<String>();
            final AtomicReference<String> branchName = new AtomicReference<String>();
            final AtomicReference<String> path = new AtomicReference<String>();

            if (!GitProperties.parseGitItemUrl(itemPath, projectName, repositoryName, branchName, path)) {
                final String messageFormat =
                    Messages.getString("CheckBuildFileExistsCommand.WrongBuildPojectUriErrorFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, itemPath);
                return new Status(
                    IStatus.ERROR,
                    TFSTeamBuildPlugin.PLUGIN_ID,
                    Status.OK,
                    message,
                    new BuildException(message));
            }

            final ICommandExecutor commandExecutor = new CommandExecutor();
            final QueryGitItemsCommand test = new QueryGitItemsCommand(
                versionControl,
                projectName.get(),
                repositoryName.get(),
                branchName.get(),
                path.get());

            final IStatus status = commandExecutor.execute(test);

            buildFileExists = status.isOK() && test.getRepositoryItems().size() > 0;
        } catch (final Exception e) {
            // encountered a version control exception (i.e. file does not
            // exist). Ignore it
            // but log just in case it was something else.
            buildFileExists = false;
            log.warn("Ignoring exception when checking for TFSBuild.proj in " + folderPath); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }

    public boolean getBuildFileExists() {
        return buildFileExists;
    }

}
