// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import org.eclipse.core.resources.IProject;

import com.microsoft.tfs.client.common.repository.TFSRepository;

public interface ProjectRepositoryManagerListener {
    /**
     * Called when a project goes online for a repository.
     *
     * @param project
     *        The {@link IProject} that is online.
     * @param repository
     *        The {@link TFSRepository} that the project is connected to.
     */
    public void onProjectConnected(IProject project, TFSRepository repository);

    /**
     * Called when a project goes offline.
     *
     * @param project
     *        The {@link IProject} that is offline.
     */
    public void onProjectDisconnected(IProject project);

    /**
     * Called when a project is no longer managed by Team Explorer Everywhere.
     *
     * @param project
     *        The {@link IProject} that is no longer managed by Team Explorer
     *        Everywhere.
     */
    public void onProjectRemoved(IProject project);

    /**
     * Note: only disconnect for multiple projects currently calls this method.
     */
    public void onOperationStarted();

    /**
     * Note: only disconnect for multiple projects currently calls this method.
     */
    public void onOperationFinished();
}
