// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.framework.command.CommandFactory;
import com.microsoft.tfs.client.common.git.commands.QueryGitRepositoriesCommand;
import com.microsoft.tfs.client.common.git.commands.QueryGitRepositoryBranchesCommand;
import com.microsoft.tfs.client.common.git.json.TfsGitBranchesJson;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.CredentialsHelper;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitRepository;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.wizard.common.WizardCrossCollectionSelectionPage;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.egit.importwizard.CrossCollectionRepositorySelectControl.RepositorySelectionChangedEvent;
import com.microsoft.tfs.client.eclipse.ui.egit.importwizard.CrossCollectionRepositorySelectControl.RepositorySelectionChangedListener;
import com.microsoft.tfs.client.eclipse.ui.egit.importwizard.CrossCollectionRepositoryTable.CrossCollectionRepositoryInfo;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepository;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepositoryCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemCollectionBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;

public class WizardCrossCollectionRepoSelectionPage extends WizardCrossCollectionSelectionPage {
    private static final Log logger = LogFactory.getLog(WizardCrossCollectionRepoSelectionPage.class);

    public static final String PAGE_NAME = "WizardCrossCollectionRepoSelectionPage"; //$NON-NLS-1$
    public static final String INITIALLY_SELECTED_REPO = "InitiallySelectedRepo"; //$NON-NLS-1$
    public static final String PROTOCOL_HANDLER_REPO = "ProtocolHandlerRepo"; //$NON-NLS-1$

    private boolean cloningHadErrors = false;
    private CrossCollectionRepositorySelectControl repositorySelectControl;
    private final List<CrossCollectionRepositoryInfo> repos = new ArrayList<CrossCollectionRepositoryInfo>(100);

    public WizardCrossCollectionRepoSelectionPage() {
        super(
            PAGE_NAME,
            Messages.getString("WizardCrossCollectionSelectionPage.RepositorySelectionTitle"), //$NON-NLS-1$
            Messages.getString("WizardCrossCollectionSelectionPage.RepositorySelectionDescription")); //$NON-NLS-1$
    }

    @Override
    protected void createControls(
        final Composite container,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags) {
        repositorySelectControl =
            new CrossCollectionRepositorySelectControl(container, SWT.NONE, sourceControlCapabilityFlags);
        repositorySelectControl.addListener(new RepositorySelectionChangedListener() {
            @Override
            public void onRepositorySelectionChanged(final RepositorySelectionChangedEvent event) {
                final CrossCollectionRepositoryInfo info = repositorySelectControl.getSelectedRepository();
                final String workingDirectory = repositorySelectControl.getWorkingDirectory();
                if (info != null) {
                    setWizardData(info.getCollection(), info);
                } else {
                    removeWizardData(false);
                }

                setPageComplete(info != null && !StringUtil.isNullOrEmpty(workingDirectory));
            }
        });
        GridDataBuilder.newInstance().grab().fill().applyTo(repositorySelectControl);
    }

