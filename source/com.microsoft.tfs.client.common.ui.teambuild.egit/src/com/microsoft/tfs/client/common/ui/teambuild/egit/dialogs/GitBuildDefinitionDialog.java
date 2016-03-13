// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.op.AddToIndexOperation;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.actions.ActionCommands;
import org.eclipse.egit.ui.internal.commit.CommitUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.resources.LocationInfo;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ServerItemTreeDialog;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.BuildDefinitionTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.ProjectFileTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.BuildDefinitionDialog;
import com.microsoft.tfs.client.common.ui.teambuild.egit.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.egit.repositories.GitRepositoriesMap;
import com.microsoft.tfs.client.common.ui.teambuild.egit.repositories.GitRepository;
import com.microsoft.tfs.client.common.ui.teambuild.egit.serveritem.GitFolderSource;
import com.microsoft.tfs.client.common.ui.teambuild.wizards.CreateGitBuildConfigurationWizard;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.GitProperties;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

/**
 * Show the edit TFGit build definition dialog.
 */
public class GitBuildDefinitionDialog extends BuildDefinitionDialog {
    private static final Log log = LogFactory.getLog(GitBuildDefinitionDialog.class);

    private GitSourceSettingsTabPage sourceSettingsTabPage;
    private GitProjectFileTabPage projectFileTabPage;

    private final IBuildServer buildServer;
    private final IBuildDefinition buildDefinition;
    private final String teamProjectName;

    private boolean localBuildProjectCreated = false;
    private boolean localCopyExists = false;
    private GitRepositoriesMap repositoriesMap;

    public GitBuildDefinitionDialog(final Shell parentShell, final IBuildDefinition buildDefinition) {
        super(parentShell, buildDefinition);
        Check.notNull(buildDefinition, "buildDefinition"); //$NON-NLS-1$

        this.buildDefinition = buildDefinition;
        this.buildServer = buildDefinition.getBuildServer();
        this.teamProjectName = buildDefinition.getTeamProject();
    }

    @Override
    protected BuildDefinitionTabPage getSourceSettingsTabPage(final IBuildDefinition buildDefinition) {
        sourceSettingsTabPage = new GitSourceSettingsTabPage(buildDefinition);
        return sourceSettingsTabPage;
    }

    @Override
    protected ProjectFileTabPage getProjectFileTabPage(final IBuildDefinition buildDefinition) {
        projectFileTabPage = new GitProjectFileTabPage(buildDefinition);
        return projectFileTabPage;
    }

    @Override
    protected SelectionListener getBrowseButtonSelectionListener(final Shell shell) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final ServerItemSource serverItemSource =
                    new GitFolderSource(buildServer.getConnection(), getRepositoriesMap());

                String initialPath = projectFileTabPage.getControl().getConfigFolderText().getText();
                if (initialPath.startsWith(GitProperties.GitPathBeginning)) {
                    initialPath =
                        initialPath.replace(GitProperties.GitPathBeginning, TypedServerItem.ROOT.getServerPath());
                }

                final ServerItemTreeDialog dialog = new ServerItemTreeDialog(
                    projectFileTabPage.getControl().getShell(),
                    Messages.getString("GitBuildDefinitionDialog.BrowseDialogTitle"), //$NON-NLS-1$
                    initialPath,
                    serverItemSource,
                    ServerItemType.ALL_FOLDERS_AND_GIT);

