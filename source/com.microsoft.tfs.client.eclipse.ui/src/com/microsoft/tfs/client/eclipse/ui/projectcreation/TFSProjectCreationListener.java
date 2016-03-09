// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.projectcreation;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;

public class TFSProjectCreationListener implements IResourceChangeListener {
    private static final Log log = LogFactory.getLog(TFSProjectCreationListener.class);

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        final ProjectCreationVisitor visitor = new ProjectCreationVisitor();

        if (!TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
            UIPreferenceConstants.CONNECT_MAPPED_PROJECTS_AT_IMPORT)) {
            return;
        }

        try {
            event.getDelta().accept(visitor);
        } catch (final CoreException e) {
            log.warn(e);
        }
    }

    private final static class ProjectCreationVisitor implements IResourceDeltaVisitor {
        @Override
        public boolean visit(final IResourceDelta delta) throws CoreException {
            final IResource resource = delta.getResource();

            /*
             * If we're given a delta for the WorkspaceRoot, simply return true
             * (to continue walking the tree to affected projects.)
             */
            if (resource instanceof IWorkspaceRoot) {
                return true;
            }
            /*
             * Otherwise, we're only interested in projects, and do not wish to
             * recurse beneath them.
             */
            else if (resource.getType() != IResource.PROJECT || !(resource instanceof IProject)) {
                return false;
            }
            /*
             * We only care about newly added projects.
             */
            else if (delta.getKind() != IResourceDelta.ADDED) {
                return false;
            }
            /*
             * We only care about Projects that have a physical on-disk
             * location.
             */
            else if (resource.getLocation() == null) {
                log.debug("New project created without an on-disk location, ignoring"); //$NON-NLS-1$
                return false;
            }
            /*
             * Ignore projects that already have a repository provider.
             */
            if (RepositoryProvider.getProvider((IProject) resource) != null) {
                log.debug("New project created with a repository provider, ignoring"); //$NON-NLS-1$
                return false;
            }

            /*
             * Get the current TFS Repository (may be null)
             */
            final TFSRepository repository =
                TFSEclipseClientPlugin.getDefault().getRepositoryManager().getDefaultRepository();

            /*
             * Get the workspace cache to determine if this folder is beneath a
             * mapped folder in one of the workspaces.
             */
            final PersistenceStoreProvider persistenceStore = (repository != null)
                ? repository.getVersionControlClient().getConnection().getPersistenceStoreProvider()
                : DefaultPersistenceStoreProvider.INSTANCE;

            final Workstation workstation = Workstation.getCurrent(persistenceStore);

            final WorkspaceInfo workspaceInfo = workstation.getLocalWorkspaceInfo(resource.getLocation().toOSString());

            /*
             * This project is in a mapped path - start a background job to
             * connect it. (We cannot do it here directly, the Workspace is
             * locked.)
             */
            if (workspaceInfo != null) {
                log.info(
                    MessageFormat.format(
                        "New project {0} is inside TFS mapped folder, setting repository provider.", //$NON-NLS-1$
                        resource.getName()));

                final TFSProjectCreationJob projectCreationJob = new TFSProjectCreationJob((IProject) resource);
                projectCreationJob.setSystem(true);
                projectCreationJob.schedule();
            }

            /* Do not recurse. */
            return false;
        }
    }
}
