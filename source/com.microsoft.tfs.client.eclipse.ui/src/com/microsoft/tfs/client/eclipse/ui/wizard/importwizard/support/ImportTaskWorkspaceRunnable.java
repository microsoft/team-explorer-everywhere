// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.client.common.util.ProgressMonitorTaskMonitorAdapter;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies.ImportGetStrategy.GetStrategyStatus;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * An implementation of IWorkspaceRunnable that runs a number of ImportTasks.
 *
 * The main reason for running ImportTasks inside an IWorkspaceRunnable is
 * because workspace change events are batched up and deferred until the
 * IWorkspaceRunnable completes. This makes batch imports run much faster
 * because builds etc are not happening concurrently to import.
 */
public class ImportTaskWorkspaceRunnable implements IWorkspaceRunnable {
    private static final Log log = LogFactory.getLog(ImportTaskWorkspaceRunnable.class);

    private final ImportTask[] importTasks;

    private final List getStrategyStatusList = new ArrayList();

    public ImportTaskWorkspaceRunnable(final ImportTask[] importTasks) {
        this.importTasks = importTasks;
    }

    @Override
    public void run(final IProgressMonitor monitor) throws CoreException {
        doRun(monitor);
    }

    private void doRun(final IProgressMonitor monitor) throws CoreException {
        /*
         * Adapt the Eclipse-centric IProgressMonitor to our class TaskMonitor
         * so we can use them all the way down to core.
         */
        final TaskMonitor taskMonitor = new ProgressMonitorTaskMonitorAdapter(monitor);
        taskMonitor.begin("", importTasks.length); //$NON-NLS-1$

        for (int i = 0; i < importTasks.length; i++) {
            /*
             * Always create a fresh child monitor for the tasks to use. Do not
             * call begin; let the subtask determine its amount of work.
             */
            TaskMonitorService.pushTaskMonitor(taskMonitor.newSubTaskMonitor(1));

            try {
                importTasks[i].run();

                final GetStrategyStatus status = importTasks[i].getGetStrategyStatus();

                if (status != null) {
                    getStrategyStatusList.add(status);
                }
            } finally {
                TaskMonitorService.popTaskMonitor(true);
            }

            if (taskMonitor.isCanceled()) {
                return;
            }
        }
    }

    public GetStrategyStatus[] getGetStrategyStatus() {
        return (GetStrategyStatus[]) getStrategyStatusList.toArray(new GetStrategyStatus[getStrategyStatusList.size()]);
    }
}