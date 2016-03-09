// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.TeamException;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class TFSRepositoryProviderType extends RepositoryProviderType {
    @Override
    public ProjectSetCapability getProjectSetCapability() {
        return new TFSProjectSetCapability();
    }

    public class TFSProjectSetCapability extends ProjectSetCapability {
        private final String SEPARATOR = ";"; //$NON-NLS-1$

        @Override
        public String[] asReference(
            final IProject[] providerProjects,
            final ProjectSetSerializationContext context,
            final IProgressMonitor monitor) throws TeamException {
            final String[] references = new String[providerProjects.length];
            for (int i = 0; i < providerProjects.length; i++) {
                final IProject project = providerProjects[i];

                /* MULTIPLE REPOSITORIES TODO */
                final Workspace repositoryWorkspace =
                    TFSEclipseClientPlugin.getDefault().getRepositoryManager().getDefaultRepository().getWorkspace();

                final String serverPath = repositoryWorkspace.getMappedServerPath(project.getLocation().toOSString());
                final String serverUrl = repositoryWorkspace.getClient().getConnection().getBaseURI().toString();

                references[i] = serverUrl + SEPARATOR + serverPath;
            }

            return references;
        }
    }
}