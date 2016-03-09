// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.commands;

import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.git.json.JsonHelper;
import com.microsoft.tfs.client.common.git.json.TfsGitBranchesJson;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.tasks.CanceledException;

public class QueryGitRepositoryBranchesCommand extends TFSCommand {

    private final VersionControlClient vcClient;
    private final GUID[] repositoryIds;
    private final Map<GUID, TfsGitBranchesJson> repositoryBranches = new HashMap<GUID, TfsGitBranchesJson>();

    public QueryGitRepositoryBranchesCommand(final VersionControlClient vcClient, final GUID repositoryId) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNull(repositoryId, "repositoryId"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.repositoryIds = new GUID[] {
            repositoryId
        };
    }

    public QueryGitRepositoryBranchesCommand(final VersionControlClient vcClient, final GUID[] repositoryIds) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNull(repositoryIds, "repositoryIds"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.repositoryIds = repositoryIds;
    }

    @Override
    public String getName() {
        return Messages.getString("QueryGitRepositoryBranches.QureyRepositoryBranchesCommandName"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("QueryGitRepositoryBranches.ErrorDescriptionText"); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (repositoryIds.length > 1) {
            progressMonitor.beginTask(getName(), repositoryIds.length);
        } else {
            progressMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
        }

        try {
            for (final GUID repositoryId : repositoryIds) {
                progressMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
                try {
                    final HttpClient httpClient = vcClient.getConnection().getHTTPClient();
                    final URI baseURI = vcClient.getConnection().getBaseURI();
                    final String gitURI =
                        baseURI.toString() + "_apis/git/repositories/" + repositoryId.toString() + "/refs/heads"; //$NON-NLS-1$ //$NON-NLS-2$

                    final GetMethod method = new GetMethod(gitURI);

                    try {
                        final int statusCode = httpClient.executeMethod(method);

                        if (statusCode != HttpStatus.SC_OK) {
                            final String messageFormat = Messages.getString("HttpTempDownloader.ServerErrorCodeFormat"); //$NON-NLS-1$
                            final String message = MessageFormat.format(messageFormat, Integer.toString(statusCode));
                            throw new RuntimeException(message);
                        }

                        final InputStream input = method.getResponseBodyAsStream();
                        final TfsGitBranchesJson branches =
                            JsonHelper.getObjectMapper().readValue(input, TfsGitBranchesJson.class);

                        repositoryBranches.put(repositoryId, branches);
                    } catch (final Exception ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        method.releaseConnection();
                    }
                } finally {
                    if (repositoryIds.length > 1) {
                        progressMonitor.worked(1);
                    }
                }
            }
        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        } finally {
            progressMonitor.done();
        }

        return Status.OK_STATUS;
    }

    public Map<GUID, TfsGitBranchesJson> getRepositoryBranches() {
        return repositoryBranches;
    }

    @Override
    public String getLoggingDescription() {
        return "QueryGitRepositoryBranches"; //$NON-NLS-1$
    }
}
