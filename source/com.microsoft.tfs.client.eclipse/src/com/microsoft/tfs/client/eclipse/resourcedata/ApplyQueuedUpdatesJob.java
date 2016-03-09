// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcedata;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.util.Check;

class ApplyQueuedUpdatesJob extends Job {
    private static final Log log = LogFactory.getLog(ApplyQueuedUpdatesJob.class);

    private final ResourceDataUpdate[] updates;
    private final ResourceDataManager resourceDataManager;

    public ApplyQueuedUpdatesJob(final ResourceDataUpdate[] updates, final ResourceDataManager resourceDataManager) {
        super(Messages.getString("ApplyQueuedUpdatesJob.JobName")); //$NON-NLS-1$

        Check.notNull(updates, "updates"); //$NON-NLS-1$
        Check.notNull(resourceDataManager, "resourceDataManager"); //$NON-NLS-1$

        this.updates = updates;
        this.resourceDataManager = resourceDataManager;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        if (updates.length == 0) {
            return Status.OK_STATUS;
        }

        /*
         * Keep track of all the resources which changed so we can fire the
         * event later.
         */
        final Set<IResource> resourcesChanged = new HashSet<IResource>();

        /*
         * This job runs its sync info updating code in a Command so it can take
         * advantage of ResourceChangingCommand, which lets resource change
         * listeners ignore the storm of events regarding synchronize data we
         * create. The Eclipse plugin's listener can avoid some heavyweight
         * investigation this way.
         */
        final ICommand command = new ResourceChangingCommand(new Command() {
            @Override
            protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
                monitor.beginTask("", updates.length + 2); //$NON-NLS-1$

                /*
                 * Track resources we updated which are entire projects so we
                 * can set the property so a full refresh is not needed in the
                 * future.
                 */
                final Set<IProject> projectsChanged = new HashSet<IProject>();

                for (int i = 0; i < updates.length; i++) {
                    final IResource[] resources = updates[i].getResources();
                    final ResourceData resourceData = updates[i].getResourceData();

                    for (int j = 0; j < resources.length; j++) {
                        /*
                         * This loop cannot simply skip inaccessible resources,
                         * because it must set sync data on resources which have
                         * just been deleted (an undo of a pending undelete, or
                         * a checkin of a delete change, etc.).
                         */
                        try {
                            monitor.subTask(resources[j].getLocation().toString());

                            /*
                             * The ResourceData in the update may be null (which
                             * will remove the data from the sync info store).
                             */
                            resourceDataManager.setResourceDataInternal(resources[j], resourceData);

                            resourcesChanged.add(resources[j]);

                            if (resources[j].getType() == IResource.PROJECT) {
                                projectsChanged.add((IProject) resources[j]);
                            }
                        } catch (final Exception e) {
                            /*
                             * Ignore these exceptions. A common cause for
                             * getting here was the user deleted the resource's
                             * container (project) while processing updates.
                             */
                        }
                    }

                    monitor.worked(1);
                }

                monitor.subTask(Messages.getString("ApplyQueuedUpdatesJob.MarkingProjectsRefreshed")); //$NON-NLS-1$
                for (final Iterator<IProject> iterator = projectsChanged.iterator(); iterator.hasNext();) {
                    final IProject project = iterator.next();

                    resourceDataManager.setCompletedRefresh(project);
                }

                monitor.worked(1);

                return Status.OK_STATUS;
            }

            @Override
            public String getName() {
                return Messages.getString("ApplyQueuedUpdatesJob.SavingCommandText"); //$NON-NLS-1$
            }

            @Override
            public String getErrorDescription() {
                return Messages.getString("ApplyQueuedUpdatesJob.SavingCommandDescription"); //$NON-NLS-1$
            }

            @Override
            public String getLoggingDescription() {
                if (updates.length == 1 && updates[0].getResources().length == 1) {
                    return MessageFormat.format("Saving server item information for {0}", updates[0].getResources()[0]); //$NON-NLS-1$
                } else {
                    return MessageFormat.format("Saving server item information for {0} items", updates.length); //$NON-NLS-1$
                }
            }
        });

        final CommandExecutor executor = new CommandExecutor(monitor);
        executor.execute(command);

        /*
         * Finish the notification outside the Command, just in case handlers
         * modify resources which the normal resource change listener should
         * handle.
         */
        monitor.subTask(Messages.getString("ApplyQueuedUpdatesJob.NotifyingListeners")); //$NON-NLS-1$
        resourceDataManager.fireResourceDataChanged(resourcesChanged.toArray(new IResource[resourcesChanged.size()]));
        monitor.worked(1);

        monitor.done();

        return Status.OK_STATUS;
    }
}