    @Override
    protected boolean onPageFinished() {
        repositorySelectControl.stopTimer();
        final CrossCollectionRepositoryInfo info = repositorySelectControl.getSelectedRepository();
        final String workingDirectory = repositorySelectControl.getWorkingDirectory();
        if (info != null && !StringUtil.isNullOrEmpty(workingDirectory)) {
            final TFSTeamProjectCollection connection = info.getCollection();

            setWizardData(connection, info);

            /* Create PAT for EGit access to VSTS if needed */
            CredentialsHelper.refreshCredentialsForGit(connection);

            final boolean cloningSucceeded = finishPage(info, workingDirectory);
            setPageComplete(cloningSucceeded);

            return cloningSucceeded;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canFlipToNextPage() {
        if (super.canFlipToNextPage()) {
            final CrossCollectionRepositoryInfo info = repositorySelectControl.getSelectedRepository();
            final String workingDirectory = repositorySelectControl.getWorkingDirectory();

            return info != null && !StringUtil.isNullOrEmpty(workingDirectory);

        } else {
            return false;
        }
    }

    @Override
    protected void clearList() {
        repos.clear();
    }

    @Override
    protected void appendCollectionInformation(final TFSTeamProjectCollection collection) {
        final VersionControlClient vcClient;
        try {
            vcClient = collection.getVersionControlClient();
        } catch (final Exception e) {
            // If we could not create a client, ignore this collection and move
            // on
            logger.warn("Failed to get a Version Control Client."); //$NON-NLS-1$
            logger.warn(e);

            return;
        }

        final QueryGitRepositoriesCommand queryCommand3 = new QueryGitRepositoriesCommand(vcClient);
        final IStatus status3 = getCommandExecutor().execute(queryCommand3);

        if (status3.isOK()) {
            final List<TfsGitRepositoryJson> gitRepositories = queryCommand3.getRepositories();

            for (final TfsGitRepositoryJson gitRepository : gitRepositories) {
                final CrossCollectionRepositoryInfo ri = new CrossCollectionRepositoryInfo(collection, gitRepository);
                repos.add(ri);
            }
        }
    }

    @Override
    protected void refreshUI() {
        repositorySelectControl.refresh(repos);

        // Check to see if there was an initially selected repo.
        // If so, select it in the list.
        if (getExtendedWizard().hasPageData(INITIALLY_SELECTED_REPO)) {
            final TypedServerGitRepository selectedRepo =
                (TypedServerGitRepository) getExtendedWizard().getPageData(INITIALLY_SELECTED_REPO);
            for (final CrossCollectionRepositoryInfo r : repos) {
                if (r.getRepositoryGUID().equalsIgnoreCase(selectedRepo.getJson().getId())) {
                    repositorySelectControl.setSelectedRepository(r);
                    // Now that we have selected it, remove the page
                    // data, so that we don't keep trying to select it
                    // as the user moves around
                    getExtendedWizard().setPageData(INITIALLY_SELECTED_REPO, null);
                    break;
                }
            }
        }
    }

    private boolean finishPage(final CrossCollectionRepositoryInfo selectedRepository, final String workingDirectory) {
        if (selectedRepository != null) {
            final ImportOptions options = (ImportOptions) getExtendedWizard().getPageData(ImportOptions.class);
            options.setCredentials(getCredentials(selectedRepository.getCollection()));
            final String projectPath = ServerPath.combine(
                TypedServerItem.ROOT.getServerPath(),
                selectedRepository.getRepository().getTeamProject().getName());
            final String serverPath = ServerPath.combine(projectPath, selectedRepository.getRepository().getName());
            final ImportGitRepository repository = new ImportGitRepository(serverPath);
            repository.setRepositroyJson(selectedRepository.getRepository());
            repository.setWorkingDirectory(workingDirectory);

            // Get the repository branches from the server and set them on the
            // repository object
            final GUID[] repositoryIds = new GUID[1];
            repositoryIds[0] = repository.getId();
            final VersionControlClient vcClient = selectedRepository.getCollection().getVersionControlClient();
            final QueryGitRepositoryBranchesCommand queryCommand =
                new QueryGitRepositoryBranchesCommand(vcClient, repositoryIds);
            final IStatus status = getCommandExecutor().execute(queryCommand);
            if (status.isOK()) {
                final Map<GUID, TfsGitBranchesJson> repositoryBranches = queryCommand.getRepositoryBranches();
                repository.setBranchesJson(repositoryBranches.get(repository.getId()));
            }

            if (getExtendedWizard().hasPageData(PROTOCOL_HANDLER_REPO)) {
                final TypedServerGitRepository initialRepo =
                    (TypedServerGitRepository) getExtendedWizard().getPageData(PROTOCOL_HANDLER_REPO);
                if (selectedRepository.getRepository().getId().equalsIgnoreCase(initialRepo.getJson().getId())) {
                    // At this point the repositories branches are not fully
                    // initialized. We make this call to finish their
                    // initialization, otherwise the default branch setting we
                    // make in the next line will be overridden later.
                    repository.getBranches();
                    repository.setDefaultBranch(initialRepo.getJson().getDefaultBranch());
                }
            }
            doClone(repository, options);

            if (!cloningHadErrors) {
                // Push the selected repository information into the page data
                // in the expected format
                final List<TypedServerItem> items = new ArrayList<TypedServerItem>();
                items.add(new TypedServerGitRepository(serverPath, selectedRepository.getRepository()));
                final ImportGitRepositoryCollection itemCollection =
                    new ImportGitRepositoryCollection(selectedRepository.getCollection(), items);
                itemCollection.getRepositories()[0].setWorkingDirectory(workingDirectory);

                getExtendedWizard().setPageData(ImportItemCollectionBase.class, itemCollection);
            }

            return !cloningHadErrors;
        }

        return false;
    }

    private void doClone(final ImportGitRepository repository, final ImportOptions options) {
        final UsernamePasswordCredentials credentials = options.getCredentials();
        cloningHadErrors = false;

        UIHelpers.syncExec(new Runnable() {
            @Override
            public void run() {
                boolean cancelled = false;
                if (cancelled) {
                    repository.setCloneStatus(ImportGitRepository.CLONE_CANCELLED);
                    cloningHadErrors = true;
                } else if (!ImportGitRepository.CLONE_FINISHED.equals(repository.getCloneStatus())
                    && !ImportGitRepository.CLONE_EMPTY_REPOSITORY.equals(repository.getCloneStatus())) {

                    final IStatus status = cloneRepository(credentials, repository);
                    if (status.getCode() == IStatus.CANCEL) {
                        cancelled = true;
                    } else if (status.getSeverity() != IStatus.OK) {
                        cloningHadErrors = true;
                    }
                }
            }
        });
    }

    private IStatus cloneRepository(
        final UsernamePasswordCredentials credentials,
        final ImportGitRepository repository) {

        setMessage(MessageFormat.format(
            Messages.getString("GitImportWizardClonePage.CloningMessageFormat"), //$NON-NLS-1$
            repository.getName()));
        repository.setRemoteName("origin"); //$NON-NLS-1$
        repository.setCloneStatus(ImportGitRepository.CLONE_IN_PROGRESS);

        final CloneGitRepositoryCommand cloneCommand = createCloneCommand(credentials, repository);

        IStatus status;
        try {
            status = this.getCommandExecutor().execute(CommandFactory.newCancelableCommand(cloneCommand));

            if (status.getSeverity() == IStatus.CANCEL) {
                repository.setCloneStatus(ImportGitRepository.CLONE_CANCELLED);
                setMessage(
                    Messages.getString("GitImportWizardClonePage.CloneCancelledMessageText"), //$NON-NLS-1$
                    IMessageProvider.WARNING);
            } else if (status.getSeverity() == IStatus.ERROR) {
                repository.setCloneStatus(ImportGitRepository.CLONE_ERROR);
                setMessage(MessageFormat.format(
                    Messages.getString("GitImportWizardClonePage.CloneFailedMessageFormat"), //$NON-NLS-1$
                    status.getMessage()), IMessageProvider.ERROR);
            } else {
                if (repository.getBranches() == null || repository.getBranches().length == 0) {
                    repository.setCloneStatus(ImportGitRepository.CLONE_EMPTY_REPOSITORY);
                } else {
                    repository.setCloneStatus(ImportGitRepository.CLONE_FINISHED);
                }
                setMessage(null);
            }
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            repository.setCloneStatus(ImportGitRepository.CLONE_ERROR);
            setMessage(e.getLocalizedMessage(), IMessageProvider.ERROR);
            status = new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        }

        return status;
    }

    private CloneGitRepositoryCommand createCloneCommand(
        final UsernamePasswordCredentials credentials,
        final ImportGitRepository repository) {
        final CloneGitRepositoryCommand cloneCommand = new CloneGitRepositoryCommand(
            credentials,
            repository.getRemoteUrl(),
            repository.getName(),
            repository.getRefs(),
            repository.getDefaultRef(),
            repository.getWorkingDirectory(),
            repository.getCloneSubmodules(),
            repository.getRemoteName());

        return cloneCommand;
    }

    private UsernamePasswordCredentials getCredentials(final TFSConnection connection) {
        final CredentialsManager credentialsManager = EclipseCredentialsManagerFactory.getGitCredentialsManager();
        Credentials savedCredentials = null;

        CachedCredentials cachedCredentials = credentialsManager.getCredentials(connection.getBaseURI());
        if (cachedCredentials != null) {
            savedCredentials = cachedCredentials.toCredentials();
            if (savedCredentials != null && savedCredentials instanceof UsernamePasswordCredentials) {
                return (UsernamePasswordCredentials) savedCredentials;
            }
        }

        if (connection.getCredentials() != null
            && !connection.isHosted()
            && (connection.getCredentials() instanceof DefaultNTCredentials)) {
            // That's possible only in case of NTLM on Windows platform.
            return null;
        }

        // Prompt for credentials providing to the dialog any credentials
        // information we've found so far.
        final CredentialsDialog credentialsDialog = new CredentialsDialog(getShell(), connection.getBaseURI());
        credentialsDialog.setCredentials(connection.getCredentials());

        if (credentialsDialog.open() == IDialogConstants.OK_ID) {
            final UsernamePasswordCredentials credentials =
                (UsernamePasswordCredentials) credentialsDialog.getCredentials();

            cachedCredentials =
                new CachedCredentials(connection.getBaseURI(), credentials.getUsername(), credentials.getPassword());
            credentialsManager.setCredentials(cachedCredentials);
            return credentials;
        }

        // The user has cancelled the dialog.
        return null;
    }
}
