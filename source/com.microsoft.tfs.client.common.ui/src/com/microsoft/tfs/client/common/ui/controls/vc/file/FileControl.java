// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.file;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.client.common.ui.tasks.vc.SetWorkingFolderTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.UncloakWorkingFolderTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFile;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemTableViewerSorter;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.util.Check;

public class FileControl extends Composite implements ISelectionProvider {
    private final static Log log = LogFactory.getLog(FileControl.class);

    private Link statusLink;
    private TableViewer fileTableViewer;
    private final FileControlPendingChangesCache pendingChangesCache = new FileControlPendingChangesCache();
    private boolean showDeletedItems;
    private String selectedServerPath;
    private TFSRepository repository;

    public FileControl(final Composite parent, final int style) {
        this(parent, style, false);
    }

    public FileControl(final Composite parent, final int style, final boolean showDeletedItems) {
        super(parent, style);

        this.showDeletedItems = showDeletedItems;

        setLayout(new FormLayout());

        // Create a composite to hold the local path label and link.
        final Composite composite = new Composite(this, SWT.BORDER);
        final GridLayout compositeLayout = new GridLayout(1, false);
        compositeLayout.marginWidth = 4;
        compositeLayout.marginHeight = 4;
        compositeLayout.horizontalSpacing = 0;
        compositeLayout.verticalSpacing = 0;
        composite.setLayout(compositeLayout);

        // Create the local path link.
        statusLink = new Link(composite, SWT.NONE);
        statusLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                localPathLinkClicked();
            }
        });
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(statusLink);

        // Layout the file table beneath local path bar.
        final FormData formData = new FormData();
        formData.right = new FormAttachment(100, 0);
        formData.top = new FormAttachment(0, 0);
        formData.left = new FormAttachment(0, 0);
        composite.setLayoutData(formData);

        // create the file view pane for displaying the files in
        // the currently selected folder

        fileTableViewer =
            new TableViewer(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);

        final Table fileTable = fileTableViewer.getTable();

        /* Don't show gridlines on Windows, it looks funny */
        fileTable.setLinesVisible(!WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS));
        fileTable.setHeaderVisible(true);

        final FormData formData_1 = new FormData();
        formData_1.bottom = new FormAttachment(100, 0);
        formData_1.right = new FormAttachment(100, 0);
        formData_1.top = new FormAttachment(composite, 0, SWT.DEFAULT);
        formData_1.left = new FormAttachment(0, 0);

        fileTable.setLayoutData(formData_1);

        addTableColumns(fileTable);

        fileTableViewer.setContentProvider(new FileControlContentProvider(showDeletedItems));
        fileTableViewer.setLabelProvider(new FileControlLabelProvider(pendingChangesCache));

        final TableViewerSorter sorter = new TFSItemTableViewerSorter(fileTableViewer);
        sorter.setComparator(4, new Comparator() {
            private final DateFormat format = SimpleDateFormat.getDateTimeInstance();

            @Override
            public int compare(final Object arg0, final Object arg1) {
                final TFSItem item1 = (TFSItem) arg0;
                final TFSItem item2 = (TFSItem) arg1;

                if (item1.getExtendedItem().getCheckinDate() == null
                    && item2.getExtendedItem().getCheckinDate() == null) {
                    return 0;
                }
                if (item1.getExtendedItem().getCheckinDate() != null) {
                    return item1.getExtendedItem().getCheckinDate().getTime().compareTo(
                        item2.getExtendedItem().getCheckinDate().getTime());
                }
                return -1;
            }
        });
        fileTableViewer.setSorter(sorter);
        sorter.sort(0);
    }

    private void localPathLinkClicked() {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(selectedServerPath, "selectedServerPath"); //$NON-NLS-1$

        final String localPath = repository.getWorkspace().getMappedLocalPath(selectedServerPath);

        if (localPath != null) {
            ViewFileHelper.viewLocalFileOrFolder(
                localPath,
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
                false);
        } else {
            final WorkingFolder workingFolder =
                repository.getWorkspace().getExactMappingForServerPath(selectedServerPath);
            if (workingFolder != null && workingFolder.isCloaked()) {
                new UncloakWorkingFolderTask(getShell(), repository, selectedServerPath).run();
            } else {
                new SetWorkingFolderTask(getShell(), repository, selectedServerPath, true).run();
            }
        }
    }

    private void addTableColumns(final Table table) {
        final TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column1 = new TableColumn(table, SWT.NONE);
        column1.setText(Messages.getString("FileControl.ColumNameName")); //$NON-NLS-1$
        column1.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column2 = new TableColumn(table, SWT.NONE);
        column2.setText(Messages.getString("FileControl.ColumnNamePendingChange")); //$NON-NLS-1$
        column2.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column3 = new TableColumn(table, SWT.NONE);
        column3.setText(Messages.getString("FileControl.ColumnNameUser")); //$NON-NLS-1$
        column3.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column4 = new TableColumn(table, SWT.NONE);
        column4.setText(Messages.getString("FileControl.ColumnNameLatest")); //$NON-NLS-1$
        column4.setResizable(true);

        tableLayout.addColumnData(new ColumnWeightData(1, true));
        final TableColumn column5 = new TableColumn(table, SWT.NONE);
        column5.setText(Messages.getString("FileControl.ColumnNameLastCheckin")); //$NON-NLS-1$
        column5.setResizable(true);
    }

    public void setShowDeletedItems(final boolean showDeletedItems) {
        this.showDeletedItems = showDeletedItems;
        ((FileControlContentProvider) fileTableViewer.getContentProvider()).setShowDeletedItems(showDeletedItems);
    }

    public void clear() {
        fileTableViewer.setInput(null);
        setStatusLinkText(""); //$NON-NLS-1$
        selectedServerPath = null;
        repository = null;
    }

    public void clearPendingChangesCache() {
        pendingChangesCache.clear();
    }

    public void refresh(final TFSFolder folder) {
        pendingChangesCache.setCurrentFolder(folder, showDeletedItems);
        fileTableViewer.setInput(folder);
    }

    @Override
    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        fileTableViewer.addSelectionChangedListener(listener);
    }

    @Override
    public ISelection getSelection() {
        return fileTableViewer.getSelection();
    }

    @Override
    public void removeSelectionChangedListener(final ISelectionChangedListener listener) {
        fileTableViewer.removeSelectionChangedListener(listener);
    }

    public void setSelectedFilename(final String filename) {
        if (!fileTableViewer.getTable().isDisposed()) {
            for (int i = 0; i < fileTableViewer.getTable().getItemCount(); i++) {
                final Object element = fileTableViewer.getElementAt(i);
                if (element instanceof TFSFile) {
                    final TFSFile file = (TFSFile) element;
                    if (file.getName().equals(filename)) {
                        fileTableViewer.getTable().setSelection(i);
                        fileTableViewer.getTable().setFocus();
                    }
                }
            }
        }
    }

    @Override
    public void setSelection(final ISelection selection) {
        fileTableViewer.setSelection(selection);
    }

    public TableViewer getTableViewer() {
        return fileTableViewer;
    }

    public void setStatusNotConnected() {
        setStatusLinkText(Messages.getString("FileControl.StatusNotConnected")); //$NON-NLS-1$
        selectedServerPath = null;
        repository = null;
    }

    public void setSelectedServerPath(final TFSRepository repository, final String serverPath) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(serverPath, "serverPath"); //$NON-NLS-1$

        this.repository = repository;
        try {
            final String localPath = repository.getWorkspace().getMappedLocalPath(serverPath);

            final String anchorFormat = "<a>{0}</a>"; //$NON-NLS-1$
            String anchor;

            if (localPath == null) {
                final String notMapped = Messages.getString("FileControl.StatusNotMapped"); //$NON-NLS-1$
                anchor = MessageFormat.format(anchorFormat, notMapped);
            } else {
                boolean exists = false;
                try {
                    exists = new File(localPath).exists();
                } catch (final Exception e) {
                    log.error(MessageFormat.format("Error testing whether local path mapped: {0}", localPath), e); //$NON-NLS-1$
                    exists = false;
                }

                if (exists) {
                    anchor = MessageFormat.format(anchorFormat, localPath);
                } else {
                    anchor = localPath;
                }
            }

            final String textFormat = Messages.getString("FileControl.LocalPathLabelFormat"); //$NON-NLS-1$
            final String text = MessageFormat.format(textFormat, anchor);
            setStatusLinkText(text);
        } catch (final PathTooLongException e) {
            setStatusLinkText(e.getLocalizedMessage());
        }

        selectedServerPath = serverPath;
    }

    public void addDoubleClickListener(final IDoubleClickListener listener) {
        fileTableViewer.addDoubleClickListener(listener);
    }

    private void setStatusLinkText(final String text) {
        statusLink.setText(text);
        layout();
    }
}
