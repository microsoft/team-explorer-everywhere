// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.framework.command.CommandFactory;
import com.microsoft.tfs.client.common.git.EclipseProjectInfo;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.helpers.WorkingSetHelper;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.ImportWizard;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportEclipseProject;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepository;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepositoryCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemCollectionBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public class GitImportWizardSelectProjectsPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "GitImportWizardSelectProjectsPage"; //$NON-NLS-1$

    private static final Log logger = LogFactory.getLog(GitImportWizardSelectProjectsPage.class);

    private final static int TREE_STYLES =
        SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL;
    private CheckboxTreeViewer treeViewer;
    private Tree treeControl;
    private TreeColumn projectColumn;
    private TreeColumn pathColumn;

    Font italicFont = null;
    Font boldFont = null;

    private Button searchForNestedProjectsButton;
    private Button workingSetButton;
    private Button workingSetSelectButton;
    private Combo workingSetCombo;

    private ImportWizard wizard;
    private ImportOptions options;
    private IWorkspace workspace;

    private ImportGitRepository[] repositories;
    private ImportEclipseProject[] folders;
    private IWorkingSet[] workingSets;
    private String[] workingSetNames;

    private final Set<String> workspaceProjects = new HashSet<String>();

    private boolean finishButtonAlreadyClicked = false;

    public GitImportWizardSelectProjectsPage() {
        super(
            PAGE_NAME,
            Messages.getString("GitImportWizardSelectProjectsPage.PageTitle"), //$NON-NLS-1$
            Messages.getString("GitImportWizardSelectProjectsPage.PageProjectsDescription")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        wizard = (ImportWizard) getExtendedWizard();
        options = (ImportOptions) wizard.getPageData(ImportOptions.class);
        workspace = options.getEclipseWorkspace();

        final Composite container = new Composite(parent, SWT.NONE);
        setControl(container);

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        createProjectsTreeControl(container);
        createWorkingSetOption(container);

        refresh();
    }

    private void handleProjectSelection() {
        setPageComplete(isValid());
    }

    private void createProjectsTreeControl(final Composite container) {
        treeViewer = new CheckboxTreeViewer(container, TREE_STYLES);
        treeControl = treeViewer.getTree();
        GridDataBuilder.newInstance().fill().grab().hHint(150).applyTo(treeControl);
        treeControl.setHeaderVisible(false);

        projectColumn = new TreeColumn(treeControl, SWT.NONE);
        projectColumn.setText(Messages.getString("GitImportWizardSelectProjectsPage.ProjectColumnName")); //$NON-NLS-1$
        projectColumn.setWidth(300);

        final TreeViewerColumn nameColumnViewer = new TreeViewerColumn(treeViewer, projectColumn);
        nameColumnViewer.setLabelProvider(new ProjectNameLabelProvider());
        nameColumnViewer.setEditingSupport(new ProjectNameEditingSupport(treeViewer));

        pathColumn = new TreeColumn(treeControl, SWT.NONE);
        pathColumn.setText(Messages.getString("GitImportWizardSelectProjectsPage.PathColumnName")); //$NON-NLS-1$
        pathColumn.setWidth(500);

        final TreeViewerColumn pathColumnViewer = new TreeViewerColumn(treeViewer, pathColumn);
        pathColumnViewer.setLabelProvider(new ImportProjectPathLabelProvider());

        treeControl.setHeaderVisible(true);
        treeControl.setSortColumn(projectColumn);
        treeControl.setLinesVisible(true);

        treeViewer.setUseHashlookup(true);
        treeViewer.setContentProvider(new ProjectTreeContentProvider());
        treeViewer.setAutoExpandLevel(2);
        treeViewer.addCheckStateListener(new ProjectCheckStateListener());

        searchForNestedProjectsButton = new Button(container, SWT.CHECK);
        searchForNestedProjectsButton.setText(
            Messages.getString("GitImportWizardSelectProjectsPage.SearchForNestedButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(searchForNestedProjectsButton);
        searchForNestedProjectsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                refresh();

            }
        });
    }

    private void createWorkingSetOption(final Composite container) {
        final Composite optionsContainer = new Composite(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().vIndent(getVerticalSpacing() * 2).applyTo(optionsContainer);

        final GridLayout optionsLayout = new GridLayout(3, false);
        optionsLayout.marginWidth = 0;
        optionsLayout.marginHeight = 0;
        optionsLayout.horizontalSpacing = getHorizontalSpacing();
        optionsLayout.verticalSpacing = 0;
        optionsContainer.setLayout(optionsLayout);

        workingSetButton = new Button(optionsContainer, SWT.CHECK);
        workingSetButton.setText(Messages.getString("GitImportWizardSelectProjectsPage.WorkingSetButtonText")); //$NON-NLS-1$
        workingSetButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                /*
                 * No working set: simply open the dialog instead of enabling a
                 * useless combo.
                 */
                if (workingSets.length == 0) {
                    selectWorkingSet();

                    /* Still no working set. (Dialog canceled.) Abort. */
                    if (workingSets.length == 0) {
                        workingSetButton.setSelection(false);
                        return;
                    }
                }

                workingSetCombo.setEnabled(workingSetButton.getSelection());
                workingSetSelectButton.setEnabled(workingSetButton.getSelection());
            }
        });

        workingSetCombo = new Combo(optionsContainer, SWT.READ_ONLY);
        workingSetCombo.setEnabled(false);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(workingSetCombo);

        workingSetSelectButton = new Button(optionsContainer, SWT.NONE);
        workingSetSelectButton.setText(
            Messages.getString("GitImportWizardSelectProjectsPage.WorkingSetSelectButtonText")); //$NON-NLS-1$
        workingSetSelectButton.setEnabled(false);
        workingSetSelectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectWorkingSet();
            }
        });

        ButtonHelper.resizeButtons(new Button[] {
            workingSetSelectButton
        });

        computeWorkingSets();

        if (options.getWorkingSet() != null) {
            workingSetButton.setSelection(true);
            selectWorkingSet(options.getWorkingSet());
        }
    }

    private void selectWorkingSet() {
        final IWorkingSetSelectionDialog workingSetDialog =
            PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetSelectionDialog(getShell(), false);

        if (workingSetDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        final IWorkingSet[] selection = workingSetDialog.getSelection();
        final IWorkingSet set = selection.length == 0 ? null : selection[0];

        computeWorkingSets(set);

        if (selection.length > 0) {
            selectWorkingSet(set);
        }
    }

    private void selectWorkingSet(final IWorkingSet selection) {
        for (int i = 0; i < workingSets.length; i++) {
            if (workingSets[i].getName().equals(selection.getName())) {
                workingSetCombo.select(i);
                return;
            }
        }
    }

    private void computeWorkingSets() {
        computeWorkingSets(null);
    }

    /**
     * Rebuilds the internal list of IWorkingSets that are available. Optionally
     * includes the given <code>newWorkingSet</code>. This is useful for
     * including an AggregateWorkingSet that is not included in the list of
     * working sets managed by the {@link IWorkbench}'s
     * {@link IWorkingSetManager}.
     *
     * @param newWorkingSet
     *        An IWorkingSet to include in the list. (may be <code>null</code>).
     */
    private void computeWorkingSets(final IWorkingSet newWorkingSet) {
        if (newWorkingSet == null) {
            workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
        } else {
            final List<IWorkingSet> workingSetList = new ArrayList<IWorkingSet>();

            workingSetList.addAll(Arrays.asList(PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets()));

            if (!workingSetList.contains(newWorkingSet)) {
                workingSetList.add(newWorkingSet);
            }

            workingSets = workingSetList.toArray(new IWorkingSet[workingSetList.size()]);
        }

        workingSetNames = new String[workingSets.length];
        for (int i = 0; i < workingSets.length; i++) {
            workingSetNames[i] = WorkingSetHelper.getLabel(workingSets[i]);
        }

        workingSetCombo.setItems(workingSetNames);

        if (workingSetCombo.getSelectionIndex() == -1) {
            workingSetCombo.select(0);
        }
    }

    @Override
    public void refresh() {
        if (isInProjectMode()) {
            setDescription(Messages.getString("GitImportWizardSelectProjectsPage.PageProjectsDescription")); //$NON-NLS-1$
            searchForNestedProjectsButton.setVisible(true);
        } else {
            setDescription(Messages.getString("GitImportWizardSelectProjectsPage.PageFoldersDescription")); //$NON-NLS-1$
            searchForNestedProjectsButton.setVisible(false);
        }

        getProjectsInWorkspace();

        final ImportGitRepositoryCollection itemCollection =
            (ImportGitRepositoryCollection) wizard.getPageData(ImportItemCollectionBase.class);
        repositories = itemCollection.getRepositories();
        folders = (ImportEclipseProject[]) wizard.getPageData(GitImportWizardSelectFoldersPage.SELECTED_FOLDERS);

        if (isInProjectMode()) {
            setProjectsInput();
        } else {
            setFoldersInput();
        }

        computeWorkingSets();
    }

    private boolean isInProjectMode() {
        return wizard.hasPageData(GitImportWizardSelectFoldersPage.IMPORT_EXISTSING_PROJECTS);
    }

    private void setProjectsInput() {
        projectColumn.setText(Messages.getString("GitImportWizardSelectProjectsPage.ProjectColumnName")); //$NON-NLS-1$
        pathColumn.setText(Messages.getString("GitImportWizardSelectProjectsPage.PathColumnName")); //$NON-NLS-1$

        UIHelpers.asyncExec(new Runnable() {
            @Override
            public void run() {
                final IStatus status = searchWorkingDirectory(repositories, workspace);

                if (status.isOK()) {
                    setMessage(null);
                    handleProjectSelection();
                } else {
                    setMessage(status.getMessage(), status.getSeverity());
                }
            }
        });
    }

    private void setFoldersInput() {
        projectColumn.setText(Messages.getString("GitImportWizardSelectProjectsPage.NewProjectColumnName")); //$NON-NLS-1$
        pathColumn.setText(Messages.getString("GitImportWizardSelectProjectsPage.FolderColumnName")); //$NON-NLS-1$

        treeViewer.setInput(folders);

        for (final ImportEclipseProject folder : folders) {
            treeViewer.setChecked(folder, true);
        }

        handleProjectSelection();
    }

    @Override
    protected boolean onPageFinished() {
        if (finishButtonAlreadyClicked) {
            return false;
        }

        finishButtonAlreadyClicked = true;

        if (workingSetButton.getSelection() && workingSets.length > 0) {
            options.setWorkingSet(workingSets[workingSetCombo.getSelectionIndex()]);
        }

        wizard.setPageData(EclipseProjectInfo.class, getSelectedProjects());

        return true;
    }

    private ImportEclipseProject[] getSelectedProjects() {
        final List<ImportEclipseProject> projects = new ArrayList<ImportEclipseProject>();

        for (final Object o : treeViewer.getCheckedElements()) {
            if (o instanceof ImportEclipseProject) {
                projects.add((ImportEclipseProject) o);
            }
        }

        return projects.toArray(new ImportEclipseProject[projects.size()]);
    }

    private IStatus searchWorkingDirectory(final ImportGitRepository[] repositories, final IWorkspace workspace) {
        final List<ImportEclipseProject> projects = new ArrayList<ImportEclipseProject>();
        setMessage(Messages.getString("GitImportWizardSelectProjectsPage.SearchingMessageText")); //$NON-NLS-1$

        final List<File> fileList = new ArrayList<File>();

        for (final ImportEclipseProject folder : folders) {
            fileList.add(new File(folder.getProjectPath()));
        }

        final FindEclipseProjectsCommand findCommand =
            new FindEclipseProjectsCommand(fileList, workspace, getSearchForNested());

        try {
            final IStatus status = getCommandExecutor().execute(CommandFactory.newCancelableCommand(findCommand));

            if (status.isOK()) {

                for (final EclipseProjectInfo project : findCommand.getProjects()) {
                    for (final ImportGitRepository repository : repositories) {
                        if (LocalPath.isChild(repository.getWorkingDirectory(), project.getProjectPath())) {
                            projects.add(new ImportEclipseProject(project, repository));
                            break;
                        }
                    }
                }

                Collections.sort(projects);
            }

            return status;

        } catch (final CanceledException e) {
            logger.error(e.getMessage(), e);
            return Status.CANCEL_STATUS;
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        } finally {
            treeViewer.setInput(projects.toArray(new ImportEclipseProject[projects.size()]));

            for (final ImportEclipseProject project : projects) {
                treeViewer.setChecked(project, true);
            }
        }
    }

    public boolean isValid() {

        final ImportEclipseProject[] selectedProjects = getSelectedProjects();

        if (selectedProjects.length == 0) {
            setMessage(
                Messages.getString("GitImportWizardSelectProjectsPage.NoEclipseProjectImportedWarningMessage"), //$NON-NLS-1$
                IMessageProvider.WARNING);
            return true;
        }

        if (workspaceProjectNameCollision(selectedProjects)) {
            return false;
        }

        if (duplicateProjectNamesSelected(selectedProjects)) {
            return false;
        }

        setErrorMessage(null);
        setMessage(null);
        return true;
    }

    private boolean workspaceProjectNameCollision(final ImportEclipseProject[] selectedProjects) {
        for (final ImportEclipseProject project : selectedProjects) {
            if (workspaceProjects.contains(project.getProjectName())) {
                setErrorMessage(MessageFormat.format(
                    //@formatter:off
                    Messages.getString("GitImportWizardSelectProjectsPage.WorkspaceProjectNameCollisionErrorFormat"), //$NON-NLS-1$
                    //@formatter:on
                    project.getProjectName()));
                return true;
            }
        }

        return false;
    }

    private boolean duplicateProjectNamesSelected(final ImportEclipseProject[] selectedProjects) {
        final Set<String> projectNames = new HashSet<String>(selectedProjects.length);

        for (final ImportEclipseProject project : selectedProjects) {
            final String projectName = project.getProjectName().toLowerCase();

            if (projectNames.contains(projectName)) {
                setErrorMessage(Messages.getString("GitImportWizardSelectProjectsPage.DuplicateProjectNamesErrorText")); //$NON-NLS-1$
                return true;
            }

            projectNames.add(projectName);
        }

        return false;
    }

    private class ProjectTreeContentProvider implements ITreeContentProvider {
        private final Object[] NO_CHILDREN = new Object[0];

        @Override
        public Object[] getChildren(final Object parentElement) {
            return NO_CHILDREN;
        }

        @Override
        public boolean hasChildren(final Object element) {
            return false;
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            return (Object[]) inputElement;
        }

        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        }

        @Override
        public Object getParent(final Object element) {
            return null;
        }

        @Override
        public void dispose() {
        }
    }

    private class ProjectNameLabelProvider extends CellLabelProvider {
        @Override
        public void update(final ViewerCell cell) {
            final Object element = cell.getElement();

            if (element instanceof ImportEclipseProject) {
                final ImportEclipseProject project = (ImportEclipseProject) element;

                if (project.isDamaged()) {
                    cell.setText(Messages.getString("GitImportWizardSelectProjectsPage.ProjectDamaged")); //$NON-NLS-1$
                    setCellFontStyle(cell, SWT.ITALIC);
                } else {
                    cell.setText(project.getProjectName());
                }

                if (!canImport(project)) {
                    setCellForeground(cell, SWT.COLOR_GRAY);
                }
            }
        }
    }

    private class ProjectNameEditingSupport extends EditingSupport {
        public ProjectNameEditingSupport(final ColumnViewer viewer) {
            super(viewer);
        }

        @Override
        protected CellEditor getCellEditor(final Object element) {
            if (element instanceof ImportEclipseProject) {
                final ImportEclipseProject project = (ImportEclipseProject) element;
                final CellEditor editor = new TextCellEditor(treeControl);

                editor.setValidator(new ICellEditorValidator() {
                    @Override
                    public String isValid(final Object value) {
                        final String newProjectName = (String) value;

                        if (project.isTopLevelFolder()) {
                            return Messages.getString("GitImportWizardSelectProjectsPage.TopmostFolderNameChangeError"); //$NON-NLS-1$
                        }

                        if (project.isValidProjectName(newProjectName)) {
                            return null;
                        } else {
                            return project.getValidationMessage();
                        }
                    }
                });

                editor.addListener(new ICellEditorListener() {
                    @Override
                    public void applyEditorValue() {
                        handleProjectSelection();
                    }

                    @Override
                    public void cancelEditor() {
                        handleProjectSelection();
                    }

                    @Override
                    public void editorValueChanged(final boolean oldValidState, final boolean newValidState) {
                        final String errorMessage = editor.getErrorMessage();
                        if (!StringUtil.isNullOrEmpty(errorMessage)) {
                            setErrorMessage(errorMessage);
                            setPageComplete(false);
                        } else {
                            handleProjectSelection();
                        }
                    }
                });

                return editor;
            } else {
                return null;
            }
        }

        @Override
        protected boolean canEdit(final Object element) {
            if (element instanceof ImportEclipseProject) {
                final ImportEclipseProject project = (ImportEclipseProject) element;
                return !project.hasProjectDescription();
            }

            return false;
        }

        @Override
        protected Object getValue(final Object element) {
            if (element instanceof ImportEclipseProject) {
                final ImportEclipseProject project = (ImportEclipseProject) element;
                return project.getProjectName();
            } else {
                return null;
            }
        }

        @Override
        protected void setValue(final Object element, final Object newValue) {
            if (element instanceof ImportEclipseProject) {
                final ImportEclipseProject project = (ImportEclipseProject) element;
                final String newProjectName = (String) newValue;
                if (!StringUtil.isNullOrEmpty(newProjectName) && !newProjectName.equals(project.getProjectName())) {
                    project.setProjectName(newProjectName);
                    treeViewer.update(element, null);
                    handleProjectSelection();
                }
            }
        }
    }

    private class ImportProjectPathLabelProvider extends CellLabelProvider {
        @Override
        public void update(final ViewerCell cell) {
            final Object element = cell.getElement();

            if (element instanceof ImportEclipseProject) {
                final ImportEclipseProject project = (ImportEclipseProject) element;

                cell.setText(project.getProjectPath());

                if (!canImport(project)) {
                    setCellForeground(cell, SWT.COLOR_GRAY);
                }
            }
        }
    }

    private class ProjectCheckStateListener implements ICheckStateListener {
        @Override
        public void checkStateChanged(final CheckStateChangedEvent event) {
            try {
                final Object eventElement = event.getElement();

                if (eventElement instanceof ImportEclipseProject) {
                    final ImportEclipseProject project = (ImportEclipseProject) eventElement;
                    if (!canImport(project)) {
                        treeViewer.setChecked(project, false);
                    }

                    project.setSelected(treeViewer.getChecked(project));
                    treeViewer.refresh(project);
                }
            } catch (final Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                handleProjectSelection();
            }
        }
    }

    private void setCellFontStyle(final ViewerCell cell, final int style) {
        if (style == SWT.ITALIC) {
            if (italicFont == null) {
                italicFont = createFont(cell.getFont(), SWT.ITALIC);
            }
            cell.setFont(italicFont);
        } else if (style == SWT.BOLD) {
            if (boldFont == null) {
                boldFont = createFont(cell.getFont(), SWT.BOLD);
            }
            cell.setFont(boldFont);
        }
    }

    private Font createFont(final Font baseFont, final int style) {
        final FontData[] fontData = baseFont.getFontData();

        for (final FontData data : fontData) {
            data.setStyle(data.getStyle() | style);
        }

        return (new Font(getControl().getDisplay(), fontData));
    }

    @Override
    public void dispose() {
        if (italicFont != null && !italicFont.isDisposed()) {
            italicFont.dispose();
            italicFont = null;
        }
        if (boldFont != null && !boldFont.isDisposed()) {
            boldFont.dispose();
            boldFont = null;
        }
    }

    private void setCellForeground(final ViewerCell cell, final int color) {
        cell.setForeground(getControl().getDisplay().getSystemColor(color));
    }

    private boolean canImport(final ImportEclipseProject project) {
        return !project.isDamaged();
    }

    private boolean getSearchForNested() {
        return isInProjectMode() && searchForNestedProjectsButton.getSelection();
    }

    private void getProjectsInWorkspace() {
        workspaceProjects.clear();

        for (final IProject project : options.getEclipseWorkspace().getRoot().getProjects()) {
            workspaceProjects.add(project.getName());
        }
    }
}
