// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchPart;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.core.clients.build.IBuildDeletionResult;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteBuildsCommand extends TFSCommand {

    private final IBuildDetail[] buildsToDelete;
    private IBuildDetail[] deletedBuilds;
    private final IBuildServer buildServer;
    private final IWorkbenchPart workbenchPart;
    private final DeleteOptions deleteOption;

    public DeleteBuildsCommand(
        final IWorkbenchPart part,
        final IBuildServer buildServer,
        final IBuildDetail[] buildsToDelete,
        final DeleteOptions deleteOption) {
        Check.notNullOrEmpty(buildsToDelete, "buildsToDelete"); //$NON-NLS-1$
        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$

        this.buildsToDelete = buildsToDelete;
        this.buildServer = buildServer;
        this.workbenchPart = part;
        this.deleteOption = deleteOption;
    }

    @Override
    public String getName() {
        if (buildsToDelete.length == 1) {
            final String messageFormat = Messages.getString("DeleteBuildsCommand.DeletingSingleBuildMessageFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, buildsToDelete[0].getBuildNumber());
        } else {
            final String messageFormat = Messages.getString("DeleteBuildsCommand.DeletingMultiBuildsMessageFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, buildsToDelete.length);
        }
    }

    @Override
    public String getErrorDescription() {
        if (buildsToDelete.length == 1) {
            return Messages.getString("DeleteBuildsCommand.DeletingSingleBuildErrorMessage"); //$NON-NLS-1$
        } else {
            return Messages.getString("DeleteBuildsCommand.DeletingMultiBuildsErrorMessage"); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (buildsToDelete.length == 1) {
            final String messageFormat =
                Messages.getString("DeleteBuildsCommand.DeletingSingleBuildMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, buildsToDelete[0].getBuildNumber());
        } else {
            final String messageFormat =
                Messages.getString("DeleteBuildsCommand.DeletingMultiBuildsMessageFormat", LocaleUtil.ROOT); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, buildsToDelete.length);
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final List<IBuildDetail> deletedBuilds = new ArrayList<IBuildDetail>(buildsToDelete.length);
        final IBuildDeletionResult[] results = buildServer.deleteBuilds(buildsToDelete, deleteOption);

        for (int i = 0; i < results.length; i++) {
            if (results[i].isSuccessful() || results[i].getTestResultFailure() == null) {
                deletedBuilds.add(buildsToDelete[i]);
                // TODO: Close any open build editors for this build.
            }
            if (!results[i].isSuccessful()) {
                // Write to console.
                if (results[i].getTestResultFailure() != null) {
                    final String messageFormat = Messages.getString("DeleteBuildsCommand.DeleteErrorTestResultsFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        buildsToDelete[i].getBuildNumber(),
                        results[i].getTestResultFailure().getMessage());

                    TFSCommonUIClientPlugin.getDefault().getConsole().printErrorMessage(message);
                }
                if (results[i].getLabelFailure() != null) {
                    final String messageFormat = Messages.getString("DeleteBuildsCommand.DeleteErrorLabelFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        buildsToDelete[i].getLabelName(),
                        buildsToDelete[i].getBuildNumber(),
                        results[i].getLabelFailure().getMessage());

                    TFSCommonUIClientPlugin.getDefault().getConsole().printWarning(message);
                }
                if (results[i].getDropLocationFailure() != null) {
                    final String messageFormat =
                        Messages.getString("DeleteBuildsCommand.DeleteErrorDropLocationFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(
                        messageFormat,
                        buildsToDelete[i].getDropLocation(),
                        buildsToDelete[i].getBuildNumber(),
                        results[i].getDropLocationFailure().getMessage());

                    TFSCommonUIClientPlugin.getDefault().getConsole().printWarning(message);
                }
            }
        }

        this.deletedBuilds = deletedBuilds.toArray(new IBuildDetail[deletedBuilds.size()]);

        workbenchPart.getSite().getShell().getDisplay().asyncExec(new Runnable() {

            @Override
            public void run() {
                // TODO: we need an event that both team nav and build explorer
                // can listen to to update after deleted builds.
                if (workbenchPart instanceof BuildExplorer && !((BuildExplorer) workbenchPart).isDisposed()) {
                    final BuildExplorer editor = (BuildExplorer) workbenchPart;
                    editor.removeBuilds(deletedBuilds.toArray(new IBuildDetail[deletedBuilds.size()]));
                }
            }
        });

        return Status.OK_STATUS;
    }

    /**
     * @return the deletedBuilds
     */
    public IBuildDetail[] getDeletedBuilds() {
        return deletedBuilds;
    }

}
