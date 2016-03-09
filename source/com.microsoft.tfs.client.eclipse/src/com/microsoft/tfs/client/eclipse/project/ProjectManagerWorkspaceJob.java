// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.util.Check;

class ProjectManagerWorkspaceJob extends Job {
    private static final Log log = LogFactory.getLog(ProjectManagerWorkspaceJob.class);

    private final IProject project;
    private final URI serverURI;
    private final Credentials credentials;
    private Workspace workspace;

    ProjectManagerWorkspaceJob(
        final ProjectManagerDataProvider dataProvider,
        final IProject project,
        final URI serverURI,
        final Credentials credentials) {
        super(
            MessageFormat.format(Messages.getString("ProjectManagerWorkspaceJob.JobNameFormat"), serverURI.toString())); //$NON-NLS-1$

        setSystem(true);

        this.project = project;
        this.serverURI = serverURI;
        this.credentials = credentials;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        TFSTeamProjectCollection connection;
        VersionControlClient client;

        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(project.getLocation(), "project.getLocation()"); //$NON-NLS-1$
        Check.notNull(project.getLocation().toOSString(), "project.getLocation().toOSString()"); //$NON-NLS-1$

        try {
            connection = new TFSTeamProjectCollection(serverURI, credentials);
            connection.ensureAuthenticated();
            client = connection.getVersionControlClient();
        } catch (final Exception e) {
            return new Status(
                Status.ERROR,
                TFSEclipseClientPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(
                    Messages.getString("ProjectManagerWorkspaceJob.CouldNotConnectToServerFormat"), //$NON-NLS-1$
                    serverURI.toString()),
                e);
        }

        workspace = client.getWorkspace(project.getLocation().toOSString());

        return Status.OK_STATUS;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}
