// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.merge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.microsoft.tfs.client.common.commands.vc.GetMergeCandidatesCommand;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.ChangesetFormatter;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewChangesetDetailsTask;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.MergeCandidate;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.LocaleUtil;

public class SelectChangesetsWizardPage extends WizardPage {
    private MergeCandidate[] mergeCandidates = new MergeCandidate[0];

    private TableViewer tableViewer;
    private String sourcePath;
    private String targetPath;
    private MergeFlags mergeFlags;

    private VersionSpec fromVersion;
    private VersionSpec toVersion;

    public static final String NAME = "SelectChangesetsWizardPage"; //$NON-NLS-1$

    private static final int COLUMN_CHANGESET = 0;
    private static final int COLUMN_DATE = 1;
    private static final int COLUMN_USER = 2;
    private static final int COLUMN_COMMENT = 3;

    private Table table;

    /**
     * Create the wizard
     */
    public SelectChangesetsWizardPage() {
        super(NAME);
        setTitle(Messages.getString("SelectChangesetsWizardPage.PageTitle")); //$NON-NLS-1$
        setDescription(Messages.getString("SelectChangesetsWizardPage.PageDescription")); //$NON-NLS-1$
        setPageComplete(false);
    }

    /**
     * Create contents of the wizard
     *
     * @param parent
     */
    @Override
    public void createControl(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NULL);
        container.setLayout(new GridLayout());
        //
        setControl(container);

        final Label descriptionTextLabel = new Label(container, SWT.WRAP);
        descriptionTextLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
        descriptionTextLabel.setText(Messages.getString("SelectChangesetsWizardPage.DescriptionLabelText")); //$NON-NLS-1$
        ControlSize.setCharWidthHint(descriptionTextLabel, MergeWizard.TEXT_CHARACTER_WIDTH);

        final Label selectTheChangesLabel = new Label(container, SWT.NONE);
        selectTheChangesLabel.setText(Messages.getString("SelectChangesetsWizardPage.SelectChangesLabelText")); //$NON-NLS-1$

        createTable(container);
        addColumns();
        addMenus();

        ControlSize.setCharWidthHint(table, 80);
        ControlSize.setCharHeightHint(table, 15);

