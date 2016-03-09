// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.commands.vc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportTask;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportTaskWorkspaceRunnable;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportGetStrategy.GetStrategyStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public class ImportProjectsCommand extends Command {
    private final static Log log = LogFactory.getLog(ImportProjectsCommand.class);

    private final ImportFolderCollection folderCollection;
    private final ImportOptions importOptions;

    private GetStrategyStatus[] getStrategyStatus;

    public ImportProjectsCommand(
        final Workspace workspace,
        final ImportFolderCollection folderCollection,
        final ImportOptions importOptions) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(folderCollection, "folderCollection"); //$NON-NLS-1$
        Check.notNull(importOptions, "importOptions"); //$NON-NLS-1$

        this.folderCollection = folderCollection;
        this.importOptions = importOptions;

        setCancellable(true);
    }

    @Override
    public String getName() {
        /*
         * This is done so this command may be run inside a
         * ResourceChangingCommand wrapper, which eventually calls
         * IWorkspace.run(), which forces the parent task name to be prepended
         * to any status text about subtasks. Using an emtpy string lets the
         * subtasks stand alone (looks better in the import wizard).
         */
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("ImportProjectsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        /* Import wizard logs better details than we can */
        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        folderCollection.queryForProjectMetadataFiles();

        final ImportTask[] tasks = folderCollection.makeImportTasks();
        final ImportTaskWorkspaceRunnable runnable = new ImportTaskWorkspaceRunnable(tasks);

        if (canRunInBatch(tasks)) {
            importOptions.getEclipseWorkspace().run(runnable, progressMonitor);
        } else {
            runnable.run(progressMonitor);
            importOptions.getEclipseWorkspace().getRoot().refreshLocal(2, progressMonitor);
        }

        getStrategyStatus = runnable.getGetStrategyStatus();

        return Status.OK_STATUS;
    }

    private boolean canRunInBatch(final ImportTask[] tasks) {
        for (int i = 0; i < tasks.length; i++) {
            if (!tasks[i].canRunInBatch()) {
                return false;
            }
        }

        return true;
    }

    public String getCommandDescription() {
        return Messages.getString("ImportProjectsCommand.CommandText"); //$NON-NLS-1$
    }

    public GetStrategyStatus[] getGetStrategyStatus() {
        return (getStrategyStatus != null) ? getStrategyStatus : new GetStrategyStatus[0];
    }
}
