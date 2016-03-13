// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.dialogs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.client.common.ui.teambuild.egit.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.egit.repositories.GitBranch;
import com.microsoft.tfs.client.common.ui.teambuild.egit.repositories.GitRepositoriesMap;
import com.microsoft.tfs.client.common.ui.teambuild.egit.repositories.GitRepository;
import com.microsoft.tfs.core.clients.build.GitProperties;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSourceProvider;
import com.microsoft.tfs.core.clients.build.exceptions.BuildException;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.util.StringUtil;

/**
 * The git repo and branch information are from REST API now. VS uses fullname
 * "ref/heads/master" as branch names, we keep consistency here
 */
public class GitSourceSettingsControl extends BaseControl {
    private Text repoPathText;
    private Combo repoCombo;
    private ComboViewer repoComboViewer;
    private Combo branchCombo;
    private ComboViewer branchComboViewer;
    private CheckboxTableViewer branchViewer;
    private final IBuildDefinition buildDefinition;
    private final String projectName;
    private final VersionControlClient vcClient;
    private GitRepositoriesMap repositoriesMap;
    private GitRepository initRepository;

    public GitSourceSettingsControl(final Composite parent, final int style, final IBuildDefinition buildDefinition) {
        super(parent, style);
        this.buildDefinition = buildDefinition;
        this.projectName = buildDefinition.getTeamProject();
        this.vcClient = buildDefinition.getBuildServer().getConnection().getVersionControlClient();
        createControls(this);
    }

