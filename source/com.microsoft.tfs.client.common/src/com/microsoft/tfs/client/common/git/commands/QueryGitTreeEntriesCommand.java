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
import com.microsoft.tfs.client.common.git.json.JsonHelper;
import com.microsoft.tfs.client.common.git.json.TfsGitTreeEntriesJson;
import com.microsoft.tfs.client.common.git.json.TfsGitTreeEntryJson;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.tasks.CanceledException;

public class QueryGitTreeEntriesCommand extends TFSCommand {

    private final VersionControlClient vcClient;
    private final GUID repositoryId;
    private final String objectId;
    private TfsGitTreeEntriesJson treeEntries;

    public QueryGitTreeEntriesCommand(
        final VersionControlClient vcClient,
        final GUID repositoryId,
        final String objectId) {
        Check.notNull(vcClient, "vcClient"); //$NON-NLS-1$
        Check.notNull(repositoryId, "repositoryId"); //$NON-NLS-1$
        Check.notNull(objectId, "objectId"); //$NON-NLS-1$

        this.vcClient = vcClient;
        this.repositoryId = repositoryId;
        this.objectId = objectId;
    }

    @Override
    public String getName() {
        return Messages.getString("QueryGitTreeEntriesCommand.CommandName"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("QueryGitTreeEntriesCommand.CommandErrorDescription"); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

        final HttpClient httpClient = vcClient.getConnection().getHTTPClient();
        final URI baseURI = vcClient.getConnection().getBaseURI();
        final String gitURI =
            baseURI.toString() + "_apis/git/repositories/" + repositoryId.toString() + "/trees/" + objectId; //$NON-NLS-1$ //$NON-NLS-2$

        final GetMethod method = new GetMethod(gitURI);

        try {
            final int statusCode = httpClient.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                final String messageFormat = Messages.getString("HttpTempDownloader.ServerErrorCodeFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, Integer.toString(statusCode));
                throw new RuntimeException(message);
            }

            final InputStream input = method.getResponseBodyAsStream();
            treeEntries = JsonHelper.getObjectMapper().readValue(input, TfsGitTreeEntriesJson.class);
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

    public List<TfsGitTreeEntryJson> getTreeEntries() {
        return treeEntries.getTreeEntries();
    }

    @Override
    public String getLoggingDescription() {
        return "QueryGitTreeEntries"; //$NON-NLS-1$
    }
}
