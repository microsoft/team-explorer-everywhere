// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.microsoft.tfs.client.common.git.EclipseProjectInfo;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.ImportWizard;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportEclipseProject;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepository;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepositoryCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemCollectionBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.util.Check;

public class GitImportWizardSelectFoldersPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "GitImportWizardSelectFoldersPage"; //$NON-NLS-1$
    public static final String SELECTED_FOLDERS = "GitImportWizardSelectFoldersPage.SelectedFolders"; //$NON-NLS-1$
    public static final String IMPORT_EXISTSING_PROJECTS = "GitImportWizardSelectFoldersPage.ImportExisitingProjects"; //$NON-NLS-1$

    private final static int TREE_STYLES =
        SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL;
    private TreeViewer treeViewer;
    private Tree treeControl;
    private TreeColumn folderColumn;

    private Button importExistingProjectsButton;
    private Button createGenericProjectsButton;

    private ImportWizard wizard;
    private ImportOptions options;
    private IWorkspace workspace;

    private ImportGitRepository[] repositories;

    public GitImportWizardSelectFoldersPage() {
        super(
            PAGE_NAME,
            Messages.getString("GitImportWizardSelectFoldersPage.PageTitle"), //$NON-NLS-1$
            Messages.getString("GitImportWizardSelectFoldersPage.PageDescription")); //$NON-NLS-1$
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

        createWizardSelctionOptions(container);
        createRepositoriesTreeControl(container);

        refresh();
    }

    private void createWizardSelctionOptions(final Composite container) {
        importExistingProjectsButton = new Button(container, SWT.RADIO);
        importExistingProjectsButton.setText(
            Messages.getString("GitImportWizardSelectFoldersPage.ImportExistingProjectsButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(3).hFill().hGrab().applyTo(importExistingProjectsButton);

        importExistingProjectsButton.setSelection(true);
        importExistingProjectsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleWizardSelection();
            }
        });

        createGenericProjectsButton = new Button(container, SWT.RADIO);
        createGenericProjectsButton.setText(
            Messages.getString("GitImportWizardSelectFoldersPage.CreateGenericProjectsButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(3).hFill().hGrab().applyTo(createGenericProjectsButton);

        createGenericProjectsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleWizardSelection();
            }
        });

        handleWizardSelection();
    }

    private void handleWizardSelection() {
        if (importExistingProjectsButton.getSelection()) {
            wizard.setPageData(IMPORT_EXISTSING_PROJECTS, Boolean.TRUE);
        } else {
            wizard.removePageData(IMPORT_EXISTSING_PROJECTS);
        }
    }

    private void createRepositoriesTreeControl(final Composite container) {

        treeViewer = new TreeViewer(container, TREE_STYLES);
        treeControl = treeViewer.getTree();
        GridDataBuilder.newInstance().fill().grab().hHint(150).applyTo(treeControl);
        treeControl.setHeaderVisible(false);

        folderColumn = new TreeColumn(treeControl, SWT.CENTER);
        folderColumn.setText(Messages.getString("GitImportWizardSelectFoldersPage.FoldersColumnName")); //$NON-NLS-1$

        final TreeViewerColumn nameColumnViewer = new TreeViewerColumn(treeViewer, folderColumn);
        nameColumnViewer.setLabelProvider(new FolderNameLabelProvider());

        treeViewer.setUseHashlookup(true);
        treeViewer.setContentProvider(new FolderTreeContentProvider());
        treeViewer.setAutoExpandLevel(2);

        container.addControlListener(new ControlAdapter() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void controlResized(final ControlEvent e) {
                // super.controlResized(e);
                final Rectangle clientArea = container.getClientArea();
                final Point preferredSize = treeControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                final Point oldSize = treeControl.getSize();

                int newColmnWidth = clientArea.width - 2 * (treeControl.getBorderWidth() + getHorizontalMargin());
                if (preferredSize.y > clientArea.height) {
                    // if the vertical scrollbar is required, subtract its width
                    // from the new column width
                    final Point vBarSize = treeControl.getVerticalBar().getSize();
                    newColmnWidth -= vBarSize.x;
                }

                if (oldSize.x > clientArea.width) {
                    // if the table shrinks, make the column
                    // smaller first and then resize the table
                    folderColumn.setWidth(newColmnWidth);
                    treeControl.setSize(clientArea.width, clientArea.height);
                } else {
                    // if table widens, make the table
                    // bigger first and then resize the columns
                    treeControl.setSize(clientArea.width, clientArea.height);
                    folderColumn.setWidth(newColmnWidth);
                }
            }
        });

        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {

            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                setPageComplete(isValid());
            }
        });
    }

    @Override
    public void refresh() {
        if (wizard.hasPageData(IMPORT_EXISTSING_PROJECTS)) {
            importExistingProjectsButton.setSelection(true);
        } else {
            createGenericProjectsButton.setSelection(true);
        }

        final ImportGitRepositoryCollection itemCollection =
            (ImportGitRepositoryCollection) wizard.getPageData(ImportItemCollectionBase.class);

        repositories = itemCollection.getRepositories();

        setFoldersInput();
        setPageComplete(isValid());
    }

    private void setFoldersInput() {
        final List<ImportEclipseProject> rootFolders = new ArrayList<ImportEclipseProject>();

        UIHelpers.asyncExec(new Runnable() {
            @Override
            public void run() {
                for (final ImportGitRepository repository : repositories) {
                    final EclipseProjectInfo rootFolder =
                        new EclipseProjectInfo(repository.getWorkingDirectory(), workspace);
                    rootFolders.add(new ImportEclipseProject(rootFolder));
                }

                treeViewer.setInput(rootFolders.toArray(new ImportEclipseProject[rootFolders.size()]));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getPreviousPage() {
        return null;
    }

    @Override
    protected boolean onPageFinished() {

        wizard.setPageData(SELECTED_FOLDERS, getSelectedFolders());

        return super.onPageFinished();
    }

    private ImportEclipseProject[] getSelectedFolders() {
        final List<ImportEclipseProject> projects = new ArrayList<ImportEclipseProject>();
        final ITreeSelection selectedElements = (ITreeSelection) treeViewer.getSelection();

        for (final Iterator<?> i = selectedElements.iterator(); i.hasNext();) {
            final ImportEclipseProject folder = (ImportEclipseProject) i.next();
            projects.add(folder);
        }

        return projects.toArray(new ImportEclipseProject[projects.size()]);
    }

    public boolean isValid() {
        final ImportEclipseProject[] selectedFolders = getSelectedFolders();
        return selectedFolders.length > 0;
    }

    private class FolderTreeContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getChildren(final Object parentElement) {
            Check.notNull(parentElement, "parentElement"); //$NON-NLS-1$

            if (parentElement instanceof ImportEclipseProject) {
                final ImportEclipseProject project = (ImportEclipseProject) parentElement;
                return project.getSubfolders();
            }

            return new Object[0];
        }

        @Override
        public boolean hasChildren(final Object element) {
            return true;
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
            final ImportEclipseProject project = (ImportEclipseProject) element;
            return project.getParentFolder();
        }

        @Override
        public void dispose() {
        }
    }

    private class FolderNameLabelProvider extends CellLabelProvider {
        @Override
        public void update(final ViewerCell cell) {
            final Object element = cell.getElement();

            if (element instanceof ImportEclipseProject) {
                final ImportEclipseProject project = (ImportEclipseProject) element;

                cell.setText(project.getParentFolder() == null ? project.getProjectPath() : project.getFolderName());
            }
        }
    }
}
