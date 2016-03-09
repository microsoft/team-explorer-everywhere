// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.util.Check;

class ProjectManagerCachedWorkspaceConnectionJob extends ProjectManagerConnectionJob {
    private static final Log log = LogFactory.getLog(ProjectManagerCachedWorkspaceConnectionJob.class);

    private final WorkspaceInfo cachedWorkspace;

    ProjectManagerCachedWorkspaceConnectionJob(
        final ProjectConnectionManager connectionManager,
        final ProjectManagerDataProvider dataProvider,
        final URI uri,
        final WorkspaceInfo cachedWorkspace) {
        super(
            dataProvider,
            cachedWorkspace.getServerURI(),
            WorkspaceLocation.LOCAL.equals(cachedWorkspace.getLocation()));

        Check.notNull(cachedWorkspace, "cachedWorkspace"); //$NON-NLS-1$

        this.cachedWorkspace = cachedWorkspace;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        final URI serverURI = cachedWorkspace.getServerURI();

        /* See if the credentials provider has credentials for us to use. */
        final CredentialsManager credentialsProvider =
            EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);
        final CachedCredentials cachedCredentials = credentialsProvider.getCredentials(serverURI);

        Credentials credentials;

        if (cachedCredentials != null) {
            credentials = cachedCredentials.toCredentials();

            /*
             * If the password is omitted (and we're connecting immediately),
             * prompt.
             */
            if (cachedCredentials.getPassword() == null
                && WorkspaceLocation.SERVER.equals(cachedWorkspace.getLocation())) {
                credentials = getDataProvider().getCredentials(cachedWorkspace, cachedCredentials.toCredentials());
            }
        } else {
            credentials = new DefaultNTCredentials();
        }

        if (credentials == null) {
            return Status.CANCEL_STATUS;
        }

        setCredentials(credentials);

        return super.run(monitor);
    }
}
