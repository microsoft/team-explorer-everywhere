// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.teamexplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.ui.internal.repository.RepositoriesView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.git.commands.QueryGitRepositoriesCommand;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.client.common.git.utils.GitHelpers;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WebAccessHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerBaseSection;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitRepository;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.egit.importwizard.GitImportWizard;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.util.URLEncode;

public class TeamExplorerGitRepositoriesSection extends TeamExplorerBaseSection {
    private static final Log log = LogFactory.getLog(TeamExplorerGitRepositoriesSection.class);
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
    private static final String REMOTES_SECTION_NAME = "remote"; //$NON-NLS-1$
    private static final String URL_VALUE_NAME = "url"; //$NON-NLS-1$
    private Project project;
    private TeamExplorerContext context;
    private TreeViewer treeViewer;
    private Map<String, List<TfsGitRepositoryJson>> projectRepositories = null;

    // RemoteUrl as key
    private Map<String, Repository> repositoryMap = null;
    private final ICommandExecutor commandExecutor = new CommandExecutor();

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        this.context = context;
        return true;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        this.context = context;
        this.project = context.getCurrentProject();
        if (project == null) {
            return;
        }
        readRepositoriesInfo(context.getDefaultRepository().getVersionControlClient());
        loadRegisteredRepositories();
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context, final Object state) {
        initialize(monitor, context);
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 1, true, 0, 5);

        if (context.isConnected()) {
            treeViewer = new TreeViewer(composite, SWT.SINGLE | SWT.NO_SCROLL);
            treeViewer.setContentProvider(new GitItemContentProvider());
            treeViewer.setLabelProvider(new GitItemLabelProvider());
            treeViewer.addDoubleClickListener(new GitItemDoubleClickListener());
            treeViewer.setInput(projectRepositories);
            treeViewer.setExpandedElements(projectRepositories.keySet().toArray());
            GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(treeViewer.getControl());
            addMenuItems();
        } else {
            createDisconnectedContent(toolkit, composite);
        }

        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
            }
        });

        return composite;
    }

    private void readRepositoriesInfo(final VersionControlClient vcClient) {
        if (projectRepositories == null && GitHelpers.isEGitInstalled(true)) {
            projectRepositories = new TreeMap<String, List<TfsGitRepositoryJson>>();
            final QueryGitRepositoriesCommand queryCommand =
                new QueryGitRepositoriesCommand(vcClient, project.getName());
            final IStatus status = commandExecutor.execute(queryCommand);

            if (status.isOK()) {
                final List<TfsGitRepositoryJson> gitRepositories = queryCommand.getRepositories();

                for (final TfsGitRepositoryJson gitRepository : gitRepositories) {
                    final String projectName = gitRepository.getTeamProject().getName();
                    List<TfsGitRepositoryJson> list = projectRepositories.get(projectName);

                    if (list == null) {
                        list = new ArrayList<TfsGitRepositoryJson>();
                        projectRepositories.put(projectName, list);
                    }

                    list.add(gitRepository);
                }

                final List<TfsGitRepositoryJson> repoList = projectRepositories.get(project.getName());
                Collections.sort(repoList, new Comparator<TfsGitRepositoryJson>() {
                    @Override
                    public int compare(final TfsGitRepositoryJson j1, final TfsGitRepositoryJson j2) {
                        return j1.getName().compareTo(j2.getName());
                    }
                });
            }
        }
    }

    public void addMenuItems() {
        final MenuManager popupMenuManager = new MenuManager("#popup"); //$NON-NLS-1$
        final Menu menu = popupMenuManager.createContextMenu(treeViewer.getControl());
        popupMenuManager.setRemoveAllWhenShown(true);

        popupMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                if (treeViewer.getSelection().isEmpty()) {
                    return;
                }

                if (treeViewer.getSelection() instanceof IStructuredSelection) {
                    final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
                    final Object[] nodes = selection.toArray();
                    final Object firstNode = selection.getFirstElement();
                    if (firstNode instanceof TfsGitRepositoryJson) {
                        final TfsGitRepositoryJson repoJson = (TfsGitRepositoryJson) firstNode;
                        manager.add(new OpenInEGitAction(repoJson));
                        manager.add(new OpenInWebAction(context, repoJson));
                        manager.add(new Separator());
                        manager.add(new GitWizardAction(nodes));
                        manager.add(new Separator());
                        manager.add(
                            new CopyAction(
                                Messages.getString("TeamExplorerGitRepositoriesSection.CopyCloneUrlAction"), //$NON-NLS-1$
                                treeViewer.getControl().getDisplay(),
                                repoJson.getRemoteUrl(),
                                true));
                    }
                }
            }
        });

        treeViewer.getControl().setMenu(menu);
    }

    private void loadRegisteredRepositories() {
        repositoryMap = new TreeMap<String, Repository>();
        final List<String> repositoryFolders = Activator.getDefault().getRepositoryUtil().getConfiguredRepositories();

        for (final String repositoryFolder : repositoryFolders) {
            final File folder = new File(repositoryFolder);
            if (!folder.exists() || !folder.isDirectory()) {
                continue;
            }

            if (!folder.getName().equals(Constants.DOT_GIT) || !FileKey.isGitRepository(folder, FS.DETECTED)) {
                continue;
            }

            final RepositoryBuilder rb = new RepositoryBuilder().setGitDir(folder).setMustExist(true);

            try {
                final Repository repo = rb.build();
                final StoredConfig repositoryConfig = repo.getConfig();
                final Set<String> remotes = repositoryConfig.getSubsections(REMOTES_SECTION_NAME);

                for (final String remoteName : remotes) {
                    final String remoteURL =
                        repositoryConfig.getString(REMOTES_SECTION_NAME, remoteName, URL_VALUE_NAME);
                    repositoryMap.put(remoteURL, repo);
                }
            } catch (final Exception e) {
                log.error("Error loading local Git repository " + repositoryFolder, e); //$NON-NLS-1$
                continue;
            }
        }
    }

    private class GitItemContentProvider extends TreeContentProvider {
        @Override
        public Object[] getElements(final Object inputElement) {
            if (projectRepositories != null) {
                return projectRepositories.keySet().toArray();
            } else {
                return null;
            }
        }

        @Override
        public Object[] getChildren(final Object parentElement) {
            if (parentElement instanceof String) {
                return projectRepositories.get(parentElement).toArray();
            } else {
                return null;
            }
        }

        @Override
        public boolean hasChildren(final Object element) {
            if (element instanceof String) {
                return projectRepositories.get(element).size() > 0;
            } else {
                return false;
            }
        }
    }

    private class GitItemLabelProvider extends LabelProvider {
        @Override
        public String getText(final Object element) {
            if (element instanceof String) {
                return (String) element;
            } else if (element instanceof TfsGitRepositoryJson) {
                return ((TfsGitRepositoryJson) element).getName();
            } else {
                return null;
            }
        }

        @Override
        public Image getImage(final Object element) {
            if (element instanceof String) {
                return imageHelper.getImage(TFSCommonUIClientPlugin.PLUGIN_ID, "images/common/tfs_project.png"); //$NON-NLS-1$
            } else if (element instanceof TfsGitRepositoryJson) {
                return imageHelper.getImage(TFSCommonUIClientPlugin.PLUGIN_ID, "images/common/git_repo.png"); //$NON-NLS-1$
            } else {
                return null;
            }
        }
    }

    private class OpenInEGitAction extends Action {
        private final String remoteUrl;

        public OpenInEGitAction(final TfsGitRepositoryJson json) {
            remoteUrl = URLEncode.encode(json.getRemoteUrl());
            setText(Messages.getString("TeamExplorerGitRepositoriesSection.OpenInEGitActionName")); //$NON-NLS-1$
        }

        @Override
        public boolean isEnabled() {
            return TeamExplorerHelpers.supportsGit(context)
                && GitHelpers.isEGitInstalled(false)
                && repositoryMap.containsKey(remoteUrl);
        }

        @Override
        public void run() {
            openInEGit(remoteUrl);
        }
    }

    private class GitWizardAction extends Action {
        private List<String> remoteUrls;
        private List<TypedServerItem> serverItems;

        public GitWizardAction(final Object[] repos) {
            setText(Messages.getString("TeamExplorerGitRepositoriesSection.ImportRepositoryActionName")); //$NON-NLS-1$
            extractGitRepos(repos);
        }

        @Override
        public boolean isEnabled() {
            if (!TeamExplorerHelpers.supportsGit(context)) {
                return false;
            }

            // no repo selected
            if (remoteUrls == null || remoteUrls.isEmpty()) {
                return false;
            }

            for (final String url : remoteUrls) {
                if (repositoryMap.containsKey(url)) {
                    // repository is already cloned
                    return false;
                }
            }

            return true;
        }

        @Override
        public void run() {
            try {
                if (!GitHelpers.isEGitInstalled(true)) {
                    final String errorMessage =
                        Messages.getString("TeamExplorerGitWizardNavigationLink.EGitMissingErrorMessageText"); //$NON-NLS-1$
                    final String title =
                        Messages.getString("TeamExplorerGitWizardNavigationLink.EGitMissingErrorMessageTitle"); //$NON-NLS-1$

                    log.error("Cannot import from a Git Repository. EGit plugin is required for this action."); //$NON-NLS-1$
                    MessageDialog.openError(treeViewer.getControl().getShell(), title, errorMessage);
                    return;
                }

                // open Git import wizard
                final GitImportWizard wizard = new GitImportWizard(serverItems);
                wizard.init(PlatformUI.getWorkbench(), null);
                final WizardDialog dialog = new WizardDialog(treeViewer.getControl().getShell(), wizard);
                dialog.open();
            } catch (final Exception e) {
                log.error("", e); //$NON-NLS-1$
            }
        }

        private void extractGitRepos(final Object[] items) {
            serverItems = new ArrayList<TypedServerItem>();
            remoteUrls = new ArrayList<String>();

            for (final Object item : items) {
                if (item instanceof TfsGitRepositoryJson) {
                    final TfsGitRepositoryJson jsonItem = (TfsGitRepositoryJson) item;
                    final String projectPath =
                        ServerPath.combine(TypedServerItem.ROOT.getServerPath(), jsonItem.getTeamProject().getName());
                    final String serverPath = ServerPath.combine(projectPath, jsonItem.getName());
                    serverItems.add(new TypedServerGitRepository(serverPath, jsonItem));
                    remoteUrls.add(jsonItem.getRemoteUrl());
                }
            }
        }
    }

    private class OpenInWebAction extends Action {
        private final TeamExplorerContext context;
        private final String projectName;
        private final String repoName;
        private final String branchName;

        public OpenInWebAction(final TeamExplorerContext context, final TfsGitRepositoryJson json) {
            this.context = context;
            this.projectName = json.getTeamProject().getName();
            this.repoName = json.getName();
            this.branchName = json.getDefaultBranch();
            setText(Messages.getString("TeamExplorerGitRepositoriesSection.OpenInWebActionName")); //$NON-NLS-1$
        }

        @Override
        public void run() {
            WebAccessHelper.openGitRepo(context, projectName, repoName, branchName);
        }
    }

    private void openInEGit(final String remoteUrl) {
        try {
            final IViewPart view = TeamExplorerHelpers.showView(TeamExplorerHelpers.EGitRepoViewID);
            if (view instanceof RepositoriesView) {
                ((RepositoriesView) view).showRepository(repositoryMap.get(remoteUrl));
            }
        } catch (final Exception e) {
            log.error("", e); //$NON-NLS-1$
        }
    }

    private class GitItemDoubleClickListener implements IDoubleClickListener {
        public GitItemDoubleClickListener() {
        }

        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            if (element instanceof String) {
                final boolean expanded = treeViewer.getExpandedState(element);
                treeViewer.setExpandedState(element, !expanded);
            } else if (element instanceof TfsGitRepositoryJson) {
                final TfsGitRepositoryJson json = (TfsGitRepositoryJson) element;
                if (repositoryMap.containsKey(URLEncode.encode(json.getRemoteUrl()))) {
                    openInEGit(json.getRemoteUrl());
                }
            }
        }
    }
}
