// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.protocolhandler;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.alm.client.VstsInfoClientHandler;
import com.microsoft.alm.client.VstsInfoHttpClient;
import com.microsoft.alm.teamfoundation.core.webapi.TeamProjectReference;
import com.microsoft.alm.teamfoundation.sourcecontrol.webapi.GitRepository;
import com.microsoft.alm.teamfoundation.sourcecontrol.webapi.VstsInfo;
import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.client.common.git.json.TfsGitTeamProjectJson;
import com.microsoft.tfs.client.common.ui.protocolhandler.ProtocolHandler;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitRepository;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

public class ProtocolHandlerHelpers {

    private static final Log log = LogFactory.getLog(ProtocolHandlerHelpers.class);

    final TeamExplorerContext context;

    public ProtocolHandlerHelpers(final TeamExplorerContext context) {
        this.context = context;
    }

    public TypedServerGitRepository getImportWizardInput() {
        final String serverUrl = ProtocolHandler.getInstance().getProtocolHandlerServerUrl();
        final String repoUrl = ProtocolHandler.getInstance().getProtocolHandlerCloneUrl();
        final String branch = ProtocolHandler.getInstance().getProtocolHandlerBranch();

        final VstsInfoHttpClient client = getVstsInfoClient(serverUrl);
        final VstsInfo vstsInfo = client.getServerRepositoryInfo(repoUrl);
        final TypedServerGitRepository typedRepo = getSelectedRepository(vstsInfo, branch);

        return typedRepo;
    }

    private VstsInfoHttpClient getVstsInfoClient(final String serverUrl) {

        Check.isTrue(context.isConnectedToCollection(), "disconnected context in Team Explorer"); //$NON-NLS-1$

        final TFSTeamProjectCollection collection = context.getServer().getConnection();
        final VstsInfoClientHandler handler = new VstsInfoClientHandler(collection.getHTTPClient());
        final VstsInfoHttpClient client = new VstsInfoHttpClient(handler);

        return client;
    }

    static TypedServerGitRepository getSelectedRepository(final VstsInfo info, final String branch) {
        Check.notNull(branch, "branch"); //$NON-NLS-1$

        final GitRepository repository = info.getRepository();
        final TeamProjectReference project = repository.getProject();

        final String projectPath = ServerPath.combine(ServerPath.ROOT, project.getName());
        final String serverPath = ServerPath.combine(projectPath, repository.getName());

        final TfsGitTeamProjectJson jsonProject =
            new TfsGitTeamProjectJson(project.getId().toString(), project.getName());

        final TfsGitRepositoryJson jsonRepo = new TfsGitRepositoryJson(
            repository.getId().toString(),
            repository.getName(),
            jsonProject,
            branch,
            repository.getRemoteUrl());

        final TypedServerGitRepository typedRepo = new TypedServerGitRepository(serverPath, jsonRepo);

        return typedRepo;

    }

    private Credentials getAccountCredentials(final String serverUrl) {

        final URI serverUri = URIUtils.newURI(serverUrl);

        final CredentialsManager credentialsManager =
            EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);
        final CachedCredentials cachedCredentials = credentialsManager.getCredentials(serverUri);

        if (cachedCredentials != null) {
            return cachedCredentials.toPreemptiveCredentials();
        } else {
            /*
             * For on-premises servers, simply use empty
             * UsernamePasswordCredentials (to force a username/password
             * dialog.) For hosted servers, use default NT credentials at all
             * (to avoid the username/password dialog.)
             */
            return ServerURIUtils.isHosted(serverUri) || Platform.isCurrentPlatform(Platform.WINDOWS)
                ? new DefaultNTCredentials() : new UsernamePasswordCredentials("", null); //$NON-NLS-1$
        }
    }
}