        tableViewer.setInput(mergeCandidates);
    }

    private void addMenus() {
        // Add a menu to the table to allow viewing of changeset details etc
        // A feature not in Visual Studio's Dialog I might add.
        final Menu menu = new Menu(table);
        table.setMenu(menu);

        final MenuItem copyMenuItem = new MenuItem(menu, SWT.NONE);
        copyMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final TableItem[] selectedItems = table.getSelection();
                String selection = ""; //$NON-NLS-1$
                for (int i = 0; i < selectedItems.length; i++) {
                    selection += selectedItems[i].getText(COLUMN_CHANGESET);
                    for (int j = 1; j < 5; j++) {
                        selection += "  " + selectedItems[i].getText(j); //$NON-NLS-1$
                    }
                    if (i < (selectedItems.length - 1)) {
                        selection += "\r\n"; //$NON-NLS-1$
                    }
                    final Clipboard clip = new Clipboard(SelectChangesetsWizardPage.this.getShell().getDisplay());
                    clip.setContents(new Object[] {
                        selection
                    }, new Transfer[] {
                        TextTransfer.getInstance()
                    });
                }
            }
        });
        copyMenuItem.setText(Messages.getString("SelectChangesetsWizardPage.CopyMenuItemText")); //$NON-NLS-1$

        new MenuItem(menu, SWT.SEPARATOR);

        final MenuItem detailsMenuItem = new MenuItem(menu, SWT.NONE);
        detailsMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final TableItem[] selectedItems = table.getSelection();

                if (selectedItems.length == 0) {
                    return;
                }

                final int changesetNumber = Integer.parseInt(selectedItems[0].getText(COLUMN_CHANGESET));

                final ViewChangesetDetailsTask task = new ViewChangesetDetailsTask(
                    getShell(),
                    ((MergeWizard) getWizard()).getRepository(),
                    changesetNumber);

                task.run();

                if (task.wasChangesetUpdated()) {
                    // Force a refresh of the table to grab any changed
                    // changeset info.
                    mergeCandidates = new MergeCandidate[0];
                    setVisible(true);
                }
            }
        });
        detailsMenuItem.setText(Messages.getString("SelectChangesetsWizardPage.DetailsMenuItemText")); //$NON-NLS-1$
    }

    private void addColumns() {
        // Columns
        final TableLayout mergeTableLayout = new TableLayout();
        table.setLayout(mergeTableLayout);

        mergeTableLayout.addColumnData(new ColumnWeightData(15, 10, true));
        final TableColumn changesetTableColumn = new TableColumn(table, SWT.NONE);
        changesetTableColumn.setText(Messages.getString("SelectChangesetsWizardPage.ColumnNameChangeset")); //$NON-NLS-1$

        mergeTableLayout.addColumnData(new ColumnWeightData(25, 10, true));
        final TableColumn changeTableColumn = new TableColumn(table, SWT.NONE);
        changeTableColumn.setText(Messages.getString("SelectChangesetsWizardPage.ColumnNameDate")); //$NON-NLS-1$

        mergeTableLayout.addColumnData(new ColumnWeightData(15, 10, true));
        final TableColumn userTableColumn = new TableColumn(table, SWT.NONE);
        userTableColumn.setText(Messages.getString("SelectChangesetsWizardPage.ColumnNameUser")); //$NON-NLS-1$

        mergeTableLayout.addColumnData(new ColumnWeightData(35, 10, true));
        final TableColumn commentTableColumn = new TableColumn(table, SWT.FILL);
        commentTableColumn.setText(Messages.getString("SelectChangesetsWizardPage.ColumnNameComment")); //$NON-NLS-1$
    }

    private void createTable(final Composite container) {
        tableViewer = new TableViewer(container, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
        table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        table.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        tableViewer.setContentProvider(new MergeCandidateContentProvider());
        tableViewer.setLabelProvider(new MergeCandidateLabelProvider());

        // Make it so that a continuous range of rows must be selected.
        table.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                selectConsecutiveChangesets();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectConsecutiveChangesets();
            }

        });
    }

    /**
     * Interigate the selection of the table and select all changesets between
     * the first selected and the last selected changeset. This is because the
     * merge works for a range of changesets not just individual ones.
     */
    private void selectConsecutiveChangesets() {
        if (table.getSelectionCount() > 0) {
            final int[] selectedRows = table.getSelectionIndices();

            int startPos = Integer.MAX_VALUE;
            int endPos = -1;
            // find the start and end positions.
            for (int i = 0; i < selectedRows.length; i++) {
                if (selectedRows[i] < startPos) {
                    startPos = selectedRows[i];
                }
                if (selectedRows[i] > endPos) {
                    endPos = selectedRows[i];
                }
            }
            table.select(startPos, endPos);
            fromVersion = new ChangesetVersionSpec(Integer.parseInt(table.getItem(startPos).getText(COLUMN_CHANGESET)));
            toVersion = new ChangesetVersionSpec(Integer.parseInt(table.getItem(endPos).getText(COLUMN_CHANGESET)));
        } else {
            fromVersion = null;
            toVersion = null;
        }
        updatePageComplete();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
     */
    @Override
    public void setVisible(final boolean visible) {
        final TFSRepository repository = ((MergeWizard) getWizard()).getRepository();

        if (visible && (mergeCandidates == null || mergeCandidates.length == 0)) {
            setPageComplete(false);

            final ICommand command = new Command() {
                @Override
                public String getName() {
                    return Messages.getString("SelectChangesetsWizardPage.ExaminingProgress"); //$NON-NLS-1$
                }

                @Override
                public String getErrorDescription() {
                    return Messages.getString("SelectChangesetsWizardPage.ExaminingError"); //$NON-NLS-1$
                }

                @Override
                public String getLoggingDescription() {
                    return Messages.getString("SelectChangesetsWizardPage.ExaminingProgress", LocaleUtil.ROOT); //$NON-NLS-1$
                }

                @Override
                protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
                    progressMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

                    final GetMergeCandidatesCommand candidatesCommand = new GetMergeCandidatesCommand(
                        repository,
                        sourcePath,
                        targetPath,
                        RecursionType.FULL,
                        mergeFlags);

                    final IStatus status = new CommandExecutor().execute(candidatesCommand);
                    mergeCandidates = candidatesCommand.getMergeCandidates();

                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (table.isDisposed()) {
                                return;
                            }

                            if (!status.isOK()) {
                                ErrorDialog.openError(
                                    getShell(),
                                    Messages.getString("SelectChangesetsWizardPage.ErrorDialogTitle"), //$NON-NLS-1$
                                    null,
                                    status);
                            }

                            /*
                             * Update the UI with the new values.
                             */
                            tableViewer.setInput(mergeCandidates);
                        }
                    });

                    progressMonitor.done();
                    return status;
                }
            };

            /*
             * The wizard command executor blocks on execute but exposes a
             * progress indicator in the wizard page which handles status and
             * cancellation.
             */
            UICommandExecutorFactory.newWizardCommandExecutor(getContainer()).execute(command);
        }

        super.setVisible(visible);
    }

    /**
     * Validate the page.
     */
    private void updatePageComplete() {
        if (fromVersion != null && toVersion != null) {
            setPageComplete(true);
            return;
        }

        setPageComplete(false);
    }

    /**
     * Called by the wizard once it knows what the source and target are.
     *
     * @param sourcePath
     * @param targetPath
     */
    public void setMergeSourceTarget(final String sourcePath, final String targetPath, final MergeFlags mergeFlags) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.mergeFlags = mergeFlags;

        mergeCandidates = new MergeCandidate[0];
        tableViewer.setInput(mergeCandidates);
    }

    public VersionSpec getFromVersion() {
        return fromVersion;
    }

    public VersionSpec getToVersion() {
        return toVersion;
    }

    private class MergeCandidateContentProvider implements IStructuredContentProvider {

        @Override
        public Object[] getElements(final Object inputElement) {
            return mergeCandidates;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        }

    }

    private class MergeCandidateLabelProvider extends LabelProvider implements ITableLabelProvider {

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (!(element instanceof MergeCandidate)) {
                return element.toString();
            }

            final ChangesetFormatter formatter = new ChangesetFormatter(((MergeCandidate) element).getChangeset());

            switch (columnIndex) {
                case COLUMN_CHANGESET:
                    return formatter.getID();
                case COLUMN_DATE:
                    return formatter.getFormattedDate();
                case COLUMN_USER:
                    return formatter.getUser();
                case COLUMN_COMMENT:
                    return formatter.getComment();
            }

            return Messages.getString("SelectChangesetsWizardPage.UnknownCellContent"); //$NON-NLS-1$
        }
    }
}
