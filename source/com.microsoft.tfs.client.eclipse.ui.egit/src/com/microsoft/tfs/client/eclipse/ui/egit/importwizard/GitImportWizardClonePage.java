// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.framework.command.CommandFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.table.TableViewerUtils;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.ImportWizard;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepository;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepositoryCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemCollectionBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;

public class GitImportWizardClonePage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "GitImportWizardClonePage"; //$NON-NLS-1$

    private static final Log logger = LogFactory.getLog(GitImportWizardClonePage.class);

    final static String REPOSITORY_COLUMN = "repository"; //$NON-NLS-1$
    final static String BRANCH_COLUMN = "branch"; //$NON-NLS-1$
    final static String STATE_COLUMN = "state"; //$NON-NLS-1$

    private Label confirmationLabel;
    private ImportWizardCloneRepositoriesTable repositoryTable;

    private boolean cloningPerformed = false;
    private boolean cloningHadErrors = false;

    public GitImportWizardClonePage() {
        super(
            PAGE_NAME,
            Messages.getString("GitImportWizardClonePage.PageName"), //$NON-NLS-1$
            Messages.getString("GitImportWizardClonePage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final Composite container = new Composite(parent, SWT.NONE);
        setControl(container);

        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        createRepositoryTable(container);
    }

    private void createRepositoryTable(final Composite container) {
        confirmationLabel = new Label(container, SWT.NONE);
        confirmationLabel.setText(Messages.getString("GitImportWizardClonePage.RepositoriesLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(confirmationLabel);

        repositoryTable = new ImportWizardCloneRepositoriesTable(container);
        GridDataBuilder.newInstance().fill().grab().applyTo(repositoryTable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);

        repositoryTable.showColumn(STATE_COLUMN, false);
    }

    @Override
    protected boolean onPageFinished() {
        cloningPerformed = true;
        repositoryTable.showColumn(STATE_COLUMN, true);
        doClone();
        cloningHadErrors = someRepositoryNotCloned();
        setPageComplete(!cloningHadErrors);

        if (cloningHadErrors) {
            final ImportGitRepository[] repositories = repositoryTable.getImportRepositories();

            for (final ImportGitRepository repository : repositories) {
                if (!repository.getCloneStatus().equals(ImportGitRepository.CLONE_FINISHED)
                    && !repository.getCloneStatus().equals(ImportGitRepository.CLONE_EMPTY_REPOSITORY)) {
                    final File folder = new File(repository.getWorkingDirectory());
                    if (folder.exists()) {
                        removeFileTree(folder);
                    }
                }
            }
        }

        return !cloningHadErrors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getNextPage() {
        if (cloningHadErrors) {
            return null;
        } else {
            return super.getNextPage();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getPreviousPage() {
        if (cloningPerformed) {
            return null;
        } else {
            return super.getPreviousPage();
        }
    }

    private void removeFileTree(final File root) {
        final String[] names = root.list();
        if (names != null) {
            for (final String name : names) {
                removeFileTree(new File(root, name));
            }
        }

        root.delete();
    }

    private ImportWizard getImportWizard() {
        return (ImportWizard) getExtendedWizard();
    }

    private boolean someRepositoryNotCloned() {
        return checkCloneStatus(false);
    }

    private boolean checkCloneStatus(final boolean checkForCloned) {
        final ImportGitRepository[] repositories = repositoryTable.getImportRepositories();

        if (repositories != null) {
            for (final ImportGitRepository repository : repositories) {
                if (checkForCloned && repository.getCloneStatus() == ImportGitRepository.CLONE_FINISHED) {
                    return true;
                } else if (!checkForCloned
                    && repository.getCloneStatus() != ImportGitRepository.CLONE_FINISHED
                    && repository.getCloneStatus() != ImportGitRepository.CLONE_EMPTY_REPOSITORY) {
                    return true;
                }
            }
        } else {
            return !checkForCloned;
        }

        return false;
    }

    private void doClone() {
        final ImportOptions options = (ImportOptions) getImportWizard().getPageData(ImportOptions.class);
        final UsernamePasswordCredentials credentials = options.getCredentials();

        UIHelpers.syncExec(new Runnable() {
            @Override
            public void run() {
                boolean cancelled = false;
                final ImportGitRepository[] repositories = repositoryTable.getImportRepositories();

                for (final ImportGitRepository repository : repositories) {
                    if (cancelled) {
                        repository.setCloneStatus(ImportGitRepository.CLONE_CANCELLED);
                    } else if (!repository.getCloneStatus().equals(ImportGitRepository.CLONE_FINISHED)
                        && !repository.getCloneStatus().equals(ImportGitRepository.CLONE_EMPTY_REPOSITORY)) {

                        final IStatus status = cloneRepository(credentials, repository);
                        if (status.getCode() == IStatus.CANCEL) {
                            cancelled = true;
                        } else if (!status.isOK()) {
                            break;
                        }
                    }
                }

                repositoryTable.refresh();
            }
        });
    }

    @Override
    protected void refresh() {
        final ImportWizard wizard = (ImportWizard) getExtendedWizard();
        final ImportGitRepositoryCollection repositoryCollection =
            (ImportGitRepositoryCollection) wizard.getPageData(ImportItemCollectionBase.class);

        final ImportGitRepository[] importRepositories = repositoryCollection.getRepositories();

        repositoryTable.setImportRepositories(importRepositories);
    }

    private IStatus cloneRepository(
        final UsernamePasswordCredentials credentials,
        final ImportGitRepository repository) {
        setMessage(MessageFormat.format(
            Messages.getString("GitImportWizardClonePage.CloningMessageFormat"), //$NON-NLS-1$
            repository.getName()));
        repository.setCloneStatus(ImportGitRepository.CLONE_IN_PROGRESS);
        repositoryTable.refresh();

        final CloneGitRepositoryCommand cloneCommand = createCloneCommand(credentials, repository);

        IStatus status;
        try {
            status = getCommandExecutor().execute(CommandFactory.newCancelableCommand(cloneCommand));

            if (status.getSeverity() == IStatus.CANCEL) {
                repository.setCloneStatus(ImportGitRepository.CLONE_CANCELLED);
                setMessage(Messages.getString("GitImportWizardClonePage.CloneCancelledMessageText"), WARNING); //$NON-NLS-1$
            } else if (status.getSeverity() == IStatus.ERROR) {
                repository.setCloneStatus(ImportGitRepository.CLONE_ERROR);
                setMessage(MessageFormat.format(
                    Messages.getString("GitImportWizardClonePage.CloneFailedMessageFormat"), //$NON-NLS-1$
                    status.getMessage()), ERROR);
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
            setMessage(e.getLocalizedMessage(), ERROR);
            status = new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        }

        repositoryTable.refresh();
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

    private static Integer getValueIndex(final String[] items, final String value) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equalsIgnoreCase(value)) {
                return new Integer(i);
            }
        }

        return null;
    }

    private class ImportWizardCloneRepositoriesTable extends TableControl {
        final TableViewer tableViewer;

        protected ImportWizardCloneRepositoriesTable(final Composite parent) {
            super(parent, SWT.FULL_SELECTION, ImportGitRepository.class, null);

            final TableColumnData[] columnData = new TableColumnData[] {
                new TableColumnData(
                    Messages.getString("GitImportWizardClonePage.ColumnRepositoryName"), //$NON-NLS-1$
                    75,
                    0.4F,
                    REPOSITORY_COLUMN),
                new TableColumnData(
                    Messages.getString("GitImportWizardClonePage.ColumnBranchName"), //$NON-NLS-1$
                    75,
                    0.4F,
                    BRANCH_COLUMN),
                new TableColumnData(
                    Messages.getString("GitImportWizardClonePage.ColumnStateName"), //$NON-NLS-1$
                    30,
                    0.2F,
                    STATE_COLUMN)
            };

            setupTable(true, true, columnData);
            setUseViewerDefaults();

            tableViewer = getViewer();

            final TableColumn repositoryColumn = getColumn(REPOSITORY_COLUMN);
            final TableViewerColumn repositoryColumnViewer = new TableViewerColumn(tableViewer, repositoryColumn);
            repositoryColumnViewer.setLabelProvider(new RepositoryNameLabelProvider());

            final TableColumn branchColumn = getColumn(BRANCH_COLUMN);
            final TableViewerColumn branchColumnViewer = new TableViewerColumn(tableViewer, branchColumn);
            branchColumnViewer.setLabelProvider(new BranchNameLabelProvider());
            branchColumnViewer.setEditingSupport(new EditingSupport(tableViewer) {
                @Override
                protected void setValue(final Object element, final Object newValue) {
                    if (element instanceof ImportGitRepository) {
                        final ImportGitRepository repository = (ImportGitRepository) element;
                        if (repository.getDefaultBranch() != null
                            && !repository.getDefaultBranch().equalsIgnoreCase((String) newValue)) {
                            repository.setDefaultBranch((String) newValue);
                            tableViewer.refresh();
                        }
                    }
                }

                @Override
                protected Object getValue(final Object element) {
                    if (element instanceof ImportGitRepository) {
                        final ImportGitRepository repository = (ImportGitRepository) element;
                        return repository.getDefaultBranch();
                    } else {
                        return null;
                    }
                }

                @Override
                protected CellEditor getCellEditor(final Object element) {
                    if (element instanceof ImportGitRepository) {
                        final ImportGitRepository repository = (ImportGitRepository) element;
                        final CellEditor editor =
                            new ComboBoxCellEditor(getTable(), repository.getBranches(), SWT.READ_ONLY) {
                            @Override
                            protected Object doGetValue() {
                                final int valueIdx = (Integer) super.doGetValue();
                                if (valueIdx < 0 || getItems().length <= valueIdx) {
                                    return null;
                                } else {
                                    return getItems()[valueIdx];
                                }
                            }

                            @Override
                            protected void doSetValue(final Object value) {
                                if (value != null) {
                                    repository.setDefaultBranch((String) value);
                                    super.doSetValue(getValueIndex(getItems(), (String) value));
                                }
                            }
                        };

                        editor.addListener(new ICellEditorListener() {
                            @Override
                            public void applyEditorValue() {
                                setErrorMessage(null);
                            }

                            @Override
                            public void cancelEditor() {
                                setErrorMessage(null);
                            }

                            @Override
                            public void editorValueChanged(final boolean oldValidState, final boolean newValidState) {
                                setErrorMessage(editor.getErrorMessage());
                            }
                        });

                        return editor;
                    } else {
                        return null;
                    }
                }

                @Override
                protected boolean canEdit(final Object element) {
                    return true;
                }
            });

            final TableColumn cloneStatusColumn = getColumn(STATE_COLUMN);
            final TableViewerColumn cloneStatusColumnViewer = new TableViewerColumn(tableViewer, cloneStatusColumn);
            cloneStatusColumnViewer.setLabelProvider(new CloneStatusLabelProvider());

            getTable().setSortColumn(repositoryColumn);

            showColumn(STATE_COLUMN, false);
        }

        public TableColumn getColumn(final String columnID) {
            final int idx = TableViewerUtils.columnPropertyNameToColumnIndex(columnID, getViewer());
            return getColumn(idx);
        }

        public TableColumn getColumn(final int columnIdx) {
            return getTable().getColumn(columnIdx);
        }

        public void showColumn(final String columnID, final boolean show) {
            getColumn(columnID).setWidth(show ? 100 : 0);
        }

        public void setImportRepositories(final ImportGitRepository[] repositories) {
            setElements(repositories);
        }

        public ImportGitRepository[] getImportRepositories() {
            return (ImportGitRepository[]) getElements();
        }
    }

    private class RepositoryNameLabelProvider extends CellLabelProvider {
        @Override
        public void update(final ViewerCell cell) {
            final Object element = cell.getElement();

            if (element instanceof ImportGitRepository) {
                final ImportGitRepository repository = (ImportGitRepository) element;
                cell.setText(repository.getName());
            }
        }
    }

    private class BranchNameLabelProvider extends CellLabelProvider {
        @Override
        public void update(final ViewerCell cell) {
            final Object element = cell.getElement();

            if (element instanceof ImportGitRepository) {
                final ImportGitRepository repository = (ImportGitRepository) element;
                cell.setText(repository.getDefaultBranch());
            }
        }
    }

    private class CloneStatusLabelProvider extends CellLabelProvider {
        @Override
        public void update(final ViewerCell cell) {
            final Object element = cell.getElement();

            if (element instanceof ImportGitRepository) {
                final ImportGitRepository repository = (ImportGitRepository) element;
                cell.setText(repository.getCloneStatus());
            }
        }
    }
}
