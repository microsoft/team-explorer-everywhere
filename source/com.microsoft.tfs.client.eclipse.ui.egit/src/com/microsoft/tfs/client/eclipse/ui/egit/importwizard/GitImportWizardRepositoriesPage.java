// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.git.commands.QueryGitRepositoryBranchesCommand;
import com.microsoft.tfs.client.common.git.json.TfsGitBranchesJson;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemTreeControl;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemLabelProvider;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.ImportWizard;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation.ImportFolderValidationFlag;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation.ImportFolderValidationStatus;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepository;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepositoryCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemCollectionBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class GitImportWizardRepositoriesPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "GitImportWizardRepositoriesPage"; //$NON-NLS-1$

    public static final CodeMarker CODEMARKER_REFRESH_COMPLETE = new CodeMarker(
        "com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.GitImportWizardRepositoriesPage#refreshComplete"); //$NON-NLS-1$

    private ServerItemTreeControl folderControl;
    private Label statusLabel;

    private ImportItemCollectionBase itemCollection;
    private List<TypedServerItem> initialSelectedItems = null;

    ImportOptions options;
    TFSTeamProjectCollection connection;
    GitRepositorySource itemSource;

    public GitImportWizardRepositoriesPage(final List<TypedServerItem> initialItems) {
        super(PAGE_NAME, null, null);
        this.initialSelectedItems = initialItems;
    }

    @Override
    public String getTitle() {
        return Messages.getString("GitImportWizardRepositoriesPage.GitPageTitle"); //$NON-NLS-1$
    }

    @Override
    public String getDescription() {
        return Messages.getString("GitImportWizardRepositoriesPage.GitPageDescription"); //$NON-NLS-1$
    }

    private ImportWizard getImportWizard() {
        return (ImportWizard) getExtendedWizard();
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final ImportOptions options = (ImportOptions) getImportWizard().getPageData(ImportOptions.class);

        final Composite container = new Composite(parent, SWT.NONE);
        setControl(container);

        final GridLayout layout = new GridLayout(3, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        /*
         * Build a composite to hold the prompt label and the folder control
         * with lessened spacing
         */

        final Composite folderContainer = new Composite(container, SWT.NONE);

        final GridLayout folderLayout = new GridLayout();
        folderLayout.marginWidth = 0;
        folderLayout.marginHeight = 0;
        folderLayout.horizontalSpacing = getHorizontalSpacing();
        folderLayout.verticalSpacing = getVerticalSpacing();
        folderContainer.setLayout(folderLayout);

        final Label folderLabel = new Label(folderContainer, SWT.NONE);
        folderLabel.setText(Messages.getString("GitImportWizardRepositoriesPage.SelectRepositories")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(folderLabel);

        final ServerItemType[] visibleItemTypes;
        visibleItemTypes = new ServerItemType[] {
            ServerItemType.ROOT,
            ServerItemType.TEAM_PROJECT,
            ServerItemType.GIT_REPOSITORY
        };

        folderControl = new ServerItemTreeControl(folderContainer, SWT.MULTI);
        folderControl.setVisibleServerItemTypes(visibleItemTypes);
        folderControl.setLabelProvider(new ImportWizardTreeLabelProvider(options));
        folderControl.addSelectionChangedListener(new ImportWizardSelectionListener());
        GridDataBuilder.newInstance().grab().fill().applyTo(folderControl);
        ControlSize.setCharSizeHints(folderControl, 40, 15);

        GridDataBuilder.newInstance().hSpan(3).grab().fill().applyTo(folderContainer);

        statusLabel = new Label(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(statusLabel);
    }

    @Override
    public void refresh() {
        final ImportWizard wizard = getImportWizard();

        wizard.removePageData(ImportItemCollectionBase.class);

        options = (ImportOptions) wizard.getPageData(ImportOptions.class);
        connection = (TFSTeamProjectCollection) wizard.getPageData(TFSTeamProjectCollection.class);

        final List<ProjectInfo> projects = wizard.getInitialTeamProjectList();

        itemSource = new GitRepositorySource(connection, projects.toArray(new ProjectInfo[projects.size()]));
        itemSource.setCommandExecutor(getCommandExecutor());
        folderControl.setServerItemSource(itemSource);

        if (projects.size() == 0) {
            setPageComplete(false);
        }

        handleSelection();

        CodeMarkerDispatch.dispatch(CODEMARKER_REFRESH_COMPLETE);
    }

    private void handleSelection() {
        itemCollection = null;

        List<TypedServerItem> gitItems = getSelectedRepositories();
        if (initialSelectedItems != null && (gitItems == null || gitItems.size() == 0)) {
            gitItems = initialSelectedItems;
            folderControl.setSelectedItems(gitItems.toArray(new TypedServerItem[gitItems.size()]));
        }

        final int gitItemsSize = gitItems.size();

        if (gitItemsSize == 0) {
            statusLabel.setText(Messages.getString("GitImportWizardRepositoriesPage.NoRrepositoriesSelected")); //$NON-NLS-1$
            setPageComplete(false);
            setErrorMessage(null);
            return;
        }

        if (gitItemsSize == 1) {
            statusLabel.setText(Messages.getString("GitImportWizardRepositoriesPage.OneRepositorySelected")); //$NON-NLS-1$
        } else {
            final String message = MessageFormat.format(
                Messages.getString("GitImportWizardRepositoriesPage.MultyRepositoriesSelectedFormat"), //$NON-NLS-1$
                gitItemsSize);
            statusLabel.setText(message);
        }

        if (gitItemsSize > 0) {
            itemCollection = new ImportGitRepositoryCollection(connection, gitItems);
        }

        if (itemCollection.isValid()) {
            setPageComplete(true);
            setErrorMessage(null);
        } else {
            setPageComplete(false);
            setErrorMessage(itemCollection.getInvalidMessage());
        }
    }

    private List<TypedServerItem> getSelectedRepositories() {
        final List<TypedServerItem> items = Arrays.asList(folderControl.getSelectedItems());

        if (items.contains(TypedServerItem.ROOT)) {
            return itemSource.getRepositoriesForRoot();
        }

        final List<TypedServerItem> gitItems = new ArrayList<TypedServerItem>();
        final ServerItemType[] typesToTest = new ServerItemType[] {
            ServerItemType.TEAM_PROJECT,
            ServerItemType.GIT_REPOSITORY
        };

        for (final ServerItemType currentType : typesToTest) {
            for (final TypedServerItem item : items) {
                if (currentType == ServerItemType.TEAM_PROJECT && currentType == item.getType()) {
                    gitItems.addAll(itemSource.getRepositoriesForProject(item));
                }

                if (currentType == ServerItemType.GIT_REPOSITORY && currentType == item.getType()) {
                    if (!gitItems.contains(item)) {
                        gitItems.add(item);
                    }
                }
            }
        }

        return gitItems;
    }

    @Override
    protected boolean onPageFinished() {
        Shell shell = ShellUtils.getBestParent(null);

        if (shell == null) {
            shell = ShellUtils.getWorkbenchShell();
        }

        if (!itemCollection.isValid()) {
            MessageDialog.openError(
                shell,
                "Invalid selection", //$NON-NLS-1$
                itemCollection.getInvalidMessage());
            return false;
        }

        final ImportWizard wizard = (ImportWizard) getExtendedWizard();
        wizard.setPageData(ImportItemCollectionBase.class, itemCollection);
        final TFSTeamProjectCollection connection =
            (TFSTeamProjectCollection) wizard.getPageData(TFSTeamProjectCollection.class);

        final ImportItemBase[] repositories = itemCollection.getItems();
        final GUID[] repositoryIds = new GUID[itemCollection.size()];

        for (int i = 0; i < repositories.length; i++) {
            final ImportGitRepository repository = (ImportGitRepository) repositories[i];
            repositoryIds[i] = repository.getId();
        }

        final VersionControlClient vcClient = connection.getVersionControlClient();
        final QueryGitRepositoryBranchesCommand queryCommand =
            new QueryGitRepositoryBranchesCommand(vcClient, repositoryIds);
        final IStatus status = getCommandExecutor().execute(queryCommand);

        if (status.isOK()) {
            final Map<GUID, TfsGitBranchesJson> repositoryBranches = queryCommand.getRepositoryBranches();

            for (int i = 0; i < repositories.length; i++) {
                final ImportGitRepository repository = (ImportGitRepository) repositories[i];
                repository.setBranchesJson(repositoryBranches.get(repository.getId()));
            }
        }

        return true;
    }

    private class ImportWizardSelectionListener implements ISelectionChangedListener {
        @Override
        public void selectionChanged(final SelectionChangedEvent event) {
            handleSelection();
        }
    }

    private class ImportWizardTreeLabelProvider extends ServerItemLabelProvider {
        private final ImportOptions options;

        public ImportWizardTreeLabelProvider(final ImportOptions options) {
            Check.notNull(options, "options"); //$NON-NLS-1$

            this.options = options;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText(final Object element) {
            final TypedServerItem node = (TypedServerItem) element;

            if (node.getType() == ServerItemType.TEAM_PROJECT) {
                final String projectName = node.getName();
                final int length = itemSource.getChildren(node).length;

                if (length == 1) {
                    return MessageFormat.format(
                        Messages.getString("GitImportWizardRepositoriesPage.OneRepositoryFormat"), //$NON-NLS-1$
                        projectName,
                        length);
                } else {
                    return MessageFormat.format(
                        Messages.getString("GitImportWizardRepositoriesPage.MultipleRepositoriesFormat"), //$NON-NLS-1$
                        projectName,
                        length);
                }
            } else {
                return super.getText(element);
            }
        }

        @Override
        public Image getImage(final Object element) {
            final TypedServerItem node = (TypedServerItem) element;

            final ImportFolderValidation validation = options.getFolderValidator().validate(node.getServerPath());

            if (validation.getStatus() == ImportFolderValidationStatus.ERROR
                && !validation.hasFlag(ImportFolderValidationFlag.NO_VISUAL_ERROR)) {
                return getImageHelper().getImage(TFSCommonUIClientPlugin.PLUGIN_ID, "images/common/warning.gif"); //$NON-NLS-1$
            } else if (node.getType() == ServerItemType.GIT_REPOSITORY) {
                return getImageHelper().getImage(TFSCommonUIClientPlugin.PLUGIN_ID, "images/common/git_repo.png"); //$NON-NLS-1$
            }

            return super.getImage(element);
        }
    }
}
