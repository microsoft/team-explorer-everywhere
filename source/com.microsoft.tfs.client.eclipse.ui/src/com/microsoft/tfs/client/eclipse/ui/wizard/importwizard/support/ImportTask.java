// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.microsoft.tfs.client.common.commands.vc.AddCommand;
import com.microsoft.tfs.client.common.ui.helpers.WorkingSetHelper;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportGetStrategy;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportGetStrategy.GetStrategyStatus;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportLocalPathStrategy;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportMapStrategy;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportOpenProjectStrategy;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * A Runnable that handles the sequence of steps to import a single selected
 * server path. An import process consists of running one or more ImportTasks in
 * sequence.
 *
 * An ImportTask delegates most of its processing to specialized strategy
 * classes. The ImportTaskFactory is responsible for creating ImportTasks by
 * determining the proper strategies to use.
 */
public class ImportTask implements Runnable {
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(ImportTask.class);

    private final ImportFolder selectedPath;
    private final ImportOptions importOptions;

    private final ImportLocalPathStrategy localPathStrategy;
    private final ImportMapStrategy mapStrategy;
    private final ImportGetStrategy getStrategy;
    private final ImportOpenProjectStrategy openProjectStrategy;

    private GetStrategyStatus getStrategyStatus;

    /**
     * Creates a new ImportTask. Instead of calling this constructor, clients
     * should use the ImportTaskFactory.
     *
     * @param selectedPath
     *        the selected server path to import
     * @param importOptions
     *        the import options in use
     * @param localPathStrategy
     *        the local path strategy to use
     * @param mapStrategy
     *        the mapping strategy to use
     * @param getStrategy
     *        the get strategy to use
     * @param openProjectStrategy
     *        the open project strategy to use
     */
    ImportTask(
        final ImportFolder selectedPath,
        final ImportOptions importOptions,
        final ImportLocalPathStrategy localPathStrategy,
        final ImportMapStrategy mapStrategy,
        final ImportGetStrategy getStrategy,
        final ImportOpenProjectStrategy openProjectStrategy) {
        this.selectedPath = selectedPath;
        this.importOptions = importOptions;
        this.localPathStrategy = localPathStrategy;
        this.mapStrategy = mapStrategy;
        this.getStrategy = getStrategy;
        this.openProjectStrategy = openProjectStrategy;
    }

    public boolean canRunInBatch() {
        return (!(localPathStrategy instanceof ImportLocalPathStrategy.NewProjectWizard));
    }

