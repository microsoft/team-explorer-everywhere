// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git.commands;

import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.git.json.JsonHelper;
import com.microsoft.tfs.client.common.git.json.TfsGitItemJson;
import com.microsoft.tfs.client.common.git.json.TfsGitItemsJson;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.URIException;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.core.httpclient.util.URIUtil;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public class QueryGitItemsCommand extends TFSCommand {

    private final VersionControlClient vcClient;
    private final GUID repositoryId;
    private final String projectName;
    private final String repositoryName;
    private final String branchName;
    private final String path;
    private TfsGitItemsJson repositoryItems;
    private TfsGitItemJson repositoryItem;

    public QueryGitItemsCommand(final VersionControlClient vcClient, final GUID repositoryId, final String branchName) {
        this(vcClient, repositoryId, branchName, null);
    }

    public QueryGitItemsCommand(
        final VersionControlClient vcClient,
        final GUID repositoryId,
        final String branchName,
        final String path) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNull(repositoryId, "repositoryId"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.repositoryId = repositoryId;
        this.branchName = branchName;
        this.path = path;
        this.projectName = null;
        this.repositoryName = null;
    }

    public QueryGitItemsCommand(
        final VersionControlClient vcClient,
        final String projectName,
        final String repositoryName,
        final String branchName,
        final String path) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNullOrEmpty(repositoryName, "repositoryName"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.repositoryId = null;
        this.branchName = branchName;
        this.path = path;
        this.projectName = projectName;
        this.repositoryName = repositoryName;
    }

    @Override
    public String getName() {
        return Messages.getString("QueryGitItemsCommand.CommandName"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("QueryGitItemsCommand.CommandErrorDescription"); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

        final HttpClient httpClient = vcClient.getConnection().getHTTPClient();
        final String gitURI = buildURI();

        final GetMethod method = new GetMethod(gitURI);
        method.setRequestHeader("Accept", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            final int statusCode = httpClient.executeMethod(method);

            if (statusCode == HttpStatus.SC_OK) {
                final InputStream input = method.getResponseBodyAsStream();
                if (StringUtil.isNullOrEmpty(path)) {
                    repositoryItems = JsonHelper.getObjectMapper().readValue(input, TfsGitItemsJson.class);
                    repositoryItem = null;
                } else {
                    repositoryItems = null;
                    repositoryItem = JsonHelper.getObjectMapper().readValue(input, TfsGitItemJson.class);
                }
            }
            /*
             * In case the item is not found we expect 404, but current server
             * implementation could return 400 and 500 as well
             */
            else if (statusCode == HttpStatus.SC_NOT_FOUND
                || statusCode == HttpStatus.SC_BAD_REQUEST
                || statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                repositoryItems = null;
                repositoryItem = null;
            } else {
                final String messageFormat = Messages.getString("HttpTempDownloader.ServerErrorCodeFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, Integer.toString(statusCode));
                throw new RuntimeException(message);
            }
        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            method.releaseConnection();
            progressMonitor.done();
        }

        return Status.OK_STATUS;
    }

    public List<TfsGitItemJson> getRepositoryItems() {
        if (repositoryItems != null) {
            return repositoryItems.getItems();
        }

        if (repositoryItem != null) {
            return Arrays.asList(new TfsGitItemJson[] {
                repositoryItem
            });
        }

        return new ArrayList<TfsGitItemJson>();
    }

    @Override
    public String getLoggingDescription() {
        return "QueryGitItems"; //$NON-NLS-1$
    }

    private String buildURI() throws URIException {
        final URI baseURI = vcClient.getConnection().getBaseURI();

        final StringBuilder sb = new StringBuilder(baseURI.toString());

        sb.append("_apis/git"); //$NON-NLS-1$

        if (projectName != null) {
            sb.append(ServerPath.PREFERRED_SEPARATOR_CHARACTER);
            sb.append(URIUtil.encodePath(projectName));
        }

        sb.append("/repositories"); //$NON-NLS-1$

        if (repositoryId != null) {
            sb.append(ServerPath.PREFERRED_SEPARATOR_CHARACTER);
            sb.append(repositoryId);
        } else if (repositoryName != null) {
            sb.append(ServerPath.PREFERRED_SEPARATOR_CHARACTER);
            sb.append(URIUtil.encodePath(repositoryName));
        }

        sb.append("/items"); //$NON-NLS-1$

        if (!StringUtil.isNullOrEmpty(path)) {
            if (path.charAt(0) != ServerPath.PREFERRED_SEPARATOR_CHARACTER) {
                sb.append(ServerPath.PREFERRED_SEPARATOR_CHARACTER);
            }

            sb.append(URIUtil.encodePath(path));
        }

        if (!StringUtil.isNullOrEmpty(branchName)) {
            sb.append("?version="); //$NON-NLS-1$
            sb.append(branchName);
        }

        return sb.toString();
    }
}
