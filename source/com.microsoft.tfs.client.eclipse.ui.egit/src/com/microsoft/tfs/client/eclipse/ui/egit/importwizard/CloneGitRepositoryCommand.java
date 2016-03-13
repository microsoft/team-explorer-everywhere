// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public class CloneGitRepositoryCommand extends TFSCommand {
    private static final String GIT_FOLDER_NAME = ".git"; //$NON-NLS-1$

    private static final int timeout = 30;

    private final CredentialsProvider credentialsProvider;
    private final URL repositoryUrl;
    private final String repositoryName;
    private final String[] refs;
    private final String defaultRef;
    private final String workingDirectory;
    private final boolean cloneSubmodules;
    private final String remoteName;

    private String errorMessage;

    private static final Log logger = LogFactory.getLog(CloneGitRepositoryCommand.class);

    public CloneGitRepositoryCommand(
        final UsernamePasswordCredentials credentials,
        final URL repositoryUrl,
        final String repositoryName,
        final String[] refs,
        final String defaultRef,
        final String workingDirectory,
        final boolean cloneSubmodules,
        final String remoteName) {
        if (credentials != null
            && !StringUtil.isNullOrEmpty(credentials.getUsername())
            && !StringUtil.isNullOrEmpty(credentials.getPassword())) {
            this.credentialsProvider =
                new UsernamePasswordCredentialsProvider(credentials.getUsername(), credentials.getPassword());
        } else {
            this.credentialsProvider = new UsernamePasswordCredentialsProvider(StringUtil.EMPTY, new char[0]);
        }

        this.repositoryUrl = repositoryUrl;
        this.repositoryName = repositoryName;
        this.workingDirectory = workingDirectory;
        this.cloneSubmodules = cloneSubmodules;
        this.remoteName = remoteName;
        /*
         * Git API does not allow for nulls in refs and defaultRef
         */
        this.refs = refs != null ? refs : new String[0];
        this.defaultRef = defaultRef != null ? defaultRef : StringUtil.EMPTY;
    }

    @Override
    public String getName() {
        final String commandNameFormat = Messages.getString("CloneGitRepositoryCommand.CommandNameFormat"); //$NON-NLS-1$
        return MessageFormat.format(commandNameFormat, repositoryName, repositoryUrl.toString());
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("CloneGitRepositoryCommand.ErrorMessage"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return null;
    }

    public String getErrorMessage() {
        return errorMessage == null ? StringUtil.EMPTY : errorMessage;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            if (cloneRepository(progressMonitor)) {
                return Status.OK_STATUS;
            } else {
                return new Status(
                    IStatus.ERROR,
                    TFSCommonClientPlugin.PLUGIN_ID,
                    0,
                    MessageFormat.format(
                        Messages.getString("CloneGitRepositoryCommand.CloningErrorMessageFormat"), //$NON-NLS-1$
                        repositoryName,
                        errorMessage),
                    null);
            }
        } catch (final CanceledException e) {
            logger.warn("Operation cancelled by the user"); //$NON-NLS-1$
            return Status.CANCEL_STATUS;
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            errorMessage = e.getLocalizedMessage();
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        } finally {
            progressMonitor.done();
        }
    }

    private boolean cloneRepository(final IProgressMonitor progressMonitor) {
        final CloneCommand cloneRepository = Git.cloneRepository();

        cloneRepository.setCredentialsProvider(credentialsProvider);
        cloneRepository.setProgressMonitor(new CloneProgress(progressMonitor));

        final File workFolder = new File(workingDirectory);
        if (!workFolder.exists()) {
            if (!workFolder.mkdirs()) {
                if (!workFolder.isDirectory()) {
                    final String errorMessageFormat = "Cannot create {0} directory"; //$NON-NLS-1$
                    throw new RuntimeException(MessageFormat.format(errorMessageFormat, workingDirectory));
                }
            }
        }
        cloneRepository.setDirectory(new File(workingDirectory));

        cloneRepository.setRemote(remoteName);
        cloneRepository.setURI(URIUtils.encodeQueryIgnoringPercentCharacters(repositoryUrl.toString()));

        cloneRepository.setCloneAllBranches(true);
        cloneRepository.setBranchesToClone(Arrays.asList(refs));
        cloneRepository.setBranch(defaultRef);

        cloneRepository.setNoCheckout(false);
        cloneRepository.setTimeout(timeout);
        cloneRepository.setCloneSubmodules(cloneSubmodules);

        try {
            cloneRepository.call();
            if (progressMonitor.isCanceled()) {
                throw new CanceledException();
            }

            registerClonedRepository(workingDirectory);
        } catch (final CanceledException e) {
            throw e;
        } catch (final Exception e) {
            logger.error("Error cloning repository:", e); //$NON-NLS-1$
            errorMessage = e.getLocalizedMessage();
            return false;
        }

        return true;
    }

    private void registerClonedRepository(final String workingDirectory) {
        final RepositoryUtil util = Activator.getDefault().getRepositoryUtil();
        util.addConfiguredRepository(new File(workingDirectory, GIT_FOLDER_NAME));
    }

    private class CloneProgress implements ProgressMonitor {
        final IProgressMonitor parentProgress;

        private int taskDone;
        private String taskTitle;
        private String taskPercent;
        private int taskTotalWork;
        private IProgressMonitor task;

        public CloneProgress(final IProgressMonitor parentProgress) {
            Check.notNull(parentProgress, "parentProgress"); //$NON-NLS-1$
            this.parentProgress = parentProgress;
        }

        @Override
        public void start(final int totalTasks) {
            parentProgress.beginTask(
                MessageFormat.format(
                    Messages.getString("CloneGitRepositoryCommand.CloningRepositoryMessageFormat"), //$NON-NLS-1$
                    repositoryName),
                totalTasks + 1);
        }

        @Override
        public void beginTask(final String title, final int totalWork) {
            if (totalWork < 0) {
                logger.error("Negative total work amount"); //$NON-NLS-1$
            }

            taskTitle = title;
            taskPercent = StringUtil.EMPTY;
            taskDone = 0;
            taskTotalWork = totalWork;
            task = new SubProgressMonitor(parentProgress, 1);
            task.beginTask(taskTitle, totalWork);
        }

        @Override
        public void update(final int completed) {
            final int taskWorked = Math.min(completed, taskTotalWork - taskDone);
            if (taskWorked != completed) {
                logger.error("Completed more than total work amount"); //$NON-NLS-1$
            }

            taskDone = taskDone + taskWorked;

            final String percent = MessageFormat.format("  {0,number,percent}", (float) taskDone / taskTotalWork); //$NON-NLS-1$

            if (!percent.equals(taskPercent)) {
                final StringBuilder sb = new StringBuilder(taskTitle);
                sb.append(" ("); //$NON-NLS-1$
                sb.append(percent.substring(percent.length() - 4));
                sb.append(")"); //$NON-NLS-1$

                task.subTask(sb.toString());
                taskPercent = percent;
            }

            task.worked(taskWorked);
        }

        @Override
        public void endTask() {
            if (task != null) {
                try {
                    task.done();
                } finally {
                    task = null;
                }
            }
        }

        @Override
        public boolean isCancelled() {
            return parentProgress.isCanceled();
        }

    }
}
