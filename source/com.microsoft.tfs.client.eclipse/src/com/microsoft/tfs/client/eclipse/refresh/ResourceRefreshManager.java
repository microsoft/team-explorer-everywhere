// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.refresh;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.util.CoreAffectedResourceCollector;
import com.microsoft.tfs.client.eclipse.commands.eclipse.RefreshResourcesCommand;

public class ResourceRefreshManager extends CoreAffectedResourceCollector {
    private final Set<Thread> ignoreThreads = new HashSet<Thread>();

    public ResourceRefreshManager(final RepositoryManager repositoryManager) {
        /* Handle newly created repositories when they're added */
        final TFSRepository[] repositories =
            repositoryManager.getRepositoriesAndAddListener(new RepositoryManagerAdapter() {
                @Override
                public void onRepositoryAdded(final RepositoryManagerEvent event) {
                    addRepository(event.getRepository());
                }

                @Override
                public void onRepositoryRemoved(final RepositoryManagerEvent event) {
                    removeRepository(event.getRepository());
                }
            });

        for (int i = 0; i < repositories.length; i++) {
            addRepository(repositories[i]);
        }
    }

    /**
     * Causes this {@link ResourceRefreshManager} to ignore change events which
     * were caused by this thread until
     * {@link #stopIgnoreThreadResourceChangeEvents()} is called.
     */
    public void startIgnoreThreadResourceRefreshEvents() {
        final Thread t = Thread.currentThread();

        synchronized (ignoreThreads) {
            ignoreThreads.add(t);
        }
    }

    /**
     * Resumes processing of change events caused by this thread which were
     * ignored since {@link #startIgnoreThreadResourceChangeEvents()} was
     * called.
     */
    public void stopIgnoreThreadResourceRefreshEvents() {
        final Thread t = Thread.currentThread();

        synchronized (ignoreThreads) {
            ignoreThreads.remove(t);
        }
    }

    @Override
    protected void resourcesChanged(final Set<IResource> resources) {
        synchronized (ignoreThreads) {
            if (ignoreThreads.contains(Thread.currentThread())) {
                return;
            }
        }

        /*
         * Note: we refresh resources synchronously, this ensures that other
         * listeners (for example, file modification validator) are called on
         * this same thread, so that they can be ignoring resources
         * appropriately. If you were to move this to a background job, it is
         * recommended that you propagate the file modification validator's
         * ignore state to the job's execution thread.
         */
        final Command refreshCommand = new RefreshResourcesCommand(resources.toArray(new IResource[resources.size()]));
        new CommandExecutor().execute(refreshCommand);
    }
}
