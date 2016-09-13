// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.protocolhandler;

import java.net.URI;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.alm.teamfoundation.core.webapi.TeamProjectReference;
import com.microsoft.alm.teamfoundation.sourcecontrol.webapi.GitRepository;
import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.client.common.git.json.TfsGitTeamProjectJson;
import com.microsoft.tfs.client.common.ui.protocolhandler.ProtocolHandler;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitRepository;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.eclipse.ui.egit.importwizard.GitImportWizard;
import com.microsoft.tfs.client.eclipse.ui.egit.importwizard.WizardCrossCollectionRepoSelectionPage;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Platform;

public class ProtocolHandlerHelpers {

    private static final Log log = LogFactory.getLog(ProtocolHandlerHelpers.class);

    final Shell shell;
    final TeamExplorerContext context;

    Credentials credentials = null;

    private ProtocolHandlerHelpers(final Shell shell, final TeamExplorerContext context) {
        this.shell = shell;
        this.context = context;
    }

    public static void clone(final Shell shell, final TeamExplorerContext context) {

        if (ProtocolHandler.getInstance().hasProtocolHandlerRequest()) {
            final ProtocolHandlerHelpers handler = new ProtocolHandlerHelpers(shell, context);
            handler.cloneRepo();
        }
    }

    private void cloneRepo() {

        final String repoUrl = ProtocolHandler.getInstance().getProtocolHandlerCloneUrl();
        final String branch = ProtocolHandler.getInstance().getProtocolHandlerBranch();

        credentials = getAccountCredentials(repoUrl);

        final VstsInfo vstsInfo = getServerRepositroyInfo(repoUrl);
        final TypedServerGitRepository typedRepo = getSelectedRepository(vstsInfo, branch);

        final GitImportWizard wizard = new GitImportWizard(Arrays.asList(new TypedServerItem[] {
            typedRepo
        }));

        wizard.init(PlatformUI.getWorkbench(), null);
        wizard.setPageData(WizardCrossCollectionRepoSelectionPage.INITIALLY_SELECTED_REPO, typedRepo);
        wizard.setPageData(WizardCrossCollectionRepoSelectionPage.PROTOCOL_HANDLER_REPO, typedRepo);
        final WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.open();

    }

    private VstsInfo getServerRepositroyInfo(final String repoUrl) {
        final VstsInfoHttpClient client = new VstsInfoHttpClient(new VstsInfoClientHandler(repoUrl, credentials));
        final VstsInfo info = client.getServerRepositoryInfo(repoUrl);

        return info;
    }

    private TypedServerGitRepository getSelectedRepository(final VstsInfo info, final String branch) {
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

    private Credentials getAccountCredentials(final String repoUrl) {

        final URI hostUrl = URIUtils.removePathAndQueryParts(URIUtils.newURI(repoUrl));

        final CredentialsManager credentialsManager =
            EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);
        final CachedCredentials cachedCredentials = credentialsManager.getCredentials(hostUrl);

        if (cachedCredentials != null) {
            return cachedCredentials.toPreemptiveCredentials();
        } else {
            /*
             * For on-premises servers, simply use empty
             * UsernamePasswordCredentials (to force a username/password
             * dialog.) For hosted servers, use default NT credentials at all
             * (to avoid the username/password dialog.)
             */
            return ServerURIUtils.isHosted(repoUrl) || Platform.isCurrentPlatform(Platform.WINDOWS)
                ? new DefaultNTCredentials() : new UsernamePasswordCredentials("", null); //$NON-NLS-1$
        }
    }
}