    private void createControls(final Composite composite) {
        final GridLayout layout = SWTUtil.gridLayout(composite, 1);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        final Label helperLabel = new Label(composite, SWT.WRAP);
        helperLabel.setText(Messages.getString("GitSourceSettingsControl.SourceSettingHelperLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).fill().hGrab().applyTo(helperLabel);

        SWTUtil.createGridLayoutSpacer(composite);

        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("GitSourceSettingsControl.RepositoryLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(label);

        repoCombo = new Combo(this, SWT.READ_ONLY);
        GridDataBuilder.newInstance().hFill().vAlign(SWT.LEFT).applyTo(repoCombo);

        repoComboViewer = new ComboViewer(repoCombo);
        setupRepoComboViewer();

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("GitSourceSettingsControl.RepoLocalPathLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(label);

        repoPathText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(repoPathText);

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("GitSourceSettingsControl.BranchLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(label);

        branchCombo = new Combo(composite, SWT.READ_ONLY);
        GridDataBuilder.newInstance().hFill().vAlign(SWT.LEFT).applyTo(branchCombo);

        label = new Label(composite, SWT.NONE);
        label.setText(Messages.getString("GitSourceSettingsControl.MonitoredBranchLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().applyTo(label);

        final Table table = new Table(composite, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI);
        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);
        GridDataBuilder.newInstance().grab().fill().applyTo(table);

        tableLayout.addColumnData(new ColumnWeightData(40, 20, true));
        final TableColumn nameTableColumn = new TableColumn(table, SWT.NONE);
        nameTableColumn.setText("Branch"); //$NON-NLS-1$

        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        branchComboViewer = new ComboViewer(branchCombo);
        branchViewer = new CheckboxTableViewer(table);
        setupBranchViewers();

        initializeRepos();
        loadSourceProvider();
    }

    public void setRepoList() {
        final List<GitRepository> repositories = repositoriesMap.getServerRepositories();
        final List<GitRepository> mappedRepositories = repositoriesMap.getMappedRepositories();

        repoComboViewer.setInput(repositories);

        ComboHelper.setVisibleItemCount(
            repoComboViewer.getCombo(),
            repositories.isEmpty() ? 1 : repositories.size(),
            ComboHelper.MAX_VISIBLE_ITEM_COUNT);

        if (!mappedRepositories.isEmpty()) {
            initRepository = mappedRepositories.get(0);
        } else if (!repositories.isEmpty()) {
            initRepository = repositories.get(0);
        }

        if (initRepository != null) {
            repoComboViewer.setSelection(new StructuredSelection(initRepository));
        }
    }

    public String getUniqueRepoName() {
        return GitProperties.createUniqueRepoName(projectName, getSelectedRepo());
    }

    public String getSelectedRepo() {
        if (repoComboViewer.getSelection().isEmpty()) {
            return null;
        } else {
            return ((GitRepository) ((IStructuredSelection) repoComboViewer.getSelection()).getFirstElement()).getName();
        }
    }

    public String getSelectedBranch() {
        if (branchComboViewer.getSelection().isEmpty()) {
            return null;
        } else {
            return ((GitBranch) ((IStructuredSelection) branchComboViewer.getSelection()).getFirstElement()).getRemoteFullName();
        }
    }

    public String[] getSelectedCIBranches() {
        final Object[] selection = branchViewer.getCheckedElements();
        if (selection == null || selection.length == 0) {
            return new String[0];
        } else {
            final List<String> selectedBranches = new ArrayList<String>();
            for (final Object branch : selection) {
                selectedBranches.add(((GitBranch) branch).getRemoteFullName());
            }

            return selectedBranches.toArray(new String[selectedBranches.size()]);
        }
    }

    public String getRepoPath() {
        return repoPathText.getText().trim();
    }

    public Text getRepoPathText() {
        return repoPathText;
    }

    public GitRepositoriesMap getRepositoriesMap() {
        return repositoriesMap;
    }

    private void loadSourceProvider() {
        final List<GitRepository> repositories = repositoriesMap.getServerRepositories();
        if (repositories == null || repositories.size() == 0) {
            return;
        }

        final IBuildDefinitionSourceProvider sourceProvider = buildDefinition.getDefaultSourceProvider();
        final String uniqRepoName = sourceProvider.getValueByName(GitProperties.RepositoryName);

        if (StringUtil.isNullOrEmpty(uniqRepoName)) {
            // This is a new build definition, sourceProvider fields are not
            // filled yet
            // set default branch checked for CI builds based on initRepo
            // populated
            if (initRepository != null) {
                branchViewer.setCheckedElements(new GitBranch[] {
                    initRepository.getDefaultBranch()
                });
            }
        } else {
            final String repoName = GitProperties.getRepoNameFromUniqueRepoName(uniqRepoName);
            final String branchName = sourceProvider.getValueByName(GitProperties.DefaultBranch);
            final List<String> ciBranches =
                GitProperties.splitBranches(sourceProvider.getValueByName(GitProperties.CIBranches));

            for (int i = 0; i < repositories.size(); i++) {
                final GitRepository repository = (GitRepository) repoComboViewer.getElementAt(i);

                if (repoName.equals(repository.getName())) {
                    repoCombo.select(i);
                    onRepoSelected(repository);

                    if (!StringUtil.isNullOrEmpty(branchName)) {
                        for (int j = 0; j < repository.getBranches().size(); j++) {
                            final GitBranch branch = (GitBranch) branchComboViewer.getElementAt(j);

                            if (branchName.equals(branch.getRemoteFullName())) {
                                branchCombo.select(j);
                                break;
                            }
                        }
                    }

                    final List<GitBranch> selectedBranches = new ArrayList<GitBranch>();

                    for (final GitBranch branch : repository.getBranches()) {
                        if (ciBranches.contains(branch.getRemoteFullName())) {
                            selectedBranches.add(branch);
                        }
                    }

                    branchViewer.setCheckedElements(selectedBranches.toArray(new GitBranch[selectedBranches.size()]));

                    return;
                }
            }
        }
    }

    private void onRepoSelected(final GitRepository repository) {
        repoPathText.setText(repository.getWorkingDirectoryPath());

        branchComboViewer.setInput(repository.getBranches());

        if (repository.getCurrentBranch() != null) {
            branchComboViewer.setSelection(new StructuredSelection(repository.getCurrentBranch()));
        } else if (repository.getDefaultBranch() != null) {
            branchComboViewer.setSelection(new StructuredSelection(repository.getDefaultBranch()));
        } else if (!repository.getBranches().isEmpty()) {
            branchComboViewer.setSelection(new StructuredSelection(repository.getBranches().get(0)));
        }

        branchViewer.setInput(repository.getBranches());
    }

    private void initializeRepos() {
        repositoriesMap = new GitRepositoriesMap(vcClient, projectName);

        if (repositoriesMap.getRegisteredRepositories().isEmpty()) {
            final String message = Messages.getString("GitSourceSettingsControl.NoRegisteredRepositories"); //$NON-NLS-1$
            throw new BuildException(message);
        }

        if (repositoriesMap.getServerRepositories().isEmpty()) {
            final String message = Messages.getString("GitSourceSettingsControl.NoServerRepositories"); //$NON-NLS-1$
            throw new BuildException(message);
        }

        if (repositoriesMap.getClonedRepositories().isEmpty()) {
            final String message = Messages.getString("GitSourceSettingsControl.NoClonedRepositoriesFound"); //$NON-NLS-1$
            throw new BuildException(message);
        }

        if (repositoriesMap.getMappedRepositories().isEmpty()) {
            final String message = Messages.getString("GitSourceSettingsControl.NoUpstreamRepositoriesFound"); //$NON-NLS-1$
            throw new BuildException(message);
        }

        setRepoList();
    }

    private void setupRepoComboViewer() {
        repoComboViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(final Object element) {
                final GitRepository repository = (GitRepository) element;
                final String name = repository.getName();
                final GitBranch branch = repository.getCurrentBranch();
                final String path = repository.getRepositoryPath();

                return MessageFormat.format(
                    "{0} [{1}] - {2}", //$NON-NLS-1$
                    name,
                    branch == null ? "?" : branch.getRemoteName(), //$NON-NLS-1$
                    path);
            }
        });

        repoComboViewer.setContentProvider(new ContentProviderAdapter() {
            @Override
            public Object[] getElements(final Object input) {
                @SuppressWarnings("unchecked")
                final List<GitRepository> repositories = ((List<GitRepository>) input);

                return repositories.toArray(new GitRepository[repositories.size()]);
            }
        });

        repoComboViewer.setComparator(new ViewerComparator() {
            @Override
            public int compare(final Viewer viewer, final Object e1, final Object e2) {
                return ((GitRepository) e1).compareTo(((GitRepository) e2));
            }
        });

        repoComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                onRepoSelected((GitRepository) selection.getFirstElement());
            }
        });
    }

    private void setupBranchViewers() {
        final IContentProvider contentProvider = new ContentProviderAdapter() {
            @Override
            public Object[] getElements(final Object input) {
                @SuppressWarnings("unchecked")
                final List<GitBranch> branches = ((List<GitBranch>) input);

                return branches.toArray(new GitBranch[branches.size()]);
            }
        };
        branchViewer.setContentProvider(contentProvider);
        branchComboViewer.setContentProvider(contentProvider);

        final LabelProvider labelProvider = new LabelProvider() {
            @Override
            public String getText(final Object element) {
                return ((GitBranch) element).getRemoteName();
            }
        };
        branchViewer.setLabelProvider(labelProvider);
        branchComboViewer.setLabelProvider(labelProvider);

        final ViewerComparator viewerComparator = new ViewerComparator() {
            @Override
            public int compare(final Viewer viewer, final Object e1, final Object e2) {
                return ((GitBranch) e1).compareTo(((GitBranch) e2));
            }
        };
        branchViewer.setComparator(viewerComparator);
        branchComboViewer.setComparator(viewerComparator);
    }
}
