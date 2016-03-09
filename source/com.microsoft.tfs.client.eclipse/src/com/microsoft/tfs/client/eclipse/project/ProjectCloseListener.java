// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.util.Check;

public class ProjectCloseListener implements IResourceChangeListener {
    private final Log log = LogFactory.getLog(ProjectCloseListener.class);

    private final ProjectRepositoryManager projectManager;

    public ProjectCloseListener(final ProjectRepositoryManager projectManager) {
        Check.notNull(projectManager, "projectManager"); //$NON-NLS-1$

        this.projectManager = projectManager;
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        if (event.getType() != IResourceChangeEvent.PRE_CLOSE || !(event.getResource() instanceof IProject)) {
            return;
        }

        final IProject project = (IProject) event.getResource();

        if (!project.isOpen()) {
            /* Sanity check */
            log.error(
                MessageFormat.format(
                    "Project Manager received close notification for project {0} (already closed)", //$NON-NLS-1$
                    project.getName()));

            return;
        }

        /* Exit if we don't manage this project */
        String providerName;
        try {
            providerName = project.getPersistentProperty(TeamUtils.PROVIDER_PROP_KEY);
        } catch (final CoreException e) {
            log.warn(
                MessageFormat.format(
                    "Could not query repository manager for project {0} (when handling close notification)", //$NON-NLS-1$
                    project.getName()),
                e);
            return;
        }

        if (providerName == null || !providerName.equals(TFSRepositoryProvider.PROVIDER_ID)) {
            return;
        }

        /*
         * If this is the only project for this connection, it will be
         * disconnected, thus we need to prompt for unsaved WIT changes.
         */
        final TFSRepository repository = projectManager.getRepository(project);

        if (repository != null) {
            final IProject[] allRepositoryProjects = projectManager.getProjectsForRepository(repository);

            if (allRepositoryProjects.length == 1 && allRepositoryProjects[0] == project) {
                /*
                 * Note: we have to ignore the cancel button here, there is no
                 * way to prevent the close from occurring.
                 */
                ProjectManagerDataProviderFactory.getDataProvider().promptForDisconnect();
            }
        }

        projectManager.close(project);
    }
}
