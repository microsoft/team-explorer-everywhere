// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.commands;

import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.git.json.JsonHelper;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoriesJson;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.core.httpclient.util.URIUtil;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public class QueryGitRepositoriesCommand extends TFSCommand implements ICommand {
    private final VersionControlClient vcClient;
    private TfsGitRepositoriesJson gitRepositories;
    private final String projectNameOrGUID;

    public QueryGitRepositoriesCommand(final VersionControlClient vcClient) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.projectNameOrGUID = null;
    }

    public QueryGitRepositoriesCommand(final VersionControlClient vcClient, final String projectNameOrGUID) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.projectNameOrGUID = projectNameOrGUID;
    }

    @Override
    public String getName() {
        final String messageFormat =
            Messages.getString("QueryGitRepositoriesCommand.QueryGitRepositoriesCommandNameFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, vcClient.getConnection().getBaseURI());
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("QueryGitRepositoriesCommand.ErrorDescriptionText"); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
        try {
            final HttpClient httpClient = vcClient.getConnection().getHTTPClient();
            final URI baseURI = vcClient.getConnection().getBaseURI();

            final String gitURI;

            if (StringUtil.isNullOrEmpty(projectNameOrGUID)) {
                gitURI = baseURI.toString() + "_apis/git/repositories"; //$NON-NLS-1$
            } else {
                gitURI = baseURI.toString() + "_apis/git/" + URIUtil.encodePath(projectNameOrGUID) + "/repositories"; //$NON-NLS-1$ //$NON-NLS-2$
            }

            final GetMethod method = new GetMethod(gitURI);

            try {
                final int statusCode = httpClient.executeMethod(method);

                if (statusCode != HttpStatus.SC_OK) {
                    final String messageFormat = Messages.getString("HttpTempDownloader.ServerErrorCodeFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, Integer.toString(statusCode));
                    throw new RuntimeException(message);
                }

                final InputStream input = method.getResponseBodyAsStream();
                gitRepositories = JsonHelper.getObjectMapper().readValue(input, TfsGitRepositoriesJson.class);
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                method.releaseConnection();
            }
        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        } finally {
            progressMonitor.done();
        }

        return Status.OK_STATUS;
    }

    public List<TfsGitRepositoryJson> getRepositories() {
        return gitRepositories.getRepositories();
    }

    @Override
    public String getLoggingDescription() {
        return "QueryGitRepositoriesCommand"; //$NON-NLS-1$
    }
}