    @Override
    public void run() {
        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();

        /*
         * Use 100 units; a few for mapping, most for the get process. Make sure
         * the numbers in the loop add to 100.
         */
        taskMonitor.begin(
            MessageFormat.format(Messages.getString("ImportTask.TaskBeginPathFormat"), selectedPath.getName()), //$NON-NLS-1$
            100);

        try {
            taskMonitor.setCurrentWorkDescription(Messages.getString("ImportTask.DetermingPathsTaskDescription")); //$NON-NLS-1$
            final String localPath = localPathStrategy.getLocalPath(selectedPath, importOptions);
            taskMonitor.worked(1);

            if (localPath == null) {
                // user cancelled the New Project Wizard
                return;
            }

            log.info(MessageFormat.format(
                "Importing {0} to {1} ({2}, {3})", //$NON-NLS-1$
                selectedPath.getName(),
                localPath,
                mapStrategy.getPlan(),
                getStrategy.getPlan(selectedPath, importOptions)));

            if (taskMonitor.isCanceled()) {
                return;
            }

            /*
             * It would be nice to catch the error we get here when the local
             * path of a working folder conflicts.
             */
            taskMonitor.setCurrentWorkDescription(Messages.getString("ImportTask.CreatingMappingTaskDescription")); //$NON-NLS-1$
            mapStrategy.map(selectedPath, localPath, importOptions);
            taskMonitor.worked(1);

            if (taskMonitor.isCanceled()) {
                mapStrategy.cancelMap(selectedPath, localPath, importOptions);
                return;
            }

            /*
             * get requires that TaskMonitorService.getTaskMonitor() returns a
             * fresh instance (begin has not been called).
             */
            taskMonitor.setCurrentWorkDescription(Messages.getString("ImportTask.GettingFilesTaskDescription")); //$NON-NLS-1$
            TaskMonitorService.pushTaskMonitor(taskMonitor.newSubTaskMonitor(95));
            try {
                getStrategyStatus = getStrategy.get(selectedPath, importOptions);
            } finally {
                TaskMonitorService.popTaskMonitor(true);
            }

            if (taskMonitor.isCanceled()) {
                mapStrategy.cancelMap(selectedPath, localPath, importOptions);

                /*
                 * TODO "cancel" the get?
                 */

                return;
            }

            taskMonitor.setCurrentWorkDescription(
                Messages.getString("ImportTask.CreatingPendingChangesTaskDescription")); //$NON-NLS-1$
            final String[] filepathsToAdd = localPathStrategy.getFilePathsToAdd();
            if (filepathsToAdd != null && filepathsToAdd.length > 0) {

                final AddCommand addCommand = new AddCommand(
                    importOptions.getTFSRepository(),
                    filepathsToAdd,
                    false,
                    LockLevel.UNCHANGED,
                    GetOptions.NONE,
                    PendChangesOptions.NONE);

                TaskMonitorService.pushTaskMonitor(taskMonitor.newSubTaskMonitor(2));

                try {
                    addCommand.run(new NullProgressMonitor());
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    TaskMonitorService.popTaskMonitor(true);
                }
            }

            taskMonitor.setCurrentWorkDescription(Messages.getString("ImportTask.OpeningProjectTaskDescription")); //$NON-NLS-1$
            final IProject project = openProjectStrategy.open(localPath, importOptions);
            if (importOptions.getWorkingSet() != null) {
                WorkingSetHelper.addToWorkingSet(project, importOptions.getWorkingSet());
            }
            taskMonitor.worked(1);

            /*
             * If this is a local workspace, there may be baseline folders
             * ($tf/.tf) which we need to mark as "team private" resources.
             * Resource Change Listener would normally catch these, but opening
             * the project (the previous step) doesn't include all the resources
             * in the initial delta.
             */
            if (importOptions.getTFSWorkspace().getLocation() == WorkspaceLocation.LOCAL) {
                TeamUtils.markBaselineFoldersTeamPrivate(project);
            }
        } catch (final CoreException ex) {
            throw new RuntimeException(ex);
        } finally {
            taskMonitor.done();
        }
    }

    public String getServerPath() {
        return selectedPath.getFullPath();
    }

    public String getLocalizedDescription() {
        final String messageFormat = Messages.getString("ImportTask.ImporingPathsFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(
            messageFormat,
            selectedPath.getFullPath(),
            localPathStrategy.getLocalPathDescription(selectedPath, importOptions));
        return message;
    }

    /**
     * WARNING: the return value is non-localized and should be used for logging
     * or debugging purposes only, never displayed directly in the UI.
     *
     * @return some text describing what this ImportTask will do when run
     *         (non-localized)
     */
    public String getImportPlan() {
        final StringBuffer sb = new StringBuffer();

        sb.append("import [" + selectedPath.getFullPath() + "]").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append(localPathStrategy.getPlan(selectedPath, importOptions)).append(NEWLINE);
        sb.append(mapStrategy.getPlan()).append(NEWLINE);
        sb.append(getStrategy.getPlan(selectedPath, importOptions)).append(NEWLINE);
        sb.append(openProjectStrategy.getPlan()).append(NEWLINE);

        return sb.toString();
    }

    /**
     * Return the status of the get operation, useful for checking for
     * conflicts, etc.
     *
     * @return the GetStatus from the get or null if no get was performed
     */
    public GetStrategyStatus getGetStrategyStatus() {
        return getStrategyStatus;
    }
}