                if (IDialogConstants.OK_ID == dialog.open()) {
                    final ServerItemType selectedItemType = dialog.getSelectedItem().getType();

                    if (selectedItemType == ServerItemType.GIT_BRANCH || selectedItemType == ServerItemType.FOLDER) {
                        final String gitURI = dialog.getSelectedServerPath().replace(
                            TypedServerItem.ROOT.getServerPath(),
                            GitProperties.GitPathBeginning);

                        projectFileTabPage.getControl().getConfigFolderText().setText(gitURI);

                        checkForBuildFileExistence(false);
                        validate();
                    }
                }
            }
        };
    }

    @Override
    protected SelectionListener getCreateButtonSelectionListener(final Shell shell) {
        return new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final CreateGitBuildConfigurationWizard wizard = new CreateGitBuildConfigurationWizard();
                updateAndVerifyBuildDefinition(true);

                wizard.init(buildDefinition);

                final WizardDialog dialog = new WizardDialog(getShell(), wizard);
                final int rc = dialog.open();

                if (rc == IDialogConstants.OK_ID) {
                    localBuildProjectCreated = true;

                    /*
                     * TFSBuild proj/resp files are created in a cloned
                     * repository, but they could be outside of projects
                     * imported into the Eclipse workspace.
                     */
                    final List<IResource> resources = new ArrayList<IResource>();

                    final LocationInfo buildProjectLocation = new LocationInfo(new Path(getBuildProjectAbsolutePath()));
                    if (buildProjectLocation.isPotentialSubProjectResource()) {
                        resources.add(buildProjectLocation.getSubProjectResource(false));
                    }

                    final LocationInfo buildResponceLocation =
                        new LocationInfo(new Path(getBuildResponseAbsolutePath()));
                    if (buildResponceLocation.isPotentialSubProjectResource()) {
                        resources.add(buildResponceLocation.getSubProjectResource(false));
                    }

                    if (resources.size() > 0) {
                        final AddToIndexOperation op = new AddToIndexOperation(resources);
                        try {
                            op.execute(new NullProgressMonitor());
                        } catch (final CoreException ex) {
                            log.error("Error adding build files to index", ex); //$NON-NLS-1$
                        }

                    } else {
                        final GitRepository repository = getBuildProjectRepository();
                        final Git git = new Git(repository.getLocalRepository());

                        final AddCommand addCommand = git.add();
                        addCommand.addFilepattern(getBuildProjectRelativePath());
                        addCommand.addFilepattern(getBuildResponseRelativePath());

                        try {
                            addCommand.call();
                        } catch (final Exception ex) {
                            log.error("Error adding build files to index", ex); //$NON-NLS-1$
                        }
                    }

                    checkForBuildFileExistence(true);
                    validate();
                }
            }
        };
    }

    @Override
    protected String getDefaultBuildFileLocation(final String newName) {
        return GitProperties.createGitItemUrl(
            buildDefinition.getTeamProject(),
            sourceSettingsTabPage.getRepo(),
            sourceSettingsTabPage.getShortBranchName(),
            newName);
    }

    @Override
    protected boolean searchForBuildFile(final boolean forceCheckForBuildFile) {
        if (super.searchForBuildFile(forceCheckForBuildFile)) {
            return true;
        } else {
            final GitProjectFileControl gitProjectFileControl = projectFileTabPage.getControl();
            final String buildFileLocation = gitProjectFileControl.getConfigFolderText().getText();

            final GitRepositoriesMap repositoriesMap = getRepositoriesMap();
            final List<GitRepository> mappedRepositories = repositoriesMap.getMappedRepositories();

            gitProjectFileControl.clearProjectFileStatus();

            final AtomicReference<String> projectName = new AtomicReference<String>();
            final AtomicReference<String> repositoryName = new AtomicReference<String>();
            final AtomicReference<String> branchName = new AtomicReference<String>();
            final AtomicReference<String> path = new AtomicReference<String>();

            if (GitProperties.parseGitItemUrl(buildFileLocation, projectName, repositoryName, branchName, path)
                && projectName.get().equalsIgnoreCase(teamProjectName)) {
                for (final GitRepository repository : mappedRepositories) {
                    if (repository.getName().equalsIgnoreCase(repositoryName.get())) {
                        gitProjectFileControl.setRepositoryCloned(true);

                        if (branchName.get().equalsIgnoreCase(repository.getCurrentBranch().getRemoteName())) {
                            gitProjectFileControl.setBranchCheckedOut(true);

                            final String configFolderLocalPath =
                                LocalPath.combine(repository.getWorkingDirectoryPath(), path.get());

                            localCopyExists = LocalPath.exists(
                                LocalPath.combine(configFolderLocalPath, BuildConstants.PROJECT_FILE_NAME));

                            gitProjectFileControl.setLocalCopyExists(localCopyExists);
                        }

                        break;
                    }
                }
            }

            return false;
        }
    }

    @Override
    protected boolean updateSourceSettingAndProjectFiles() {
        sourceSettingsTabPage.updateSourceProvider(buildDefinition);
        if (isMSBuildBasedBuild()) {
            projectFileTabPage.updateConfigurationFolderPath(buildDefinition, projectFileTabPage.getConfigFolderText());
        }

        return true;
    }

    private GitRepositoriesMap getRepositoriesMap() {
        if (repositoriesMap == null) {
            repositoriesMap = sourceSettingsTabPage.getControl().getRepositoriesMap();
        }

        return repositoriesMap;
    }

    @Override
    public void commitChangesIfNeeded() {
        if (isCommitDialogNeeded()) {
            /*
             * TFSBuild proj/resp files are created in a cloned repository, but
             * they could be outside of projects imported into the Eclipse
             * workspace. In this case we cannot preselect the files in the
             * commit dialog that requires Eclipse resource objects as
             * parameters.
             */
            final List<IResource> resources = new ArrayList<IResource>();

            final LocationInfo buildProjectLocation = new LocationInfo(new Path(getBuildProjectAbsolutePath()));
            if (buildProjectLocation.isPotentialSubProjectResource()) {
                resources.add(buildProjectLocation.getSubProjectResource(false));
            }

            final LocationInfo buildResponceLocation = new LocationInfo(new Path(getBuildResponseAbsolutePath()));
            if (buildResponceLocation.isPotentialSubProjectResource()) {
                resources.add(buildResponceLocation.getSubProjectResource(false));
            }

            if (resources.size() > 0) {
                CommonUtils.runCommand(ActionCommands.COMMIT_ACTION, new StructuredSelection(resources));
            } else {
                final GitRepository repository = getBuildProjectRepository();
                final CommitUI commitUi =
                    new CommitUI(getShell(), repository.getLocalRepository(), new IResource[0], true);
                commitUi.commit();
            }
        }
    }

    private GitRepository getBuildProjectRepository() {
        final GitRepositoriesMap repositoriesMap = getRepositoriesMap();
        final List<GitRepository> repositories = repositoriesMap.getServerRepositories();

        final AtomicReference<String> repositoryName = new AtomicReference<String>();

        final String buildFileLocation = buildDefinition.getConfigurationFolderPath();

        if (GitProperties.parseGitItemUrl(buildFileLocation, null, repositoryName, null, null)) {

            for (final GitRepository repository : repositories) {
                if (repository.getName().equalsIgnoreCase(repositoryName.get())) {
                    return repository;
                }
            }
        }

        return null;
    }

    private String getBuildProjectRelativePath() {
        final String buildFileLocation = buildDefinition.getConfigurationFolderPath();
        final AtomicReference<String> path = new AtomicReference<String>();

        if (GitProperties.parseGitItemUrl(buildFileLocation, null, null, null, path)) {
            return path.get() + GitProperties.PathSeparator + BuildConstants.PROJECT_FILE_NAME;
        }

        return null;
    }

    private String getBuildProjectAbsolutePath() {
        final String relativePath = getBuildProjectRelativePath();

        if (!StringUtil.isNullOrEmpty(relativePath)) {
            final GitRepository repository = getBuildProjectRepository();
            return LocalPath.combine(repository.getWorkingDirectoryPath(), relativePath);
        }

        return null;
    }

    private String getBuildResponseRelativePath() {
        final String buildFileLocation = buildDefinition.getConfigurationFolderPath();
        final AtomicReference<String> path = new AtomicReference<String>();

        if (GitProperties.parseGitItemUrl(buildFileLocation, null, null, null, path)) {
            return path.get() + GitProperties.PathSeparator + BuildConstants.RESPONSE_FILE_NAME;
        }

        return null;
    }

    private String getBuildResponseAbsolutePath() {
        final String relativePath = getBuildResponseRelativePath();

        if (!StringUtil.isNullOrEmpty(relativePath)) {
            final GitRepository repository = getBuildProjectRepository();
            return LocalPath.combine(repository.getWorkingDirectoryPath(), relativePath);
        }

        return null;
    }

    private boolean isCommitDialogNeeded() {
        return localBuildProjectCreated && localCopyExists;
    }

}
